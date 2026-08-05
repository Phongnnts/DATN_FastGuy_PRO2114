package dao;

import utils.DatabaseUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryTransactionDAO {
    private static final String SELECT_CLAUSE =
            "SELECT t.inventoryTransactionId, t.order.orderId, t.order.orderCode, " +
            "t.variant.variantId, t.variant.variantName, " +
            "t.variant.product.productId, t.variant.product.name, " +
            "t.transactionType, t.quantity, t.createdAt, " +
            "t.reasonCode, t.note, t.quantityBefore, t.quantityAfter, t.createdBy.fullName " +
            "FROM InventoryTransaction t";
    private static final String COUNT_CLAUSE = "SELECT COUNT(t) FROM InventoryTransaction t";
    private static final String ORDER_CLAUSE = " ORDER BY t.createdAt DESC, t.inventoryTransactionId DESC";
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    static List<String> buildConditions(Integer variantId, Integer productId, String transactionType, LocalDateTime from, LocalDateTime to) {
        List<String> conditions = new ArrayList<>();
        if (variantId != null) conditions.add("t.variant.variantId = :variantId");
        if (productId != null) conditions.add("t.variant.product.productId = :productId");
        if (transactionType != null && !transactionType.isBlank()) conditions.add("t.transactionType = :transactionType");
        if (from != null) conditions.add("t.createdAt >= :fromDate");
        if (to != null) conditions.add("t.createdAt < :toDate");
        return conditions;
    }

    static String where(List<String> conditions) {
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    static int firstResult(int page, int size) {
        return (page - 1) * size;
    }

    static LocalDateTime toEnd(LocalDate date) {
        return LocalDate.MAX.equals(date) ? LocalDateTime.MAX : date.plusDays(1).atStartOfDay();
    }

    static Map<String, Object> toDto(Object[] row) {
        Map<String, Object> m = new HashMap<>();
        m.put("transactionId", row[0]);
        m.put("type", row[7]);
        m.put("quantity", row[8]);
        m.put("createdAt", row[9] != null ? ((LocalDateTime) row[9]).format(ISO_FORMAT) : null);
        m.put("variantId", row[3]);
        m.put("variantName", row[4]);
        m.put("productId", row[5]);
        m.put("productName", row[6]);
        m.put("orderId", row[1]);
        m.put("orderCode", row[2]);
        m.put("reasonCode", row[10]);
        m.put("note", row[11]);
        m.put("quantityBefore", row[12]);
        m.put("quantityAfter", row[13]);
        m.put("createdByName", row[14]);
        return m;
    }

    public Map<String, Object> find(Integer variantId, Integer productId, String transactionType, LocalDate fromDate, LocalDate toDate, int page, int size) {
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime to = toDate != null ? toEnd(toDate) : null;
        List<String> conditions = buildConditions(variantId, productId, transactionType, from, to);
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            String where = where(conditions);
            Query rowsQuery = em.createQuery(SELECT_CLAUSE + where + ORDER_CLAUSE, Object[].class);
            Query countQuery = em.createQuery(COUNT_CLAUSE + where, Long.class);
            applyParams(rowsQuery, conditions, variantId, productId, transactionType, from, to);
            applyParams(countQuery, conditions, variantId, productId, transactionType, from, to);
            @SuppressWarnings("unchecked")
            List<Object[]> rows = rowsQuery.setFirstResult(firstResult(page, size)).setMaxResults(size).getResultList();
            long total = (long) countQuery.getSingleResult();
            List<Map<String, Object>> items = new ArrayList<>();
            for (Object[] row : rows) items.add(toDto(row));
            Map<String, Object> result = new HashMap<>();
            result.put("items", items);
            result.put("total", total);
            return result;
        } finally {
            em.close();
        }
    }

    private static void applyParams(Query query, List<String> conditions, Integer variantId, Integer productId, String transactionType, LocalDateTime from, LocalDateTime to) {
        if (conditions.contains("t.variant.variantId = :variantId")) query.setParameter("variantId", variantId);
        if (conditions.contains("t.variant.product.productId = :productId")) query.setParameter("productId", productId);
        if (conditions.contains("t.transactionType = :transactionType")) query.setParameter("transactionType", transactionType);
        if (conditions.contains("t.createdAt >= :fromDate")) query.setParameter("fromDate", from);
        if (conditions.contains("t.createdAt < :toDate")) query.setParameter("toDate", to);
    }
}
