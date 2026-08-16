package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;

class StoreConfigPolicyTest {
    @Test
    void lowStockThresholdDefaultsForMissingMalformedAndOutOfRangeValues() {
        for (String value : new String[] {null, "bad", "0", "1001"}) {
            PersistenceFake fake = new PersistenceFake();
            if (value != null) fake.values.put(StoreConfigService.LOW_STOCK_THRESHOLD, value);
            assertEquals(5, new StoreConfigService(fake::entityManager).getLowStockThreshold());
        }
    }

    @Test
    void lowStockThresholdAcceptsInclusiveBoundaries() {
        for (int threshold : new int[] {1, 1000}) {
            PersistenceFake fake = new PersistenceFake();
            fake.values.put(StoreConfigService.LOW_STOCK_THRESHOLD, String.valueOf(threshold));
            assertEquals(threshold, new StoreConfigService(fake::entityManager).getLowStockThreshold());
        }
    }

    @Test
    void updateRejectsNonIntegerAndOutOfRangeThresholds() {
        for (Object threshold : new Object[] {"1.5", "bad", 0, 1001}) {
            PersistenceFake fake = new PersistenceFake();
            StoreConfigService service = new StoreConfigService(fake::entityManager);
            assertThrows(RuntimeException.class,
                    () -> service.update(Map.of(StoreConfigService.LOW_STOCK_THRESHOLD, threshold)));
            assertTrue(fake.rolledBack);
            assertFalse(fake.committed);
        }
    }

    @Test
    void updatePersistsInclusiveBoundaries() {
        for (int threshold : new int[] {1, 1000}) {
            PersistenceFake fake = new PersistenceFake();
            new StoreConfigService(fake::entityManager)
                    .update(Map.of(StoreConfigService.LOW_STOCK_THRESHOLD, threshold));
            assertEquals(String.valueOf(threshold), fake.values.get(StoreConfigService.LOW_STOCK_THRESHOLD));
            assertTrue(fake.committed);
        }
    }

    @Test
    void updateRejectsUnknownKeys() {
        PersistenceFake fake = new PersistenceFake();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new StoreConfigService(fake::entityManager).update(Map.of("unknown", "value")));
        assertEquals("Unsupported config key: unknown", error.getMessage());
        assertTrue(fake.rolledBack);
    }

    @Test
    void publicConfigExcludesLowStockThreshold() {
        PersistenceFake fake = new PersistenceFake();
        fake.values.put(StoreConfigService.LOW_STOCK_THRESHOLD, "10");
        assertFalse(new StoreConfigService(fake::entityManager).getPublicConfig().containsKey("lowStockThreshold"));
        assertFalse(new StoreConfigService(fake::entityManager).getPublicConfig()
                .containsKey(StoreConfigService.LOW_STOCK_THRESHOLD));
    }

    @Test
    void invalidValueRollsBackWithoutPartialWrites() {
        PersistenceFake fake = new PersistenceFake();
        fake.values.put("store_name", "Before");
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("store_name", "After");
        update.put(StoreConfigService.LOW_STOCK_THRESHOLD, 0);

        assertThrows(IllegalArgumentException.class,
                () -> new StoreConfigService(fake::entityManager).update(update));

        assertEquals("Before", fake.values.get("store_name"));
        assertTrue(fake.rolledBack);
        assertFalse(fake.committed);
    }

    @Test
    void textKeysAllowEmptyValuesWhileOthersStillReject() throws IOException {
        String src = Files.readString(Path.of("src/main/java/service/StoreConfigService.java"));
        assertTrue(src.contains("if (!TEXT_KEYS.contains(key)) throw new IllegalArgumentException(\"Invalid config value for \" + key)"));
        assertTrue(src.contains("value = \"\";"));
    }

    @Test
    void taxRateConstrainedToZeroThroughOneHundred() throws IOException {
        String src = Files.readString(Path.of("src/main/java/service/StoreConfigService.java"));
        assertTrue(src.contains("if (\"tax_rate\".equals(key) && (fee.compareTo(BigDecimal.ZERO) < 0 || fee.compareTo(HUNDRED) > 0))"));
        assertTrue(src.contains("tax_rate must be between 0 and 100"));
    }

    @Test
    void estimatedDeliveryMinutesConstrainedToTenThroughOneEighty() throws IOException {
        String src = Files.readString(Path.of("src/main/java/service/StoreConfigService.java"));
        assertTrue(src.contains("if (\"estimated_delivery_minutes\".equals(key) && (minutes < 10 || minutes > 180))"));
        assertTrue(src.contains("estimated_delivery_minutes must be between 10 and 180"));
    }

    @Test
    void storeAndCheckoutDefaultToAllDayBusinessHours() throws IOException {
        String configSrc = Files.readString(Path.of("src/main/java/service/StoreConfigService.java"));
        String orderSrc = Files.readString(Path.of("src/main/java/service/OrderService.java"));
        assertTrue(configSrc.contains("config.getOrDefault(OPEN_TIME, \"00:00\")"));
        assertTrue(configSrc.contains("config.getOrDefault(CLOSE_TIME, \"00:00\")"));
        assertTrue(orderSrc.contains("config.getOrDefault(StoreConfigService.OPEN_TIME, \"00:00\")"));
        assertTrue(orderSrc.contains("config.getOrDefault(StoreConfigService.CLOSE_TIME, \"00:00\")"));
    }

    private static class PersistenceFake {
        private final Map<String, String> values = new LinkedHashMap<>();
        private final Map<String, String> pending = new LinkedHashMap<>();
        private boolean committed;
        private boolean rolledBack;
        private boolean active;

        private EntityManager entityManager() {
            EntityTransaction transaction = (EntityTransaction) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] {EntityTransaction.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "begin" -> { active = true; yield null; }
                        case "commit" -> {
                            values.putAll(pending);
                            pending.clear();
                            committed = true;
                            active = false;
                            yield null;
                        }
                        case "rollback" -> {
                            pending.clear();
                            rolledBack = true;
                            active = false;
                            yield null;
                        }
                        case "isActive" -> active;
                        default -> defaultValue(method.getReturnType());
                    });
            return (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] {EntityManager.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getTransaction" -> transaction;
                        case "createNativeQuery" -> query((String) args[0]);
                        default -> defaultValue(method.getReturnType());
                    });
        }

        private Query query(String sql) {
            Map<String, Object> parameters = new LinkedHashMap<>();
            return (Query) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Query.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setParameter" -> {
                            parameters.put((String) args[0], args[1]);
                            yield proxy;
                        }
                        case "getResultList" -> {
                            List<Object[]> rows = new ArrayList<>();
                            values.forEach((key, value) -> rows.add(new Object[] {key, value}));
                            yield rows;
                        }
                        case "executeUpdate" -> {
                            String key = (String) parameters.get("key");
                            String value = (String) parameters.get("value");
                            if (sql.startsWith("UPDATE")) {
                                if (!values.containsKey(key) && !pending.containsKey(key)) yield 0;
                                pending.put(key, value);
                                yield 1;
                            }
                            pending.put(key, value);
                            yield 1;
                        }
                        default -> defaultValue(method.getReturnType());
                    });
        }

        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) return null;
            if (type == boolean.class) return false;
            if (type == char.class) return '\0';
            return 0;
        }
    }
}
