package service;

import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

public class MenuPerformanceReportService {
    private final Supplier<EntityManager> entityManagers;
    public MenuPerformanceReportService() { this(DatabaseUtil::getEntityManager); }
    MenuPerformanceReportService(Supplier<EntityManager> value) { entityManagers = value; }

    public Map<String,Object> report(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) throw new IllegalArgumentException("Invalid date range");
        EntityManager em = entityManagers.get();
        try {
            List<Object[]> raw = em.createQuery("SELECT o.orderId,oi.orderItemId,oi.variant.variantId,oi.productName,oi.variantName,oi.quantity,oi.totalPrice,o.discountAmount,oi.totalCostSnapshot FROM OrderItem oi JOIN oi.order o WHERE o.orderStatus='DELIVERED' AND o.deliveredAt>=:from AND o.deliveredAt<:to ORDER BY o.orderId,oi.orderItemId", Object[].class)
                    .setParameter("from", from.atStartOfDay()).setParameter("to", to.plusDays(1).atStartOfDay()).getResultList();
            return summarize(toRows(raw));
        } finally { em.close(); }
    }

    static List<BigDecimal> allocateDiscount(BigDecimal discount, List<BigDecimal> totals) {
        BigDecimal normalized = money(discount == null ? BigDecimal.ZERO : discount);
        BigDecimal subtotal = totals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (subtotal.signum() <= 0) return totals.stream().map(value -> BigDecimal.ZERO.setScale(2)).toList();
        List<BigDecimal> exact = totals.stream().map(total -> normalized.multiply(total).divide(subtotal, 8, RoundingMode.HALF_UP)).toList();
        List<BigDecimal> result = exact.stream().map(value -> value.setScale(2, RoundingMode.DOWN)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        int cents = normalized.subtract(result.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).movePointRight(2).intValueExact();
        List<Integer> order = java.util.stream.IntStream.range(0, totals.size()).boxed()
                .sorted((left,right) -> exact.get(right).subtract(result.get(right)).compareTo(exact.get(left).subtract(result.get(left)))).toList();
        for (int index = 0; index < cents; index++) result.set(order.get(index % order.size()), result.get(order.get(index % order.size())).add(new BigDecimal("0.01")));
        return result;
    }

    static Map<String,Object> row(Integer variantId,String product,String variant,int quantity,BigDecimal gross,BigDecimal discount,BigDecimal cost) {
        Map<String,Object> row = new LinkedHashMap<>();
        row.put("variantId",variantId);row.put("productName",product);row.put("variantName",variant);row.put("quantitySold",quantity);
        row.put("grossRevenue",money(gross));row.put("allocatedDiscount",money(discount));row.put("netRevenue",money(gross.subtract(discount)));row.put("cost",cost==null?null:money(cost));
        return row;
    }

    static Map<String,Object> summarize(List<Map<String,Object>> source) {
        Map<String,Map<String,Object>> grouped = new LinkedHashMap<>();
        int missing = 0;
        for (Map<String,Object> value : source) {
            Integer id=(Integer)value.get("variantId");String groupKey=id==null?"legacy:"+value.get("productName")+":"+value.get("variantName"):"variant:"+id;BigDecimal cost=(BigDecimal)value.get("cost"); if(cost==null)missing++;
            Map<String,Object> line=grouped.computeIfAbsent(groupKey,key->row(id,(String)value.get("productName"),(String)value.get("variantName"),0,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO));
            line.put("quantitySold",(int)line.get("quantitySold")+(int)value.get("quantitySold"));
            for(String key:List.of("grossRevenue","allocatedDiscount","netRevenue"))line.put(key,money(((BigDecimal)line.get(key)).add((BigDecimal)value.get(key))));
            if(cost==null)line.put("costComplete",false);else line.put("cost",money(((BigDecimal)line.get("cost")).add(cost)));
        }
        BigDecimal gross=BigDecimal.ZERO,discount=BigDecimal.ZERO,net=BigDecimal.ZERO,cost=BigDecimal.ZERO;
        for(Map<String,Object> line:grouped.values()){gross=gross.add((BigDecimal)line.get("grossRevenue"));discount=discount.add((BigDecimal)line.get("allocatedDiscount"));net=net.add((BigDecimal)line.get("netRevenue"));cost=cost.add((BigDecimal)line.get("cost"));decorate(line);}
        Map<String,Object> result=new LinkedHashMap<>();result.put("grossRevenue",money(gross));result.put("allocatedDiscount",money(discount));result.put("netRevenue",money(net));result.put("cost",money(cost));result.put("grossProfit",money(net.subtract(cost)));result.put("costComplete",missing==0);result.put("missingCostItemCount",missing);result.put("foodCostPercent",percent(cost,net));result.put("grossMarginPercent",percent(net.subtract(cost),net));result.put("items",new ArrayList<>(grouped.values()));return result;
    }

    private static List<Map<String,Object>> toRows(List<Object[]> raw){List<Map<String,Object>> rows=new ArrayList<>();for(int start=0;start<raw.size();){int end=start+1;while(end<raw.size()&&raw.get(end)[0].equals(raw.get(start)[0]))end++;List<BigDecimal>totals=raw.subList(start,end).stream().map(r->(BigDecimal)r[6]).toList();List<BigDecimal>discounts=allocateDiscount((BigDecimal)raw.get(start)[7],totals);for(int i=start;i<end;i++){Object[]r=raw.get(i);rows.add(row((Integer)r[2],(String)r[3],(String)r[4],(int)r[5],(BigDecimal)r[6],discounts.get(i-start),(BigDecimal)r[8]));}start=end;}return rows;}
    private static void decorate(Map<String,Object> line){BigDecimal net=(BigDecimal)line.get("netRevenue"),cost=(BigDecimal)line.get("cost"),profit=net.subtract(cost);line.putIfAbsent("costComplete",true);line.put("grossProfit",money(profit));line.put("foodCostPercent",percent(cost,net));line.put("grossMarginPercent",percent(profit,net));}
    private static BigDecimal money(BigDecimal value){return value.setScale(2,RoundingMode.HALF_UP);}
    private static BigDecimal percent(BigDecimal value,BigDecimal base){return base.signum()==0?BigDecimal.ZERO.setScale(2):value.multiply(BigDecimal.valueOf(100)).divide(base,2,RoundingMode.HALF_UP);}
}
