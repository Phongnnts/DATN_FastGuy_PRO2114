package service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

public class StoreConfigService {
    public static final String OPEN_TIME = "business_open_time";
    public static final String CLOSE_TIME = "business_close_time";
    public static final String SERVICE_FEE = "service_fee";
    public static final String LOW_STOCK_THRESHOLD = "low_stock_threshold";
    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;
    public static final LocalTime ORDER_CUTOFF_TIME = LocalTime.of(20, 45);
    public static final Set<String> GHN_KEYS = Set.of("ghn_from_district_id", "ghn_from_ward_code", "default_service_type_id", "default_weight", "default_length", "default_width", "default_height");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private final Supplier<EntityManager> entityManagers;

    public StoreConfigService() {
        this(DatabaseUtil::getEntityManager);
    }

    StoreConfigService(Supplier<EntityManager> entityManagers) {
        this.entityManagers = entityManagers;
    }

    public Map<String, String> getAll() {
        EntityManager em = entityManagers.get();
        try {
            Map<String, String> result = new LinkedHashMap<>();
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery("SELECT config_key, config_value FROM ShippingConfig ORDER BY config_key").getResultList();
            for (Object[] row : rows) result.put((String) row[0], (String) row[1]);
            return result;
        } finally {
            em.close();
        }
    }

    private static final java.util.Set<String> TIME_KEYS = Set.of(OPEN_TIME, CLOSE_TIME);
    private static final java.util.Set<String> FEE_KEYS = Set.of(SERVICE_FEE, "tax_rate", "delivery_fee", "min_order_amount");
    private static final java.util.Set<String> INT_KEYS = Set.of("estimated_delivery_minutes", LOW_STOCK_THRESHOLD);
    private static final java.util.Set<String> TEXT_KEYS = Set.of("store_name", "store_phone", "store_address", "store_logo", "morning_count_notice_enabled", "morning_count_notice_title", "morning_count_notice_message", "morning_count_notice_image_url", "morning_count_notice_link", "morning_count_notice_cta_label");

    public Map<String, Object> getPublicConfig() {
        Map<String, String> config = getAll();
        String openTime = config.getOrDefault(OPEN_TIME, "00:00");
        String closeTime = config.getOrDefault(CLOSE_TIME, "00:00");
        BigDecimal serviceFee = parseFee(config.get(SERVICE_FEE));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isOpen", isOpen(openTime, closeTime, LocalTime.now()));
        result.put("openTime", openTime);
        result.put("closeTime", closeTime);
        result.put("orderCutoffTime", ORDER_CUTOFF_TIME.toString());
        result.put("serviceFee", serviceFee);
        result.put("taxRate", parseFee(config.get("tax_rate")));
        result.put("deliveryFee", parseFee(config.get("delivery_fee")));
        result.put("minOrderAmount", parseFee(config.get("min_order_amount")));
        result.put("estimatedDeliveryMinutes", parseIntSafe(config.get("estimated_delivery_minutes"), 30));
        result.put("storeName", config.getOrDefault("store_name", "FastGuy"));
        result.put("storePhone", config.getOrDefault("store_phone", ""));
        result.put("storeAddress", config.getOrDefault("store_address", ""));
        result.put("storeLogo", config.getOrDefault("store_logo", ""));
        result.put("morningCountNotice", morningCountNotice(config));
        return result;
    }

    private Map<String,Object> morningCountNotice(Map<String,String> config) {
        if (!"1".equals(config.get("morning_count_notice_enabled"))) return null;
        EntityManager em=entityManagers.get();
        try {
            Number approved=(Number)em.createNativeQuery("SELECT COUNT_BIG(*) FROM StockCount WHERE count_date=:today AND status='APPROVED'").setParameter("today",LocalDate.now()).getSingleResult();
            if (approved.longValue()>0) return null;
        } finally { em.close(); }
        String title=config.getOrDefault("morning_count_notice_title","Cửa hàng đang chuẩn bị nguyên liệu").trim();
        String message=config.getOrDefault("morning_count_notice_message","Chúng tôi đang kiểm kê đầu ngày.").trim();
        String link=config.getOrDefault("morning_count_notice_link","").trim();
        if (!link.isEmpty() && !(link.startsWith("/") || link.startsWith("https://") || link.startsWith("http://"))) link="";
        Map<String,Object> notice=new LinkedHashMap<>();
        notice.put("stableKey","morning-count-"+LocalDate.now());notice.put("title",title.isEmpty()?"Cửa hàng đang chuẩn bị nguyên liệu":title);notice.put("message",message.isEmpty()?"Chúng tôi đang kiểm kê đầu ngày.":message);notice.put("imageUrl",config.getOrDefault("morning_count_notice_image_url","").trim());notice.put("link",link);notice.put("ctaLabel",config.getOrDefault("morning_count_notice_cta_label","Xem thông báo").trim());return notice;
    }

    private int parseIntSafe(String val, int defaultVal) {
        try { return val != null && !val.isBlank() ? Integer.parseInt(val) : defaultVal; }
        catch (NumberFormatException e) { return defaultVal; }
    }

    public int getLowStockThreshold() {
        int threshold = parseIntSafe(getAll().get(LOW_STOCK_THRESHOLD), DEFAULT_LOW_STOCK_THRESHOLD);
        return threshold >= 1 && threshold <= 1000 ? threshold : DEFAULT_LOW_STOCK_THRESHOLD;
    }

    public void update(Map<String, Object> values) {
        if (values == null || values.isEmpty()) throw new IllegalArgumentException("Missing config values");
        EntityManager em = entityManagers.get();
        try {
            em.getTransaction().begin();
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue() == null ? null : String.valueOf(entry.getValue()).trim();
                if (!TIME_KEYS.contains(key) && !FEE_KEYS.contains(key) && !INT_KEYS.contains(key) && !TEXT_KEYS.contains(key)) {
                    throw new IllegalArgumentException("Unsupported config key: " + key);
                }
                if (value == null || value.isEmpty()) {
                    if (!TEXT_KEYS.contains(key)) throw new IllegalArgumentException("Invalid config value for " + key);
                    value = "";
                } else {
                    if (key.startsWith("morning_count_notice_")) {
                        int max=Set.of("morning_count_notice_image_url").contains(key)?1000:Set.of("morning_count_notice_message","morning_count_notice_link").contains(key)?500:200;
                        if(value.length()>max)throw new IllegalArgumentException("Invalid config value for "+key);
                        if((key.endsWith("_url")||key.endsWith("_link"))&&!value.isEmpty()&&!(value.startsWith("/")||value.startsWith("https://")||value.startsWith("http://")))throw new IllegalArgumentException("Invalid notice URL");
                        if("morning_count_notice_enabled".equals(key)&&!Set.of("0","1").contains(value))throw new IllegalArgumentException("Invalid notice enabled value");
                    } else if (FEE_KEYS.contains(key)) {
                        BigDecimal fee = parseFee(value);
                        if ("tax_rate".equals(key) && (fee.compareTo(BigDecimal.ZERO) < 0 || fee.compareTo(HUNDRED) > 0)) {
                            throw new IllegalArgumentException("tax_rate must be between 0 and 100");
                        }
                    } else if (TIME_KEYS.contains(key)) {
                        LocalTime.parse(value);
                    } else if (INT_KEYS.contains(key)) {
                        int minutes = Integer.parseInt(value);
                        if ("estimated_delivery_minutes".equals(key) && (minutes < 10 || minutes > 180)) {
                            throw new IllegalArgumentException("estimated_delivery_minutes must be between 10 and 180");
                        }
                        if (LOW_STOCK_THRESHOLD.equals(key)) {
                            int threshold = minutes;
                            if (threshold < 1 || threshold > 1000) {
                                throw new IllegalArgumentException("low_stock_threshold must be between 1 and 1000");
                            }
                        }
                    }
                }
                int updated = em.createNativeQuery("UPDATE ShippingConfig SET config_value = :value WHERE config_key = :key")
                        .setParameter("key", key).setParameter("value", value).executeUpdate();
                if (updated == 0) {
                    em.createNativeQuery("INSERT INTO ShippingConfig (config_key, config_value) VALUES (:key, :value)")
                            .setParameter("key", key).setParameter("value", value).executeUpdate();
                }
            }
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public static boolean acceptsCheckoutAt(LocalTime now) {
        return now != null && now.isBefore(ORDER_CUTOFF_TIME);
    }

    public static boolean isOpen(String open, String close, LocalTime now) {
        LocalTime openTime = LocalTime.parse(open);
        LocalTime closeTime = LocalTime.parse(close);
        return openTime.equals(closeTime) || (openTime.isBefore(closeTime)
                ? !now.isBefore(openTime) && now.isBefore(closeTime)
                : !now.isBefore(openTime) || now.isBefore(closeTime));
    }

    public static BigDecimal parseFee(String value) {
        try {
            BigDecimal fee = value == null ? BigDecimal.ZERO : new BigDecimal(value);
            if (fee.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Invalid service fee");
            return fee;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid service fee");
        }
    }
}
