package service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

public class StoreConfigService {
    public static final String OPEN_TIME = "business_open_time";
    public static final String CLOSE_TIME = "business_close_time";
    public static final String SERVICE_FEE = "service_fee";

    public Map<String, String> getAll() {
        EntityManager em = DatabaseUtil.getEntityManager();
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
    private static final java.util.Set<String> INT_KEYS = Set.of("estimated_delivery_minutes");
    private static final java.util.Set<String> TEXT_KEYS = Set.of("store_name", "store_phone", "store_address", "store_logo");

    public Map<String, Object> getPublicConfig() {
        Map<String, String> config = getAll();
        String openTime = config.getOrDefault(OPEN_TIME, "08:00");
        String closeTime = config.getOrDefault(CLOSE_TIME, "22:00");
        BigDecimal serviceFee = parseFee(config.get(SERVICE_FEE));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isOpen", isOpen(openTime, closeTime, LocalTime.now()));
        result.put("openTime", openTime);
        result.put("closeTime", closeTime);
        result.put("serviceFee", serviceFee);
        result.put("taxRate", parseFee(config.get("tax_rate")));
        result.put("deliveryFee", parseFee(config.get("delivery_fee")));
        result.put("minOrderAmount", parseFee(config.get("min_order_amount")));
        result.put("estimatedDeliveryMinutes", parseIntSafe(config.get("estimated_delivery_minutes"), 30));
        result.put("storeName", config.getOrDefault("store_name", "FastGuy"));
        result.put("storePhone", config.getOrDefault("store_phone", ""));
        result.put("storeAddress", config.getOrDefault("store_address", ""));
        result.put("storeLogo", config.getOrDefault("store_logo", ""));
        return result;
    }

    private int parseIntSafe(String val, int defaultVal) {
        try { return val != null && !val.isBlank() ? Integer.parseInt(val) : defaultVal; }
        catch (NumberFormatException e) { return defaultVal; }
    }

    public void update(Map<String, Object> values) {
        if (values == null || values.isEmpty()) throw new IllegalArgumentException("Missing config values");
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue() == null ? null : String.valueOf(entry.getValue()).trim();
                if (!TIME_KEYS.contains(key) && !FEE_KEYS.contains(key) && !INT_KEYS.contains(key) && !TEXT_KEYS.contains(key)) {
                    throw new IllegalArgumentException("Unsupported config key: " + key);
                }
                if (value == null || value.isEmpty()) throw new IllegalArgumentException("Invalid config value for " + key);
                if (FEE_KEYS.contains(key)) parseFee(value);
                else if (TIME_KEYS.contains(key)) LocalTime.parse(value);
                else if (INT_KEYS.contains(key)) Integer.parseInt(value);
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
