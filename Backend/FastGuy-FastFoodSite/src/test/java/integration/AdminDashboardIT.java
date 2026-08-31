package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import service.AdminService;
import utils.DatabaseUtil;

class AdminDashboardIT {
    private static final String DATABASE = "FastGuyDB_Operations060_Test";
    private static final List<String> MIGRATIONS = List.of("059_shift_schedule_order_timeout", "060_operating_finance");
    private static final Set<String> SECTIONS = Set.of("financial", "orders", "refunds", "cod", "inventory", "staffing");
    private static final Set<String> ACTIVE_STATUSES = Set.of("PENDING", "CONFIRMED", "PREPARING", "READY", "ASSIGNED", "PICKED_UP", "DELIVERY_FAILED");
    private static final Set<String> ATTENTION_TYPES = Set.of("OVERDUE_PENDING_ORDERS", "DELIVERY_FAILED_ORDERS", "PENDING_REFUNDS", "STAFF_COVERAGE_GAPS", "LOW_STOCK_ITEMS", "PENDING_COD_SETTLEMENTS");
    private static final Set<String> ATTENTION_SEVERITIES = Set.of("WARNING", "CRITICAL");
    private static final Set<String> JDBC_PROPERTIES = Set.of("databaseName", "encrypt", "trustServerCertificate", "sendTimeAsDatetime");

    @Test
    void dashboardBuildsDecisionKpisOnDisposableDatabase() {
        runIntegration(DatabaseUtil::getEntityManager, () -> assertHealthyDashboard(new AdminService().getDashboard()), System::getenv, DatabaseUtil::close);
    }

    static void runIntegration(Supplier<EntityManager> entityManagers, Runnable dashboardAction, EnvReader env, Runnable databaseClose) {
        Throwable failure = null;
        try {
            runChecked(entityManagers, dashboardAction, env);
        } catch (RuntimeException | Error exception) {
            failure = exception;
            throw exception;
        } finally {
            try {
                databaseClose.run();
            } catch (RuntimeException | Error closeFailure) {
                if (failure != null) failure.addSuppressed(closeFailure);
                else throw closeFailure;
            }
        }
    }

    static void runChecked(Supplier<EntityManager> entityManagers, Runnable dashboardAction, EnvReader env) {
        verifyPreBootstrap(env);
        EntityManager em = null;
        Throwable failure = null;
        try {
            em = entityManagers.get();
            verifyTarget(em, env.get("FASTGUY_E2E_DB_SERVER"));
            dashboardAction.run();
        } catch (RuntimeException | Error exception) {
            failure = exception;
            throw exception;
        } finally {
            try {
                if (em != null && em.isOpen()) em.close();
            } catch (RuntimeException | Error closeFailure) {
                if (failure != null) failure.addSuppressed(closeFailure);
                else throw closeFailure;
            }
        }
    }

    static void verifyPreBootstrap(EnvReader env) {
        if (!"true".equals(env.get("FASTGUY_DISPOSABLE_DB"))) throw new IllegalStateException("FASTGUY_DISPOSABLE_DB must be exactly true");
        if (!DATABASE.equals(env.get("FASTGUY_E2E_DB_NAME"))) throw new IllegalStateException("FASTGUY_E2E_DB_NAME must identify the disposable database");
        String expectedServer = env.get("FASTGUY_E2E_DB_SERVER");
        if (expectedServer == null || !expectedServer.matches("[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?")) throw new IllegalStateException("FASTGUY_E2E_DB_SERVER must be an exact nonblank server name");
        verifyJdbcUrl(env.get("DB_URL"), expectedServer);
    }

    private static void verifyJdbcUrl(String url, String expectedServer) {
        String prefix = "jdbc:sqlserver://";
        if (url == null || !url.startsWith(prefix) || url.indexOf('?') >= 0 || url.indexOf('#') >= 0 || url.endsWith(";")) throw new IllegalStateException("DB_URL must use the approved SQL Server JDBC form");
        int propertiesStart = url.indexOf(';', prefix.length());
        if (propertiesStart < 0) throw new IllegalStateException("DB_URL must contain databaseName");
        String authority = url.substring(prefix.length(), propertiesStart);
        String server = authority;
        int portSeparator = authority.indexOf(':');
        if (portSeparator >= 0) {
            if (authority.indexOf(':', portSeparator + 1) >= 0) throw new IllegalStateException("DB_URL authority is invalid");
            server = authority.substring(0, portSeparator);
            String portText = authority.substring(portSeparator + 1);
            if (!portText.matches("[0-9]+")) throw new IllegalStateException("DB_URL port is invalid");
            int port;
            try {
                port = Integer.parseInt(portText);
            } catch (NumberFormatException exception) {
                throw new IllegalStateException("DB_URL port is invalid");
            }
            if (port < 1 || port > 65535) throw new IllegalStateException("DB_URL port is invalid");
        }
        if (!server.matches("[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?") || !expectedServer.equals(server)) throw new IllegalStateException("DB_URL server does not match FASTGUY_E2E_DB_SERVER");

        Set<String> keys = new HashSet<>();
        String database = null;
        String properties = url.substring(propertiesStart + 1);
        if (properties.isEmpty()) throw new IllegalStateException("DB_URL must contain databaseName");
        for (String property : properties.split(";", -1)) {
            int separator = property.indexOf('=');
            if (separator <= 0 || separator == property.length() - 1 || property.indexOf('=', separator + 1) >= 0) throw new IllegalStateException("DB_URL property is invalid");
            String key = property.substring(0, separator);
            String value = property.substring(separator + 1);
            if (!JDBC_PROPERTIES.contains(key) || !keys.add(key)) throw new IllegalStateException("DB_URL contains an unsupported or duplicate property");
            if ("databaseName".equals(key)) database = value;
            else if (!"true".equals(value) && !"false".equals(value)) throw new IllegalStateException("DB_URL boolean property is invalid");
        }
        if (!DATABASE.equals(database)) throw new IllegalStateException("DB_URL databaseName does not identify the disposable database");
    }

    static void verifyTarget(EntityManager em, String expectedServer) {
        Object result = em.createNativeQuery("SELECT @@SERVERNAME,DB_NAME(),DATABASEPROPERTYEX(DB_NAME(),'Status'),CAST(compatibility_level AS int) FROM sys.databases WHERE name=DB_NAME()").getSingleResult();
        if (!(result instanceof Object[] identity) || identity.length != 4) throw new IllegalStateException("Database identity query returned an invalid result");
        if (!expectedServer.equals(identity[0])) throw new IllegalStateException("Connected SQL Server does not match FASTGUY_E2E_DB_SERVER");
        if (!DATABASE.equals(identity[1])) throw new IllegalStateException("Connected database is not the disposable target");
        if (!"ONLINE".equals(identity[2])) throw new IllegalStateException("Database must be ONLINE");
        if (!(identity[3] instanceof Number compatibility) || compatibility.intValue() < 160) throw new IllegalStateException("Database compatibility level must be at least 160");
        List<?> migrations = em.createNativeQuery("SELECT migration_id FROM dbo.SchemaMigrationHistory WHERE migration_id IN ('059_shift_schedule_order_timeout','060_operating_finance') ORDER BY migration_id").getResultList();
        if (!MIGRATIONS.equals(migrations)) throw new IllegalStateException("Required migrations 059 and 060 must be present exactly");
    }

    static void assertHealthyDashboard(Map<String, Object> data) {
        assertInstanceOf(BigDecimal.class, data.get("netCashRevenueToday"));
        long activeOrderCount = count(data.get("activeOrderCount"), "activeOrderCount", false);
        for (String field : List.of("pendingRefundCount", "pendingCodCount", "lowStockItemCount", "staffingGapCount")) count(data.get(field), field, false);
        long operationalOrders = count(data.get("operationalOrderCountToday"), "operationalOrderCountToday", false);
        long operationalCompleted = count(data.get("operationalCompletedCountToday"), "operationalCompletedCountToday", false);
        Number completionRateValue = assertInstanceOf(Number.class, data.get("completionRateToday"));
        double completionRate = completionRateValue.doubleValue();
        assertTrue(Double.isFinite(completionRate) && completionRate >= 0 && completionRate <= 100, "completionRateToday");
        if (operationalOrders == 0) {
            assertEquals(0L, operationalCompleted, "operationalCompletedCountToday");
            assertEquals(0.0, completionRate, "completionRateToday");
        } else {
            assertTrue(operationalCompleted <= operationalOrders, "operationalCompletedCountToday");
            assertEquals(operationalCompleted * 100.0 / operationalOrders, completionRate, 0.000001, "completionRateToday");
        }

        Map<?, ?> activeOrdersByStatus = assertInstanceOf(Map.class, data.get("activeOrdersByStatus"));
        long activeStatusTotal = 0;
        for (Map.Entry<?, ?> entry : activeOrdersByStatus.entrySet()) {
            String status = assertInstanceOf(String.class, entry.getKey());
            assertTrue(ACTIVE_STATUSES.contains(status), "activeOrdersByStatus." + status);
            activeStatusTotal = Math.addExact(activeStatusTotal, count(entry.getValue(), "activeOrdersByStatus." + status, false));
        }
        assertEquals(activeOrderCount, activeStatusTotal, "activeOrderCount");

        Map<?, ?> availability = assertInstanceOf(Map.class, data.get("sectionAvailability"));
        assertEquals(SECTIONS, availability.keySet());
        availability.forEach((section, value) -> assertEquals("AVAILABLE", value, "sectionAvailability." + section));

        List<?> attentionItems = assertInstanceOf(List.class, data.get("attentionItems"));
        for (Object rawItem : attentionItems) {
            Map<?, ?> item = assertInstanceOf(Map.class, rawItem);
            assertEquals(Set.of("type", "severity", "count"), item.keySet());
            String type = assertInstanceOf(String.class, item.get("type"));
            String severity = assertInstanceOf(String.class, item.get("severity"));
            assertTrue(ATTENTION_TYPES.contains(type), "attentionItems.type");
            assertTrue(ATTENTION_SEVERITIES.contains(severity), "attentionItems.severity");
            count(item.get("count"), "attentionItems.count", true);
        }
    }

    private static long count(Object value, String field, boolean positive) {
        assertTrue(value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long, field + " must be an integral count");
        long count = ((Number) value).longValue();
        assertTrue(positive ? count > 0 : count >= 0, field);
        return count;
    }

    @FunctionalInterface
    interface EnvReader {
        String get(String name);
    }
}
