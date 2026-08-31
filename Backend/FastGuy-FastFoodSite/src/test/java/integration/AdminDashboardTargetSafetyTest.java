package integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

class AdminDashboardTargetSafetyTest {
    private static final String DATABASE = "FastGuyDB_Operations060_Test";
    private static final String SERVER = "FASTGUY-SQL";
    private static final String IDENTITY_SQL = "SELECT @@SERVERNAME,DB_NAME(),DATABASEPROPERTYEX(DB_NAME(),'Status'),CAST(compatibility_level AS int) FROM sys.databases WHERE name=DB_NAME()";
    private static final String MIGRATION_SQL = "SELECT migration_id FROM dbo.SchemaMigrationHistory WHERE migration_id IN ('059_shift_schedule_order_timeout','060_operating_finance') ORDER BY migration_id";
    private static final List<String> MIGRATIONS = List.of("059_shift_schedule_order_timeout", "060_operating_finance");

    @Test
    void rejectsWrongDisposableFlagValuesBeforeBootstrap() {
        for (String value : List.of("false", "TRUE ")) {
            Map<String, String> env = validEnv();
            env.put("FASTGUY_DISPOSABLE_DB", value);
            assertPreBootstrapRejected(env);
        }
    }

    @Test
    void rejectsEveryMissingPreBootstrapEnvironmentValueBeforeBootstrap() {
        for (String name : List.of("FASTGUY_DISPOSABLE_DB", "FASTGUY_E2E_DB_NAME", "FASTGUY_E2E_DB_SERVER", "DB_URL")) {
            Map<String, String> env = validEnv();
            env.remove(name);
            assertPreBootstrapRejected(env);
        }
    }

    @Test
    void rejectsBlankExpectedServerBeforeBootstrap() {
        Map<String, String> env = validEnv();
        env.put("FASTGUY_E2E_DB_SERVER", " ");
        assertPreBootstrapRejected(env);
    }

    @Test
    void rejectsWrongEnvironmentDatabaseBeforeBootstrap() {
        Map<String, String> env = validEnv();
        env.put("FASTGUY_E2E_DB_NAME", "FastGuyDB");
        assertPreBootstrapRejected(env);
    }

    @Test
    void rejectsJdbcUrlForWrongDatabaseOrServerBeforeBootstrap() {
        for (String url : List.of(
                "jdbc:sqlserver://" + SERVER + ";databaseName=FastGuyDB",
                "jdbc:sqlserver://OTHER-SQL;databaseName=" + DATABASE,
                "jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE + "_Other",
                "jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE + ";databaseName=FastGuyDB")) {
            Map<String, String> env = validEnv();
            env.put("DB_URL", url);
            assertPreBootstrapRejected(env);
        }
    }

    @Test
    void acceptsExactValidPreflightBeforeBootstrap() {
        assertDoesNotThrow(() -> AdminDashboardIT.verifyPreBootstrap(validEnv()::get));
    }

    @Test
    void rejectsWrongCatalogDatabaseBeforeDashboardWork() {
        assertCatalogRejected(new Target(identity(SERVER, "FastGuyDB", "ONLINE", 160), MIGRATIONS));
    }

    @Test
    void rejectsWrongCatalogServerBeforeDashboardWork() {
        assertCatalogRejected(new Target(identity("OTHER-SQL", DATABASE, "ONLINE", 160), MIGRATIONS));
    }

    @Test
    void rejectsOfflineCatalogDatabaseBeforeDashboardWork() {
        assertCatalogRejected(new Target(identity(SERVER, DATABASE, "OFFLINE", 160), MIGRATIONS));
    }

    @Test
    void rejectsLowCatalogCompatibilityBeforeDashboardWork() {
        assertCatalogRejected(new Target(identity(SERVER, DATABASE, "ONLINE", 159), MIGRATIONS));
    }

    @Test
    void rejectsMissingReorderedOrUnexpectedMigrationIdsBeforeDashboardWork() {
        for (List<String> migrations : List.of(
                List.of("059_shift_schedule_order_timeout"),
                List.of("060_operating_finance"),
                List.of("060_operating_finance", "059_shift_schedule_order_timeout"),
                List.of("059_shift_schedule_order_timeout", "061_work_shift_attendance_approval"))) {
            assertCatalogRejected(new Target(identity(SERVER, DATABASE, "ONLINE", 160), migrations));
        }
    }

    @Test
    void usesExactCatalogQueriesAndCompletesCatalogVerificationBeforeDashboardWork() {
        Target target = validTarget();
        AtomicBoolean dashboardWork = new AtomicBoolean();

        assertDoesNotThrow(() -> AdminDashboardIT.runChecked(target::open, () -> {
            assertTrue(target.catalogComplete());
            dashboardWork.set(true);
        }, validEnv()::get));

        assertTrue(dashboardWork.get());
        assertEquals(List.of(IDENTITY_SQL, MIGRATION_SQL), target.queries());
        assertTrue(target.closed());
    }

    @Test
    void acceptsHealthyZeroDashboardBaseline() {
        assertDoesNotThrow(() -> AdminDashboardIT.assertHealthyDashboard(healthyDashboard()));
    }

    @Test
    void rejectsUnavailableDashboardSection() {
        Map<String, Object> data = healthyDashboard();
        sectionAvailability(data).put("inventory", "UNAVAILABLE");
        assertThrows(AssertionError.class, () -> AdminDashboardIT.assertHealthyDashboard(data));
    }

    @Test
    void rejectsWrongDashboardMetricTypes() {
        Map<String, Object> data = healthyDashboard();
        data.put("netCashRevenueToday", 0L);
        assertThrows(AssertionError.class, () -> AdminDashboardIT.assertHealthyDashboard(data));
        data = healthyDashboard();
        data.put("activeOrderCount", "0");
        Map<String, Object> invalidCounts = data;
        assertThrows(AssertionError.class, () -> AdminDashboardIT.assertHealthyDashboard(invalidCounts));
    }

    @Test
    void rejectsNegativeDashboardMetrics() {
        Map<String, Object> data = healthyDashboard();
        data.put("pendingRefundCount", -1L);
        assertThrows(AssertionError.class, () -> AdminDashboardIT.assertHealthyDashboard(data));
        data = healthyDashboard();
        data.put("activeOrdersByStatus", Map.of("PENDING", -1L));
        Map<String, Object> invalidStatusCounts = data;
        assertThrows(AssertionError.class, () -> AdminDashboardIT.assertHealthyDashboard(invalidStatusCounts));
    }

    private static void assertPreBootstrapRejected(Map<String, String> env) {
        AtomicBoolean bootstrap = new AtomicBoolean();
        AtomicBoolean dashboardWork = new AtomicBoolean();

        assertThrows(IllegalStateException.class, () -> AdminDashboardIT.runChecked(() -> {
            bootstrap.set(true);
            return validTarget().open();
        }, () -> dashboardWork.set(true), env::get));

        assertFalse(bootstrap.get());
        assertFalse(dashboardWork.get());
    }

    private static void assertCatalogRejected(Target target) {
        AtomicBoolean dashboardWork = new AtomicBoolean();

        assertThrows(IllegalStateException.class, () -> AdminDashboardIT.runChecked(target::open, () -> dashboardWork.set(true), validEnv()::get));

        assertFalse(dashboardWork.get());
        assertTrue(target.closed());
    }

    private static Map<String, String> validEnv() {
        Map<String, String> env = new HashMap<>();
        env.put("FASTGUY_DISPOSABLE_DB", "true");
        env.put("FASTGUY_E2E_DB_NAME", DATABASE);
        env.put("FASTGUY_E2E_DB_SERVER", SERVER);
        env.put("DB_URL", "jdbc:sqlserver://" + SERVER + ":1433;encrypt=true;databaseName=" + DATABASE + ";trustServerCertificate=true");
        return env;
    }

    private static Target validTarget() {
        return new Target(identity(SERVER, DATABASE, "ONLINE", 160), MIGRATIONS);
    }

    private static Object[] identity(String server, String database, String status, int compatibility) {
        return new Object[] { server, database, status, compatibility };
    }

    private static Map<String, Object> healthyDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("netCashRevenueToday", BigDecimal.ZERO);
        data.put("activeOrderCount", 0L);
        data.put("pendingRefundCount", 0L);
        data.put("pendingCodCount", 0L);
        data.put("lowStockItemCount", 0L);
        data.put("staffingGapCount", 0L);
        data.put("activeOrdersByStatus", Map.of());
        data.put("operationalOrderCountToday", 0L);
        data.put("operationalCompletedCountToday", 0L);
        data.put("completionRateToday", 0.0);
        data.put("attentionItems", List.of());
        Map<String, String> availability = new LinkedHashMap<>();
        for (String section : List.of("financial", "orders", "refunds", "cod", "inventory", "staffing")) availability.put(section, "AVAILABLE");
        data.put("sectionAvailability", availability);
        return data;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> sectionAvailability(Map<String, Object> data) {
        return (Map<String, String>) data.get("sectionAvailability");
    }

    private static final class Target {
        private final Object[] identity;
        private final List<String> migrations;
        private final List<String> queries = new ArrayList<>();
        private final AtomicInteger opens = new AtomicInteger();
        private final AtomicBoolean catalogComplete = new AtomicBoolean();
        private final AtomicBoolean closed = new AtomicBoolean();

        private Target(Object[] identity, List<String> migrations) {
            this.identity = identity;
            this.migrations = migrations;
        }

        private EntityManager open() {
            opens.incrementAndGet();
            return (EntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(), new Class<?>[] { EntityManager.class }, (proxy, method, args) -> {
                return switch (method.getName()) {
                    case "createNativeQuery" -> query((String) args[0]);
                    case "isOpen" -> !closed.get();
                    case "close" -> { closed.set(true); yield null; }
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            });
        }

        private Query query(String sql) {
            queries.add(sql);
            Object result;
            if (IDENTITY_SQL.equals(sql)) result = identity;
            else if (MIGRATION_SQL.equals(sql)) result = migrations;
            else throw new AssertionError("Unexpected catalog query");
            return (Query) Proxy.newProxyInstance(Query.class.getClassLoader(), new Class<?>[] { Query.class }, (proxy, method, args) -> {
                if ("getSingleResult".equals(method.getName()) && result == identity) return result;
                if ("getResultList".equals(method.getName()) && result == migrations) {
                    catalogComplete.set(true);
                    return result;
                }
                throw new UnsupportedOperationException(method.getName());
            });
        }

        private List<String> queries() { return List.copyOf(queries); }
        private boolean catalogComplete() { return catalogComplete.get(); }
        private boolean closed() { return closed.get(); }
    }
}
