package integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import service.AdminService;
import utils.DatabaseUtil;

class AdminDashboardIT {
    private static final String DATABASE = "FastGuyDB_Operations060_Test";

    @Test
    void dashboardBuildsDecisionKpisOnDisposableDatabase() {
        String disposableFlag = System.getenv("FASTGUY_DISPOSABLE_DB");
        if (!"true".equalsIgnoreCase(disposableFlag)) throw new IllegalStateException("FASTGUY_DISPOSABLE_DB=true is required");
        EntityManager em = null;
        try {
            em = DatabaseUtil.getEntityManager();
            verifyTarget(em, disposableFlag);
            Map<String, Object> data = new AdminService().getDashboard();
            for (String field : new String[] { "netCashRevenueToday", "activeOrderCount", "pendingRefundCount", "pendingCodCount", "lowStockItemCount", "staffingGapCount", "activeOrdersByStatus", "operationalOrderCountToday", "operationalCompletedCountToday", "completionRateToday", "attentionItems", "sectionAvailability" }) {
                assertTrue(data.containsKey(field), field);
            }
        } finally {
            try {
                if (em != null && em.isOpen()) em.close();
            } finally {
                DatabaseUtil.close();
            }
        }
    }

    static void verifyTarget(EntityManager em, String disposableFlag) {
        if (!"true".equalsIgnoreCase(disposableFlag)) throw new IllegalStateException("FASTGUY_DISPOSABLE_DB=true is required");
        Object result = em.createNativeQuery("SELECT @@SERVERNAME,DB_NAME(),DATABASEPROPERTYEX(DB_NAME(),'Status'),CAST(compatibility_level AS int) FROM sys.databases WHERE name=DB_NAME()").getSingleResult();
        if (!(result instanceof Object[] identity) || identity.length != 4) throw new IllegalStateException("Database identity query returned an invalid result");
        if (!(identity[0] instanceof String server) || server.isBlank()) throw new IllegalStateException("@@SERVERNAME must be nonblank");
        if (!DATABASE.equals(identity[1])) throw new IllegalStateException("Expected database " + DATABASE + " but was " + identity[1]);
        if (!"ONLINE".equals(identity[2])) throw new IllegalStateException("Database must be ONLINE");
        if (!(identity[3] instanceof Number compatibility) || compatibility.intValue() < 160) throw new IllegalStateException("Database compatibility level must be at least 160");
        Object migrationResult = em.createNativeQuery("SELECT COUNT_BIG(*) FROM dbo.SchemaMigrationHistory WHERE migration_id IN ('059_shift_schedule_order_timeout','060_operating_finance')").getSingleResult();
        if (!(migrationResult instanceof Number migrations) || migrations.longValue() != 2L) throw new IllegalStateException("Required migrations 059_shift_schedule_order_timeout and 060_operating_finance must be present");
    }
}
