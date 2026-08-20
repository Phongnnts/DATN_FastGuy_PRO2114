package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dao.OrdersDAO;

class AdminReportingIT {
    @Test
    void financialBreakdownReconcilesAndOperationalCohortIsBounded() {
        OrdersDAO dao = new OrdersDAO();
        LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2100, 1, 1, 0, 0);

        Map<String, Double> financial = dao.financialBreakdown(start, end);
        double expectedGross = financial.get("itemRevenue") + financial.get("shippingRevenue")
                + financial.get("serviceFeeRevenue") - financial.get("discountTotal");
        assertEquals(expectedGross, financial.get("grossRevenue"), 0.01);

        long[] operational = dao.operationalCohortSummary(start, end);
        assertTrue(operational[0] >= operational[1]);
        assertTrue(operational[0] >= 0);
        assertTrue(dao.monthlyFinancialTrend(start, end).stream().allMatch(row -> Math.abs(((Number) row.get("grossRevenue")).doubleValue() - ((Number) row.get("refundTotal")).doubleValue() - ((Number) row.get("netCashRevenue")).doubleValue()) < 0.01));
        assertTrue(dao.revenueByHour(start, end).stream().allMatch(row -> ((Number) row.get("hour")).intValue() >= 0));
        assertTrue(dao.performanceByWeekday(start, end).stream().allMatch(row -> ((Number) row.get("weekday")).intValue() >= 1));
        assertTrue(dao.refundTrend(start, end).stream().allMatch(row -> ((Number) row.get("amount")).doubleValue() >= 0));
        assertTrue(dao.exceptionReasons(start, end).stream().allMatch(row -> row.get("reason") != null));
    }
}
