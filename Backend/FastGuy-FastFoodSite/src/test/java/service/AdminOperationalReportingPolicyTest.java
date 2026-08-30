package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dao.CodSettlementDAO;
import dao.InventoryItemDAO;
import dao.OrdersDAO;
import dao.ProductDAO;
import dao.UserDAO;
import entity.InventoryItem;
import entity.Orders;

class AdminOperationalReportingPolicyTest {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Test
    void dashboardUsesCarryOverOrdersAndEventTimeFinancialTruth() throws Exception {
        Map<String, Object> data = new AdminDashboardTestFixture().service().getDashboard();

        assertEquals(1L, data.get("activeOrderCount"));
        assertEquals(Map.of("PREPARING", 1L), data.get("activeOrdersByStatus"));
        assertEquals(new BigDecimal("80.00"), data.get("netCashRevenueToday"));
        assertInstanceOf(BigDecimal.class, data.get("netCashRevenueToday"));
        assertEquals(1L, data.get("pendingRefundCount"));
        assertEquals(2L, data.get("operationalOrderCountToday"));
        assertEquals(2L, data.get("operationalCompletedCountToday"));
        assertEquals(100.0, data.get("completionRateToday"));
    }

    @Test
    void eachFailedProviderReadMarksOnlyItsOwnSectionUnavailable() throws Exception {
        for (String failed : List.of("financial", "orders", "refunds", "cod", "inventory", "staffing")) {
            AdminDashboardTestFixture fixture = new AdminDashboardTestFixture();
            fixture.failedSection = failed;

            Map<String, Object> data = fixture.service().getDashboard();
            @SuppressWarnings("unchecked")
            Map<String, String> availability = (Map<String, String>) data.get("sectionAvailability");

            for (String section : List.of("financial", "orders", "refunds", "cod", "inventory", "staffing")) {
                assertEquals(section.equals(failed) ? "UNAVAILABLE" : "AVAILABLE", availability.get(section), failed + ":" + section);
            }
            if (!"orders".equals(failed)) assertEquals(1L, data.get("activeOrderCount"));
            if (!"cod".equals(failed)) assertEquals(2L, data.get("pendingCodCount"));
            if (!"inventory".equals(failed)) assertEquals(2L, data.get("lowStockItemCount"));
            if (!"staffing".equals(failed)) assertEquals(1L, data.get("staffingGapCount"));
        }
    }

    @Test
    void dashboardRetainsCanonicalAndCompatibilityFields() throws Exception {
        Map<String, Object> data = new AdminDashboardTestFixture().service().getDashboard();
        Set<String> required = Set.of(
                "netCashRevenueToday", "activeOrderCount", "pendingRefundCount", "pendingCodCount", "lowStockItemCount", "staffingGapCount",
                "activeOrdersByStatus", "operationalOrderCountToday", "operationalCompletedCountToday", "completionRateToday", "attentionItems", "sectionAvailability",
                "customerCount", "totalUsers", "totalOrders", "activeProductCount", "totalProducts", "ordersByStatus", "operationalOrderCount",
                "operationalCompletedCount", "completionRate", "totalRevenue", "pendingOrders", "revenueToday", "ordersToday", "pendingCodAmount",
                "revenueByMonth", "topProducts", "lowStockThreshold", "outOfStockSkuCount", "lowStockSkuCount", "deliveredOrdersToday",
                "activeOrdersToday", "aovToday", "grossProfitToday", "costComplete");

        assertTrue(data.keySet().containsAll(required));
        Map<String, Object> period = new AdminDashboardTestFixture().service().getDashboardWithPeriod("7d");
        assertTrue(period.keySet().containsAll(Set.of("grossRevenue", "periodRevenue", "refundTotal", "refundCount", "netRevenue", "periodOrders", "periodTopProducts")));
    }

    @Test
    void daoQueriesUseCurrentStateAndFinancialEventTimes() throws Exception {
        String source = Files.readString(Path.of("src/main/java/dao/OrdersDAO.java"));
        int active = source.indexOf("public long countCurrentActiveOrders()");
        int activeByStatus = source.indexOf("public Map<String, Long> countCurrentActiveOrdersByStatus()");
        int revenue = source.indexOf("public BigDecimal sumDeliveredPaidRevenue(");
        int refunds = source.indexOf("public BigDecimal sumProcessedRefunds(");

        assertTrue(active >= 0 && activeByStatus > active && revenue > activeByStatus && refunds > revenue);
        String activeQuery = source.substring(active, activeByStatus);
        assertTrue(activeQuery.contains("NOT IN ('DELIVERED','CANCELLED','RETURNED_TO_STORE')"));
        assertTrue(!activeQuery.contains("createdAt"));
        String revenueQuery = source.substring(revenue, refunds);
        assertTrue(revenueQuery.contains("orderStatus = 'DELIVERED'"));
        assertTrue(revenueQuery.contains("paymentStatus = 'PAID'"));
        assertTrue(revenueQuery.contains("deliveredAt >= :start"));
        String refundQuery = source.substring(refunds);
        assertTrue(refundQuery.contains("refundStatus = 'REFUNDED'"));
        assertTrue(refundQuery.contains("refundedAt >= :start"));
    }
}

class AdminDashboardTestFixture {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    final List<Orders> orders = new ArrayList<>();
    final List<InventoryItem> inventoryItems = new ArrayList<>();
    String failedSection;
    boolean failPendingRefunds;

    AdminDashboardTestFixture() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        orders.add(order("PREPARING", "UNPAID", today.minusDays(1).atTime(20, 0), null, null, null, null));
        orders.add(order("DELIVERED", "PAID", today.atTime(8, 0), today.atTime(9, 0), new BigDecimal("100.00"), null, null));
        orders.add(order("DELIVERED", "UNPAID", today.atTime(8, 30), today.atTime(9, 30), new BigDecimal("900.00"), null, null));
        orders.add(order("DELIVERED", "PAID", today.minusDays(2).atTime(8, 0), today.minusDays(1).atTime(9, 0), new BigDecimal("50.00"), new BigDecimal("20.00"), today.atTime(10, 0)));
        inventoryItems.add(item("5.0000", "5.0000", "1.0000", true));
        inventoryItems.add(item("10.0000", "7.5000", "3.0000", true));
        inventoryItems.add(item("10.0000", "2.0000", "3.0000", true));
    }

    AdminService service() throws Exception {
        AdminService service = new AdminService();
        set(service, "userDAO", new StubUserDAO());
        set(service, "ordersDAO", new StubOrdersDAO());
        set(service, "productDAO", new StubProductDAO());
        set(service, "codSettlementDAO", new StubCodSettlementDAO());
        setIfPresent(service, "inventoryItemDAO", new StubInventoryItemDAO());
        set(service, "storeConfigService", new StubStoreConfigService());
        set(service, "menuPerformanceReportService", new StubMenuPerformanceReportService());
        set(service, "workShiftService", new StubWorkShiftService());
        return service;
    }

    private void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void setIfPresent(Object target, String name, Object value) throws Exception {
        try {
            set(target, name, value);
        } catch (NoSuchFieldException ignored) {
        }
    }

    private Orders order(String status, String payment, LocalDateTime createdAt, LocalDateTime deliveredAt, BigDecimal finalAmount, BigDecimal refundAmount, LocalDateTime refundedAt) {
        Orders order = new Orders();
        order.setOrderStatus(status);
        order.setPaymentStatus(payment);
        order.setCreatedAt(createdAt);
        order.setDeliveredAt(deliveredAt);
        order.setFinalAmount(finalAmount);
        order.setRefundAmount(refundAmount);
        order.setRefundedAt(refundedAt);
        if (refundedAt != null) order.setRefundStatus("REFUNDED");
        return order;
    }

    private InventoryItem item(String onHand, String reserved, String minimum, boolean active) {
        InventoryItem item = new InventoryItem();
        item.setOnHandQuantity(new BigDecimal(onHand));
        item.setReservedQuantity(new BigDecimal(reserved));
        item.setMinimumQuantity(new BigDecimal(minimum));
        item.setActive(active);
        return item;
    }

    private boolean inRange(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return value != null && !value.isBefore(start) && value.isBefore(end);
    }

    private boolean active(Orders order) {
        return !Set.of("DELIVERED", "CANCELLED", "RETURNED_TO_STORE").contains(order.getOrderStatus());
    }

    class StubOrdersDAO extends OrdersDAO {
        public long countCurrentActiveOrders() {
            if ("orders".equals(failedSection)) throw new IllegalStateException("orders unavailable");
            return orders.stream().filter(AdminDashboardTestFixture.this::active).count();
        }

        public Map<String, Long> countCurrentActiveOrdersByStatus() {
            Map<String, Long> counts = new HashMap<>();
            orders.stream().filter(AdminDashboardTestFixture.this::active).forEach(order -> counts.merge(order.getOrderStatus(), 1L, Long::sum));
            return counts;
        }

        public BigDecimal sumDeliveredPaidRevenue(LocalDateTime start, LocalDateTime end) {
            if ("financial".equals(failedSection)) throw new IllegalStateException("financial unavailable");
            return orders.stream()
                    .filter(order -> "DELIVERED".equals(order.getOrderStatus()) && "PAID".equals(order.getPaymentStatus()))
                    .filter(order -> inRange(order.getDeliveredAt(), start, end))
                    .map(Orders::getFinalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public BigDecimal sumProcessedRefunds(LocalDateTime start, LocalDateTime end) {
            return orders.stream().filter(order -> "REFUNDED".equals(order.getRefundStatus()))
                    .filter(order -> inRange(order.getRefundedAt(), start, end))
                    .map(Orders::getRefundAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        @Override public long count() { return orders.size(); }
        @Override public double sumRevenue() { return 150.0; }
        @Override public long countByStatus(String status) { return orders.stream().filter(order -> status.equals(order.getOrderStatus())).count(); }
        @Override public long[] operationalCohortSummary(LocalDateTime start, LocalDateTime end) {
            long all = orders.stream().filter(order -> inRange(order.getCreatedAt(), start, end)).count();
            long complete = orders.stream().filter(order -> inRange(order.getCreatedAt(), start, end) && "DELIVERED".equals(order.getOrderStatus())).count();
            return new long[] {all, complete};
        }
        @Override public double sumRevenueByDateRange(LocalDateTime start, LocalDateTime end) { return sumDeliveredPaidRevenue(start, end).doubleValue(); }
        @Override public BigDecimal sumRevenueDecimalByDateRange(LocalDateTime start, LocalDateTime end) { return sumDeliveredPaidRevenue(start, end); }
        @Override public List<Map<String, Object>> sumRevenueByMonth() { return List.of(); }
        @Override public List<Map<String, Object>> findTopProducts(int limit) { return List.of(); }
        @Override public long countByStatusAndDateRange(String status, LocalDateTime start, LocalDateTime end) { return orders.stream().filter(order -> status.equals(order.getOrderStatus()) && inRange(order.getDeliveredAt(), start, end)).count(); }
        @Override public long countActiveByDateRange(LocalDateTime start, LocalDateTime end) { return 0L; }
        @Override public long countAttentionOverdue(LocalDateTime now) { return 1L; }
        @Override public long countPendingRefunds() {
            if (failPendingRefunds || "refunds".equals(failedSection)) throw new IllegalStateException("refunds unavailable");
            return 1L;
        }
        @Override public double sumRefundsInRange(LocalDateTime start, LocalDateTime end) { return sumProcessedRefunds(start, end).doubleValue(); }
        @Override public BigDecimal sumRefundsDecimalInRange(LocalDateTime start, LocalDateTime end) { return sumProcessedRefunds(start, end); }
        @Override public long countRefundsInRange(LocalDateTime start, LocalDateTime end) { return sumProcessedRefunds(start, end).signum() == 0 ? 0L : 1L; }
        @Override public List<Map<String, Object>> findTopProductsByDateRange(LocalDateTime start, LocalDateTime end, int limit) { return List.of(); }
    }

    class StubInventoryItemDAO extends InventoryItemDAO {
        public Map<String, Long> inventoryRiskCounts() {
            if ("inventory".equals(failedSection)) throw new IllegalStateException("inventory unavailable");
            long out = inventoryItems.stream().filter(InventoryItem::isActive).filter(item -> item.availableQuantity().signum() <= 0).count();
            long low = inventoryItems.stream().filter(InventoryItem::isActive).filter(item -> item.availableQuantity().signum() > 0 && item.availableQuantity().compareTo(item.getMinimumQuantity()) <= 0).count();
            return Map.of("outOfStock", out, "lowStock", low, "lowStockItemCount", out + low);
        }
    }

    static class StubUserDAO extends UserDAO {
        @Override public long countByRole(String role) { return 3L; }
    }

    static class StubProductDAO extends ProductDAO {
        @Override public long countAvailableProducts() { return 7L; }
        @Override public long[] countStockRiskSkus(int threshold) { return new long[] {0L, 0L}; }
    }

    class StubCodSettlementDAO extends CodSettlementDAO {
        @Override public BigDecimal sumPendingAmount() { return new BigDecimal("500.00"); }
        @Override public long countPending() {
            if ("cod".equals(failedSection)) throw new IllegalStateException("cod unavailable");
            return 2L;
        }
    }

    static class StubStoreConfigService extends StoreConfigService {
        @Override public int getLowStockThreshold() { return 5; }
    }

    static class StubMenuPerformanceReportService extends MenuPerformanceReportService {
        @Override public Map<String, Object> report(LocalDate from, LocalDate to) { return Map.of("grossProfit", new BigDecimal("60.00"), "costComplete", true); }
    }

    class StubWorkShiftService extends WorkShiftService {
        @Override public long countCoverageGaps() {
            if ("staffing".equals(failedSection)) throw new IllegalStateException("staffing unavailable");
            return 1L;
        }
    }
}
