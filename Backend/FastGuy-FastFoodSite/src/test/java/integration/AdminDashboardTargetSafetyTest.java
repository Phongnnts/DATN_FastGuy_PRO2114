package integration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

class AdminDashboardTargetSafetyTest {
    private static final String DATABASE = "FastGuyDB_Operations060_Test";

    @Test
    void rejectsMissingDisposableFlagBeforeAnyQueryOrDashboardWork() {
        Target target = new Target(identity(DATABASE, "ONLINE", 160), 2L);

        assertRejected(target, null);

        assertEquals(0, target.queryCount());
    }

    @Test
    void rejectsWrongDatabaseBeforeDashboardWork() {
        assertRejected(new Target(identity("FastGuyDB", "ONLINE", 160), 2L), "true");
    }

    @Test
    void rejectsOfflineDatabaseBeforeDashboardWork() {
        assertRejected(new Target(identity(DATABASE, "OFFLINE", 160), 2L), "true");
    }

    @Test
    void rejectsLowCompatibilityBeforeDashboardWork() {
        assertRejected(new Target(identity(DATABASE, "ONLINE", 159), 2L), "true");
    }

    @Test
    void rejectsEitherMissingRequiredMigrationBeforeDashboardWork() {
        assertRejected(new Target(identity(DATABASE, "ONLINE", 160), 1L), "true");
        assertRejected(new Target(identity(DATABASE, "ONLINE", 160), 0L), "true");
    }

    @Test
    void acceptsExactDisposableTargetIdentity() {
        Target target = new Target(identity(DATABASE, "ONLINE", 160), 2L);
        AtomicBoolean dashboardWork = new AtomicBoolean();

        assertDoesNotThrow(() -> {
            AdminDashboardIT.verifyTarget(target.entityManager(), "true");
            dashboardWork.set(true);
        });

        assertTrue(dashboardWork.get());
        assertEquals(2, target.queryCount());
    }

    private static void assertRejected(Target target, String disposableFlag) {
        AtomicBoolean dashboardWork = new AtomicBoolean();

        assertThrows(IllegalStateException.class, () -> {
            AdminDashboardIT.verifyTarget(target.entityManager(), disposableFlag);
            dashboardWork.set(true);
        });

        assertFalse(dashboardWork.get());
    }

    private static Object[] identity(String database, String status, int compatibility) {
        return new Object[] { "FASTGUY-SQL", database, status, compatibility };
    }

    private static final class Target {
        private final Object[] identity;
        private final long migrationCount;
        private final AtomicInteger queryCount = new AtomicInteger();

        private Target(Object[] identity, long migrationCount) {
            this.identity = identity;
            this.migrationCount = migrationCount;
        }

        private int queryCount() {
            return queryCount.get();
        }

        private EntityManager entityManager() {
            return (EntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(), new Class<?>[] { EntityManager.class }, (proxy, method, args) -> {
                if (!"createNativeQuery".equals(method.getName())) throw new UnsupportedOperationException(method.getName());
                queryCount.incrementAndGet();
                String sql = (String) args[0];
                Object result;
                if (sql.contains("@@SERVERNAME")) result = identity;
                else if (sql.contains("SchemaMigrationHistory")) result = migrationCount;
                else throw new AssertionError("Unexpected query: " + sql);
                return query(result);
            });
        }

        private Query query(Object result) {
            return (Query) Proxy.newProxyInstance(Query.class.getClassLoader(), new Class<?>[] { Query.class }, (proxy, method, args) -> {
                if ("getSingleResult".equals(method.getName())) return result;
                throw new UnsupportedOperationException(method.getName());
            });
        }
    }
}
