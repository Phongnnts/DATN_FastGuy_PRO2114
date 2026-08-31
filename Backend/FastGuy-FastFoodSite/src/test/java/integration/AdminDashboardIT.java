package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
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

    @Test
    void dashboardBuildsDecisionKpisOnDisposableDatabase() {
        Throwable failure = null;
        try {
            runChecked(DatabaseUtil::getEntityManager, () -> assertHealthyDashboard(new AdminService().getDashboard()), System::getenv);
        } catch (RuntimeException | Error exception) {
            failure = exception;
            throw exception;
        } finally {
            try {
                DatabaseUtil.close();
            } catch (RuntimeException closeFailure) {
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
            } catch (RuntimeException closeFailure) {
                if (failure != null) failure.addSuppressed(closeFailure);
                else throw closeFailure;
            }
        }
    }

    static void verifyPreBootstrap(EnvReader env) {
        if (!"true".equals(env.get("FASTGUY_DISPOSABLE_DB"))) throw new IllegalStateException("FASTGUY_DISPOSABLE_DB must be exactly true");
        if (!DATABASE.equals(env.get("FASTGUY_E2E_DB_NAME"))) throw new IllegalStateException("FASTGUY_E2E_DB_NAME must identify the disposable database");
        String expectedServer = env.get("FASTGUY_E2E_DB_SERVER");
        if (expectedServer == null || expectedServer.isBlank() || !expectedServer.equals(expectedServer.trim())) throw new IllegalStateException("FASTGUY_E2E_DB_SERVER must be an exact nonblank server name");
        String url = env.get("DB_URL");
        if (!expectedServer.equals(jdbcServer(url))) throw new IllegalStateException("DB_URL server does not match FASTGUY_E2E_DB_SERVER");
        if (!DATABASE.equals(jdbcDatabase(url))) throw new IllegalStateException("DB_URL databaseName does not identify the disposable database");
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
        BigDecimal netCashRevenue = assertInstanceOf(BigDecimal.class, data.get("netCashRevenueToday"));
        assertTrue(netCashRevenue.signum() >= 0, "netCashRevenueToday");
        for (String field : List.of("activeOrderCount", "pendingRefundCount", "pendingCodCount", "lowStockItemCount", "staffingGapCount", "operationalOrderCountToday", "operationalCompletedCountToday")) assertNonnegativeCount(data.get(field), field);
        Number completionRate = assertInstanceOf(Number.class, data.get("completionRateToday"));
        assertTrue(completionRate.doubleValue() >= 0, "completionRateToday");
        Map<?, ?> activeOrdersByStatus = assertInstanceOf(Map.class, data.get("activeOrdersByStatus"));
        activeOrdersByStatus.forEach((status, count) -> {
            assertInstanceOf(String.class, status);
            assertNonnegativeCount(count, "activeOrdersByStatus." + status);
        });
        assertInstanceOf(List.class, data.get("attentionItems"));
        Map<?, ?> availability = assertInstanceOf(Map.class, data.get("sectionAvailability"));
        assertEquals(SECTIONS, availability.keySet());
        availability.forEach((section, value) -> {
            assertInstanceOf(String.class, section);
            assertTrue(Set.of("AVAILABLE", "UNAVAILABLE").contains(value), "sectionAvailability." + section);
            assertEquals("AVAILABLE", value, "sectionAvailability." + section);
        });
    }

    private static void assertNonnegativeCount(Object value, String field) {
        assertTrue(value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long, field + " must be an integral count");
        assertTrue(((Number) value).longValue() >= 0, field);
    }

    private static String jdbcServer(String url) {
        String authority = jdbcAuthority(url);
        int portSeparator = authority.lastIndexOf(':');
        if (portSeparator > 0 && authority.substring(portSeparator + 1).chars().allMatch(Character::isDigit)) authority = authority.substring(0, portSeparator);
        if (authority.isBlank()) throw new IllegalStateException("DB_URL must contain a server");
        return authority;
    }

    private static String jdbcDatabase(String url) {
        jdbcAuthority(url);
        String database = null;
        for (String property : url.split(";", -1)) {
            int separator = property.indexOf('=');
            if (separator < 0 || !"databaseName".equalsIgnoreCase(property.substring(0, separator))) continue;
            if (database != null) throw new IllegalStateException("DB_URL must contain one databaseName");
            database = property.substring(separator + 1);
        }
        if (database == null || database.isBlank()) throw new IllegalStateException("DB_URL must contain databaseName");
        return database;
    }

    private static String jdbcAuthority(String url) {
        String prefix = "jdbc:sqlserver://";
        if (url == null || !url.startsWith(prefix)) throw new IllegalStateException("DB_URL must be a SQL Server JDBC URL");
        int end = url.indexOf(';', prefix.length());
        return url.substring(prefix.length(), end < 0 ? url.length() : end);
    }

    @FunctionalInterface
    interface EnvReader {
        String get(String name);
    }
}
