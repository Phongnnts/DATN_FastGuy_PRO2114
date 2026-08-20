package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dao.OrdersDAO;

class AdminRefundMetricsPolicyTest {

    static class StubOrdersDAO extends OrdersDAO {
        double gross = 0.0;
        double refund = 0.0;
        long refundCount = 0L;

        @Override
        public double sumRevenueByDateRange(LocalDateTime start, LocalDateTime end) {
            return gross;
        }

        @Override
        public Map<String, Double> financialBreakdown(LocalDateTime start, LocalDateTime end) {
            return Map.of("itemRevenue", gross, "shippingRevenue", 0.0, "serviceFeeRevenue", 0.0, "discountTotal", 0.0, "grossRevenue", gross);
        }

        @Override
        public long[] operationalCohortSummary(LocalDateTime start, LocalDateTime end) {
            return new long[]{0L, 0L};
        }

        @Override
        public double sumRefundsInRange(LocalDateTime start, LocalDateTime end) {
            return refund;
        }

        @Override
        public long countRefundsInRange(LocalDateTime start, LocalDateTime end) {
            return refundCount;
        }

        @Override
        public double sumRevenueToday() {
            return 0.0;
        }

        @Override
        public long countToday() {
            return 0L;
        }

        @Override
        public long countByStatusAndDateRange(String status, LocalDateTime start, LocalDateTime end) {
            return 0L;
        }

        @Override
        public long countAllByDateRange(LocalDateTime start, LocalDateTime end) {
            return 0L;
        }

        @Override
        public double avgOrderValue(LocalDateTime start, LocalDateTime end) {
            return 0.0;
        }

        @Override
        public List<Map<String, Object>> sumRevenueByCustomRange(LocalDateTime start, LocalDateTime end) {
            return new ArrayList<>();
        }

        @Override
        public List<Map<String, Object>> revenueByDay(LocalDateTime start, LocalDateTime end) {
            return new ArrayList<>();
        }

        @Override
        public List<Map<String, Object>> findTopProductsByDateRange(LocalDateTime start, LocalDateTime end, int limit) {
            return new ArrayList<>();
        }

        @Override
        public List<Map<String, Object>> ordersByStatusInPeriod(LocalDateTime start, LocalDateTime end) {
            return new ArrayList<>();
        }

        @Override
        public List<Map<String, Object>> revenueByCategory(LocalDateTime start, LocalDateTime end) {
            return new ArrayList<>();
        }

        @Override
        public List<Map<String, Object>> paymentMethodStats(LocalDateTime start, LocalDateTime end) { return new ArrayList<>(); }
        @Override
        public List<Map<String, Object>> monthlyFinancialTrend(LocalDateTime start, LocalDateTime end) { return new ArrayList<>(); }
        @Override
        public List<Map<String, Object>> revenueByHour(LocalDateTime start, LocalDateTime end) { return new ArrayList<>(); }
        @Override
        public List<Map<String, Object>> performanceByWeekday(LocalDateTime start, LocalDateTime end) { return new ArrayList<>(); }
        @Override
        public List<Map<String, Object>> refundTrend(LocalDateTime start, LocalDateTime end) { return new ArrayList<>(); }
        @Override
        public List<Map<String, Object>> exceptionReasons(LocalDateTime start, LocalDateTime end) { return new ArrayList<>(); }
    }

    @Test
    void fullReportPlacesRefundMetricsAndNetRevenue() throws Exception {
        StubOrdersDAO stub = new StubOrdersDAO();
        stub.gross = 1_000_000.0;
        stub.refund = 120_000.0;
        stub.refundCount = 3L;

        AdminService service = new AdminService();
        Field dao = AdminService.class.getDeclaredField("ordersDAO");
        dao.setAccessible(true);
        dao.set(service, stub);

        Map<String, Object> data = service.getFullReport("7d", null, null);

        assertEquals(1_000_000.0, (double) data.get("grossRevenue"));
        assertEquals(1_000_000.0, (double) data.get("periodRevenue"));
        assertEquals(120_000.0, (double) data.get("refundTotal"));
        assertEquals(3L, data.get("refundCount"));
        assertEquals(880_000.0, (double) data.get("netRevenue"));
        assertEquals((double) data.get("grossRevenue") - (double) data.get("refundTotal"), (double) data.get("netRevenue"));
    }

    @Test
    void refundQueriesFilterOnRefundedStatusAndRefundedAtBounds() throws Exception {
        String source = Files.readString(Path.of("src/main/java/dao/OrdersDAO.java"));
        int sum = source.indexOf("public double sumRefundsInRange");
        int count = source.indexOf("public long countRefundsInRange");
        assertTrue(sum >= 0 && count > sum);
        String sumQuery = source.substring(sum, count);
        assertTrue(sumQuery.contains("o.refundStatus = 'REFUNDED'"));
        assertTrue(sumQuery.contains("o.refundedAt >= :start"));
        assertTrue(sumQuery.contains("o.refundedAt < :end"));
        assertTrue(sumQuery.contains("result != null ? result.doubleValue() : 0.0"));
        assertTrue(source.substring(count).contains("o.refundStatus = 'REFUNDED'"));
    }
}
