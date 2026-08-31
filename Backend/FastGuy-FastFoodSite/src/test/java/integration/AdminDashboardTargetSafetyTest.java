package integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final List<String> ACTIVE_STATUSES = List.of("PENDING", "CONFIRMED", "PREPARING", "READY", "ASSIGNED", "PICKED_UP", "DELIVERY_FAILED");

    @Test
    void requiresCaseSensitiveExactDisposableFlagBeforeBootstrap() {
        for (String value : listWithNull("false", "TRUE", "TRUE ", "true ", null)) {
            Map<String, String> env = validEnv();
            putOrRemove(env, "FASTGUY_DISPOSABLE_DB", value);
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
    void rejectsInvalidExpectedServerOrDatabaseBeforeBootstrap() {
        for (String server : listWithNull("", " ", " FASTGUY-SQL", "FASTGUY-SQL ", null)) {
            Map<String, String> env = validEnv();
            putOrRemove(env, "FASTGUY_E2E_DB_SERVER", server);
            assertPreBootstrapRejected(env);
        }
        Map<String, String> env = validEnv();
        env.put("FASTGUY_E2E_DB_NAME", "FastGuyDB");
        assertPreBootstrapRejected(env);
    }

    @Test
    void rejectsJdbcDatabaseAndServerAliasBypasses() {
        for (String property : List.of(
                "database=FastGuyDB",
                "DATABASE=" + DATABASE,
                "serverName=OTHER-SQL",
                "SERVERNAME=" + SERVER,
                "databaseName=" + DATABASE,
                "DATABASENAME=" + DATABASE)) {
            Map<String, String> env = validEnv();
            env.put("DB_URL", validUrl() + ";" + property);
            assertPreBootstrapRejected(env);
        }
    }

    @Test
    void rejectsJdbcCredentialUnknownDuplicateAndCaseVariantProperties() {
        for (String property : List.of(
                "user=admin",
                "password=secret",
                "applicationName=test",
                "encrypt=false",
                "ENCRYPT=true",
                "trustServerCertificate=false",
                "TRUSTSERVERCERTIFICATE=true",
                "sendTimeAsDatetime=true",
                "SENDTIMEASDATETIME=false",
                "DatabaseName=" + DATABASE)) {
            Map<String, String> env = validEnv();
            env.put("DB_URL", validUrl() + ";" + property);
            assertPreBootstrapRejected(env);
        }
    }

    @Test
    void rejectsJdbcWrongTargetMalformedAuthorityAndQueryAlternatives() {
        for (String url : List.of(
                "jdbc:sqlserver://" + SERVER + ";databaseName=FastGuyDB",
                "jdbc:sqlserver://OTHER-SQL;databaseName=" + DATABASE,
                "jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE + "_Other",
                "jdbc:sqlserver://;databaseName=" + DATABASE,
                "jdbc:sqlserver://" + SERVER + ":;databaseName=" + DATABASE,
                "jdbc:sqlserver://" + SERVER + ":abc;databaseName=" + DATABASE,
                "jdbc:sqlserver://" + SERVER + ":0;databaseName=" + DATABASE,
                "jdbc:sqlserver://" + SERVER + ":65536;databaseName=" + DATABASE,
                "jdbc:sqlserver://" + SERVER + "/path;databaseName=" + DATABASE,
                "jdbc:sqlserver://user@" + SERVER + ";databaseName=" + DATABASE,
                "jdbc:sqlserver://" + SERVER + "\\instance;databaseName=" + DATABASE,
                "jdbc:sqlserver://" + SERVER + "?databaseName=" + DATABASE,
                "jdbc:sqlserver://" + SERVER + "#fragment;databaseName=" + DATABASE,
                "JDBC:SQLSERVER://" + SERVER + ";databaseName=" + DATABASE,
                validUrl() + ";",
                validUrl() + ";encrypt=")) {
            Map<String, String> env = validEnv();
            env.put("DB_URL", url);
            assertPreBootstrapRejected(env);
        }
    }

    @Test
    void acceptsOnlyCanonicalTargetUrlWithUniqueAllowlistedProperties() {
        for (String url : List.of(
                "jdbc:sqlserver://" + SERVER + ";databaseName=" + DATABASE,
                "jdbc:sqlserver://" + SERVER + ":1433;databaseName=" + DATABASE + ";encrypt=true;trustServerCertificate=true;sendTimeAsDatetime=false",
                validUrl())) {
            Map<String, String> env = validEnv();
            env.put("DB_URL", url);
            assertDoesNotThrow(() -> AdminDashboardIT.verifyPreBootstrap(env::get));
        }
    }

    @Test
    void rejectsWrongCatalogIdentityStateCompatibilityOrMigrationList() {
        for (Target target : List.of(
                new Target(identity(SERVER, "FastGuyDB", "ONLINE", 160), MIGRATIONS),
                new Target(identity("OTHER-SQL", DATABASE, "ONLINE", 160), MIGRATIONS),
                new Target(identity(SERVER, DATABASE, "OFFLINE", 160), MIGRATIONS),
                new Target(identity(SERVER, DATABASE, "ONLINE", 159), MIGRATIONS),
                new Target(identity(SERVER, DATABASE, "ONLINE", 160), List.of("059_shift_schedule_order_timeout")),
                new Target(identity(SERVER, DATABASE, "ONLINE", 160), List.of("060_operating_finance", "059_shift_schedule_order_timeout")),
                new Target(identity(SERVER, DATABASE, "ONLINE", 160), List.of("059_shift_schedule_order_timeout", "061_work_shift_attendance_approval")))) {
            assertCatalogRejected(target);
        }
    }

    @Test
    void usesExactCatalogQueriesBeforeDashboardWorkAndClosesEntityManager() {
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
    void supplierFailurePreventsDashboardAndDatabaseCleanupStillRuns() {
        RuntimeException original = new RuntimeException("bootstrap");
        AtomicBoolean dashboardWork = new AtomicBoolean();
        AtomicBoolean databaseClosed = new AtomicBoolean();

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> AdminDashboardIT.runIntegration(() -> {
            throw original;
        }, () -> dashboardWork.set(true), validEnv()::get, () -> databaseClosed.set(true)));

        assertSame(original, thrown);
        assertFalse(dashboardWork.get());
        assertTrue(databaseClosed.get());
    }

    @Test
    void dashboardFailureClosesEntityManagerAndDatabaseCleanup() {
        Target target = validTarget();
        RuntimeException original = new RuntimeException("dashboard");
        AtomicBoolean databaseClosed = new AtomicBoolean();

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> AdminDashboardIT.runIntegration(target::open, () -> {
            throw original;
        }, validEnv()::get, () -> databaseClosed.set(true)));

        assertSame(original, thrown);
        assertTrue(target.closed());
        assertTrue(databaseClosed.get());
    }

    @Test
    void entityManagerAndDatabaseCloseFailuresAreSuppressedOnOriginalFailure() {
        RuntimeException original = new RuntimeException("dashboard");
        RuntimeException entityManagerClose = new RuntimeException("entity manager close");
        RuntimeException databaseClose = new RuntimeException("database close");
        Target target = validTarget().withCloseFailure(entityManagerClose);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> AdminDashboardIT.runIntegration(target::open, () -> {
            throw original;
        }, validEnv()::get, () -> {
            throw databaseClose;
        }));

        assertSame(original, thrown);
        assertEquals(List.of(entityManagerClose, databaseClose), List.of(thrown.getSuppressed()));
        assertTrue(target.closeAttempted());
    }

    @Test
    void acceptsHealthyZeroBaselineAndNegativeNetCash() {
        assertDoesNotThrow(() -> AdminDashboardIT.assertHealthyDashboard(healthyDashboard()));
        Map<String, Object> negativeCash = healthyDashboard();
        negativeCash.put("netCashRevenueToday", new BigDecimal("-12.34"));
        assertDoesNotThrow(() -> AdminDashboardIT.assertHealthyDashboard(negativeCash));
    }

    @Test
    void rejectsInvalidCountTypesAndNegativeCounts() {
        for (Object[] mutation : List.of(
                new Object[] { "activeOrderCount", "0" },
                new Object[] { "pendingRefundCount", -1L },
                new Object[] { "pendingCodCount", 1.5 },
                new Object[] { "lowStockItemCount", -1 },
                new Object[] { "staffingGapCount", BigDecimal.ZERO })) {
            Map<String, Object> data = healthyDashboard();
            data.put((String) mutation[0], mutation[1]);
            assertDashboardRejected(data);
        }
    }

    @Test
    void rejectsInvalidCompletionCohortInvariants() {
        for (Object[] values : List.of(
                new Object[] { 0L, 1L, 0.0 },
                new Object[] { 0L, 0L, 1.0 },
                new Object[] { 1L, 2L, 200.0 },
                new Object[] { 3L, 1L, 50.0 },
                new Object[] { 3L, 1L, Double.NaN },
                new Object[] { 3L, 1L, Double.POSITIVE_INFINITY },
                new Object[] { 3L, 1L, -1.0 },
                new Object[] { 3L, 1L, 101.0 })) {
            Map<String, Object> data = healthyDashboard();
            data.put("operationalOrderCountToday", values[0]);
            data.put("operationalCompletedCountToday", values[1]);
            data.put("completionRateToday", values[2]);
            assertDashboardRejected(data);
        }
    }

    @Test
    void rejectsActiveOrderStatusAndSumInvariants() {
        for (Object count : List.of(-1L, 1.5)) {
            Map<String, Object> data = healthyDashboard();
            data.put("activeOrderCount", 1L);
            data.put("activeOrdersByStatus", Map.of("PENDING", count));
            assertDashboardRejected(data);
        }
        for (String status : List.of("DELIVERED", "CANCELLED", "RETURNED_TO_STORE")) {
            Map<String, Object> data = healthyDashboard();
            data.put("activeOrderCount", 1L);
            data.put("activeOrdersByStatus", Map.of(status, 1L));
            assertDashboardRejected(data);
        }
        Map<String, Object> mismatched = healthyDashboard();
        mismatched.put("activeOrderCount", 2L);
        mismatched.put("activeOrdersByStatus", Map.of("PENDING", 1L));
        assertDashboardRejected(mismatched);
    }

    @Test
    void rejectsSectionAvailabilityMutations() {
        for (Map<String, String> availability : List.of(
                Map.of("financial", "AVAILABLE"),
                availability("inventory", "UNAVAILABLE"),
                availability("inventory", "UNKNOWN"))) {
            Map<String, Object> data = healthyDashboard();
            data.put("sectionAvailability", availability);
            assertDashboardRejected(data);
        }
    }

    @Test
    void rejectsAttentionItemShapeTypeSeverityAndCountMutations() {
        List<Map<String, Object>> invalidItems = List.of(
                Map.of("type", "LOW_STOCK_ITEMS", "severity", "WARNING"),
                Map.of("type", "LOW_STOCK_ITEMS", "severity", "WARNING", "count", 1L, "extra", true),
                Map.of("type", "UNKNOWN", "severity", "WARNING", "count", 1L),
                Map.of("type", "LOW_STOCK_ITEMS", "severity", "INFO", "count", 1L),
                Map.of("type", "LOW_STOCK_ITEMS", "severity", "WARNING", "count", 0L),
                Map.of("type", "LOW_STOCK_ITEMS", "severity", "WARNING", "count", 1.5));
        for (Map<String, Object> item : invalidItems) {
            Map<String, Object> data = healthyDashboard();
            data.put("attentionItems", List.of(item));
            assertDashboardRejected(data);
        }
    }

    @Test
    void acceptsEveryKnownAttentionTypeAndSeverity() {
        List<Map<String, Object>> items = new ArrayList<>();
        int index = 0;
        for (String type : List.of("OVERDUE_PENDING_ORDERS", "DELIVERY_FAILED_ORDERS", "PENDING_REFUNDS", "STAFF_COVERAGE_GAPS", "LOW_STOCK_ITEMS", "PENDING_COD_SETTLEMENTS")) {
            items.add(Map.of("type", type, "severity", index++ % 2 == 0 ? "WARNING" : "CRITICAL", "count", 1L));
        }
        Map<String, Object> data = healthyDashboard();
        data.put("attentionItems", items);
        assertDoesNotThrow(() -> AdminDashboardIT.assertHealthyDashboard(data));
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

    private static void assertDashboardRejected(Map<String, Object> data) {
        assertThrows(AssertionError.class, () -> AdminDashboardIT.assertHealthyDashboard(data));
    }

    private static Map<String, String> validEnv() {
        Map<String, String> env = new HashMap<>();
        env.put("FASTGUY_DISPOSABLE_DB", "true");
        env.put("FASTGUY_E2E_DB_NAME", DATABASE);
        env.put("FASTGUY_E2E_DB_SERVER", SERVER);
        env.put("DB_URL", validUrl());
        return env;
    }

    private static String validUrl() {
        return "jdbc:sqlserver://" + SERVER + ":1433;encrypt=true;databaseName=" + DATABASE + ";trustServerCertificate=true";
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
        data.put("sectionAvailability", availability(null, null));
        return data;
    }

    private static Map<String, String> availability(String changedSection, String value) {
        Map<String, String> availability = new LinkedHashMap<>();
        for (String section : List.of("financial", "orders", "refunds", "cod", "inventory", "staffing")) availability.put(section, section.equals(changedSection) ? value : "AVAILABLE");
        return availability;
    }

    private static <T> List<T> listWithNull(T... values) {
        List<T> result = new ArrayList<>();
        for (T value : values) result.add(value);
        return result;
    }

    private static void putOrRemove(Map<String, String> map, String key, String value) {
        if (value == null) map.remove(key);
        else map.put(key, value);
    }

    private static final class Target {
        private final Object[] identity;
        private final List<String> migrations;
        private final List<String> queries = new ArrayList<>();
        private final AtomicInteger opens = new AtomicInteger();
        private final AtomicBoolean catalogComplete = new AtomicBoolean();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicBoolean closeAttempted = new AtomicBoolean();
        private RuntimeException closeFailure;

        private Target(Object[] identity, List<String> migrations) {
            this.identity = identity;
            this.migrations = migrations;
        }

        private Target withCloseFailure(RuntimeException failure) {
            closeFailure = failure;
            return this;
        }

        private EntityManager open() {
            opens.incrementAndGet();
            return (EntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(), new Class<?>[] { EntityManager.class }, (proxy, method, args) -> switch (method.getName()) {
                case "createNativeQuery" -> query((String) args[0]);
                case "isOpen" -> !closed.get();
                case "close" -> {
                    closeAttempted.set(true);
                    closed.set(true);
                    if (closeFailure != null) throw closeFailure;
                    yield null;
                }
                default -> throw new UnsupportedOperationException(method.getName());
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
        private boolean closeAttempted() { return closeAttempted.get(); }
    }
}
