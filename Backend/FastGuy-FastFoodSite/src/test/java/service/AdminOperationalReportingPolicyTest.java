package service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AdminOperationalReportingPolicyTest {
    @Test
    void serviceSeparatesOperationalCohortFromFinancialEvents() throws Exception {
        String source = Files.readString(Path.of("src/main/java/service/AdminService.java"));
        assertTrue(source.contains("ZoneId.of(\"Asia/Ho_Chi_Minh\")"));
        assertTrue(source.contains("customerCount"));
        assertTrue(source.contains("activeProductCount"));
        assertTrue(source.contains("operationalOrderCount"));
        assertTrue(source.contains("operationalCompletedCount"));
        assertTrue(source.contains("completionRate"));
        for (String field : new String[] {"itemRevenue", "shippingRevenue", "serviceFeeRevenue", "discountTotal", "grossRevenue", "refundTotal", "netCashRevenue"}) {
            assertTrue(source.contains("\"" + field + "\""), field);
        }
    }

    @Test
    void daoUsesCreatedCohortAndReconcilableFinancialQueries() throws Exception {
        String source = Files.readString(Path.of("src/main/java/dao/OrdersDAO.java"));
        assertTrue(source.contains("operationalCohortSummary"));
        assertTrue(source.contains("financialBreakdown"));
        assertTrue(source.contains("revenueByHour"));
        assertTrue(source.contains("performanceByWeekday"));
        assertTrue(source.contains("refundTrend"));
        assertTrue(source.contains("exceptionReasons"));
        assertTrue(source.contains("monthlyFinancialTrend"));
        assertTrue(source.contains("GROUP BY p.product_id, p.name"));
        assertTrue(source.contains("findAllByCreatedAtRange"));
    }

    @Test
    void dashboardIncludesDeliveryExceptionStatuses() throws Exception {
        String source = Files.readString(Path.of("src/main/java/service/AdminService.java"));
        assertTrue(source.contains("DELIVERY_FAILED"));
        assertTrue(source.contains("RETURNED_TO_STORE"));
    }
}
