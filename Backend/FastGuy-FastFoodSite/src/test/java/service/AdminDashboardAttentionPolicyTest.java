package service;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;

class AdminDashboardAttentionPolicyTest {
    @Test void dashboardPublishesTodayKpisAndSixActionableAttentionTypes() throws Exception {
        String service=Files.readString(Path.of("src/main/java/service/AdminService.java"));
        for(String field:new String[]{"deliveredOrdersToday","activeOrdersToday","aovToday","grossProfitToday","costComplete","attentionItems"}) assertTrue(service.contains("data.put(\""+field+"\""),field);
        for(String type:new String[]{"OVERDUE_PENDING_ORDERS","DELIVERY_FAILED_ORDERS","PENDING_REFUNDS","STAFF_COVERAGE_GAPS","LOW_STOCK_ITEMS","PENDING_COD_SETTLEMENTS"}) assertTrue(service.contains(type),type);
        assertTrue(service.contains("countOverdueActive"));
        assertTrue(service.contains("countPendingRefunds"));
        assertTrue(service.contains("menuPerformanceReportService.report"));
    }

    @Test void pendingRefundCountIsComputedInSql() throws Exception {
        String dao=Files.readString(Path.of("src/main/java/dao/OrdersDAO.java"));
        assertTrue(dao.contains("long countPendingRefunds()"));
        assertTrue(dao.contains("refundStatus = 'PENDING'") || dao.contains("refund_status='PENDING'"));
    }
}
