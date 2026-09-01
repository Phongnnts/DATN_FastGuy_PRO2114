package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import dao.CodSettlementDAO;
import dao.InventoryItemDAO;
import dao.OrdersDAO;
import dao.ProductDAO;
import dao.UserDAO;
import entity.InventoryItem;
import entity.Orders;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

class AdminOperationalReportingPolicyTest {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Test
    void dashboardUsesCarryOverOrdersAndEventTimeFinancialTruth() throws Exception {
        Map<String, Object> data = new AdminDashboardTestFixture().service().getDashboard();

        assertEquals(2L, data.get("activeOrderCount"));
        assertEquals(Map.of("PREPARING", 1L, "DELIVERY_FAILED", 1L), data.get("activeOrdersByStatus"));
        assertEquals(new BigDecimal("80.00"), data.get("netCashRevenueToday"));
        assertInstanceOf(BigDecimal.class, data.get("netCashRevenueToday"));
        assertEquals(1L, data.get("pendingRefundCount"));
        assertEquals(2L, data.get("operationalOrderCountToday"));
        assertEquals(2L, data.get("operationalCompletedCountToday"));
        assertEquals(100.0, data.get("completionRateToday"));
    }

    @Test
    void eachFailedCanonicalProviderMarksOnlyItsSectionUnavailable() throws Exception {
        for (String failed : List.of("financial", "orders", "refunds", "cod", "inventory", "staffing")) {
            AdminDashboardTestFixture fixture = new AdminDashboardTestFixture();
            fixture.failedProvider = failed;

            Map<String, Object> data = fixture.service().getDashboard();
            Map<String, String> availability = availability(data);

            for (String section : List.of("financial", "orders", "refunds", "cod", "inventory", "staffing")) {
                assertEquals(section.equals(failed) ? "UNAVAILABLE" : "AVAILABLE", availability.get(section), failed + ":" + section);
            }
            if (!"orders".equals(failed)) assertEquals(2L, data.get("activeOrderCount"));
            if (!"cod".equals(failed)) assertEquals(2L, data.get("pendingCodCount"));
            if (!"inventory".equals(failed)) assertEquals(2L, data.get("lowStockItemCount"));
            if (!"staffing".equals(failed)) assertEquals(1L, data.get("staffingGapCount"));
        }
    }

    @Test
    void userFailurePreservesOrderTruth() throws Exception {
        AdminDashboardTestFixture fixture = new AdminDashboardTestFixture();
        fixture.failedProvider = "user";

        Map<String, Object> data = fixture.service().getDashboard();

        assertEquals("AVAILABLE", availability(data).get("orders"));
        assertEquals(2L, data.get("activeOrderCount"));
        assertEquals(Map.of("PREPARING", 1L, "DELIVERY_FAILED", 1L), data.get("activeOrdersByStatus"));
        assertNull(data.get("customerCount"));
        assertNull(data.get("totalUsers"));
    }

    @Test
    void menuFailurePreservesRevenueAndNetCashTruth() throws Exception {
        AdminDashboardTestFixture fixture = new AdminDashboardTestFixture();
        fixture.failedProvider = "menu";

        Map<String, Object> data = fixture.service().getDashboard();

        assertEquals("AVAILABLE", availability(data).get("financial"));
        assertEquals(new BigDecimal("100.00"), data.get("revenueToday"));
        assertEquals(new BigDecimal("80.00"), data.get("netCashRevenueToday"));
        assertNull(data.get("grossProfitToday"));
        assertEquals(false, data.get("costComplete"));
    }

    @Test
    void legacyInventoryDependencyFailuresPreserveInventoryTruth() throws Exception {
        for (String failed : List.of("storeConfig", "product")) {
            AdminDashboardTestFixture fixture = new AdminDashboardTestFixture();
            fixture.failedProvider = failed;

            Map<String, Object> data = fixture.service().getDashboard();

            assertEquals("AVAILABLE", availability(data).get("inventory"), failed);
            assertEquals(2L, data.get("lowStockItemCount"), failed);
            assertEquals(1L, data.get("outOfStockSkuCount"), failed);
            assertEquals(1L, data.get("lowStockSkuCount"), failed);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> attentionItems = (List<Map<String, Object>>) data.get("attentionItems");
            assertTrue(attentionItems.stream().anyMatch(item -> "LOW_STOCK_ITEMS".equals(item.get("type")) && Long.valueOf(2L).equals(item.get("count"))), failed);
            if ("storeConfig".equals(failed)) assertNull(data.get("lowStockThreshold"));
            if ("product".equals(failed)) {
                assertNull(data.get("activeProductCount"));
                assertNull(data.get("totalProducts"));
            }
        }
    }

    @Test
    void failedPeriodFinancialReadOmitsDerivedNetRevenue() throws Exception {
        AdminDashboardTestFixture fixture = new AdminDashboardTestFixture();
        fixture.failedProvider = "periodFinancial";

        Map<String, Object> data = fixture.service().getDashboardWithPeriod("7d");

        assertFalse(data.containsKey("grossRevenue"));
        assertFalse(data.containsKey("periodRevenue"));
        assertFalse(data.containsKey("netRevenue"));
        assertEquals(new BigDecimal("20.00"), data.get("refundTotal"));
        assertEquals("UNAVAILABLE", availability(data).get("financial"));
    }

    @Test
    void failedPeriodRefundReadOmitsDerivedNetRevenue() throws Exception {
        AdminDashboardTestFixture fixture = new AdminDashboardTestFixture();
        fixture.failedProvider = "periodRefund";

        Map<String, Object> data = fixture.service().getDashboardWithPeriod("7d");

        assertEquals(new BigDecimal("100.00"), data.get("grossRevenue"));
        assertEquals(new BigDecimal("100.00"), data.get("periodRevenue"));
        assertFalse(data.containsKey("refundTotal"));
        assertFalse(data.containsKey("refundCount"));
        assertFalse(data.containsKey("netRevenue"));
        assertEquals("UNAVAILABLE", availability(data).get("refunds"));
    }

    @Test
    void dashboardKeepsCompatibilityMoneyAsBigDecimal() throws Exception {
        Map<String, Object> data = new AdminDashboardTestFixture().service().getDashboardWithPeriod("7d");

        for (String key : List.of("netCashRevenueToday", "totalRevenue", "pendingCodAmount", "revenueToday", "aovToday", "grossProfitToday", "grossRevenue", "periodRevenue", "refundTotal", "netRevenue")) {
            assertInstanceOf(BigDecimal.class, data.get(key), key);
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> monthly = (List<Map<String, Object>>) data.get("revenueByMonth");
        assertInstanceOf(BigDecimal.class, monthly.get(0).get("revenue"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) data.get("periodTopProducts");
        assertInstanceOf(BigDecimal.class, products.get(0).get("revenue"));
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
    void currentActiveQueriesHaveExactUnboundedPredicates() throws Exception {
        QueryCapture total = new QueryCapture(2L, List.of());
        assertEquals(2L, productionDao(total).countCurrentActiveOrders());
        assertEquals("SELECT COUNT(o) FROM Orders o WHERE o.orderStatus NOT IN ('DELIVERED','CANCELLED','RETURNED_TO_STORE')", total.jpql);
        assertEquals(Map.of(), total.parameters);

        QueryCapture grouped = new QueryCapture(null, List.<Object[]>of(
                new Object[] {"PREPARING", 1L},
                new Object[] {"DELIVERY_FAILED", 1L}));
        assertEquals(Map.of("PREPARING", 1L, "DELIVERY_FAILED", 1L), productionDao(grouped).countCurrentActiveOrdersByStatus());
        assertEquals("SELECT o.orderStatus, COUNT(o) FROM Orders o WHERE o.orderStatus NOT IN ('DELIVERED','CANCELLED','RETURNED_TO_STORE') GROUP BY o.orderStatus", grouped.jpql);
        assertEquals(Map.of(), grouped.parameters);
    }

    @Test
    void deliveredPaidRevenueQueryHasExactPredicatesAndBindings() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 8, 31, 0, 0);
        LocalDateTime end = start.plusDays(1);
        QueryCapture capture = new QueryCapture(new BigDecimal("100.00"), List.of());

        assertEquals(new BigDecimal("100.00"), productionDao(capture).sumDeliveredPaidRevenue(start, end));
        assertEquals("SELECT SUM(o.finalAmount) FROM Orders o WHERE o.orderStatus = 'DELIVERED' AND o.paymentStatus = 'PAID' AND o.deliveredAt >= :start AND o.deliveredAt < :end", capture.jpql);
        assertEquals(Map.of("start", start, "end", end), capture.parameters);
    }

    @Test
    void processedRefundQueryHasExactPredicatesAndBindings() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 8, 31, 0, 0);
        LocalDateTime end = start.plusDays(1);
        QueryCapture capture = new QueryCapture(new BigDecimal("20.00"), List.of());

        assertEquals(new BigDecimal("20.00"), productionDao(capture).sumProcessedRefunds(start, end));
        assertEquals("SELECT SUM(o.refundAmount) FROM Orders o WHERE o.refundStatus = 'REFUNDED' AND o.refundedAt >= :start AND o.refundedAt < :end", capture.jpql);
        assertEquals(Map.of("start", start, "end", end), capture.parameters);
    }

    @Test
    void productionDaoKeepsAggregateAndRowMoneyTyped() throws Exception {
        QueryCapture totals = new QueryCapture(new BigDecimal("150.00"), List.of());
        Method method = OrdersDAO.class.getMethod("sumRevenueDecimal");
        assertEquals(new BigDecimal("150.00"), method.invoke(productionDao(totals)));

        QueryCapture monthly = new QueryCapture(null, List.<Object[]>of(new Object[] {8, 2026, new BigDecimal("150.00")}));
        assertInstanceOf(BigDecimal.class, productionDao(monthly).sumRevenueByMonth().get(0).get("revenue"));

        QueryCapture products = new QueryCapture(null, List.<Object[]>of(new Object[] {1, "Burger", 2, new BigDecimal("80.00")}));
        assertInstanceOf(BigDecimal.class, productionDao(products).findTopProductsByDateRange(LocalDateTime.MIN, LocalDateTime.MAX, 5).get(0).get("revenue"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> availability(Map<String, Object> data) {
        return (Map<String, String>) data.get("sectionAvailability");
    }

    private OrdersDAO productionDao(QueryCapture capture) throws Exception {
        Constructor<OrdersDAO> constructor = OrdersDAO.class.getDeclaredConstructor(Supplier.class);
        constructor.setAccessible(true);
        Supplier<EntityManager> supplier = () -> entityManager(capture);
        return constructor.newInstance(supplier);
    }

    private EntityManager entityManager(QueryCapture capture) {
        return (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {EntityManager.class}, (proxy, method, args) -> switch (method.getName()) {
            case "createQuery", "createNativeQuery" -> {
                capture.jpql = normalize((String) args[0]);
                yield query(capture);
            }
            case "close" -> null;
            case "isOpen" -> true;
            default -> defaultValue(method.getReturnType());
        });
    }

    private Object query(QueryCapture capture) {
        Object[] query = new Object[1];
        query[0] = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {TypedQuery.class}, (proxy, method, args) -> switch (method.getName()) {
            case "setParameter" -> {
                capture.parameters.put(args[0], args[1]);
                yield query[0];
            }
            case "getSingleResult" -> capture.singleResult;
            case "getResultList" -> capture.rows;
            default -> defaultValue(method.getReturnType());
        });
        return query[0];
    }

    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }

    private static final class QueryCapture {
        private final Object singleResult;
        private final List<Object[]> rows;
        private final Map<Object, Object> parameters = new LinkedHashMap<>();
        private String jpql;

        private QueryCapture(Object singleResult, List<Object[]> rows) {
            this.singleResult = singleResult;
            this.rows = rows;
        }
    }
}

class AdminDashboardTestFixture {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    final List<Orders> orders = new ArrayList<>();
    final List<InventoryItem> inventoryItems = new ArrayList<>();
    String failedProvider;
    boolean failPendingRefunds;

    AdminDashboardTestFixture() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        orders.add(order("PREPARING", "UNPAID", today.minusDays(1).atTime(20, 0), null, null, null, null, null));
        orders.add(order("DELIVERY_FAILED", "UNPAID", today.minusDays(1).atTime(21, 0), null, null, null, null, null));
        orders.add(order("DELIVERED", "PAID", today.atTime(8, 0), today.atTime(9, 0), new BigDecimal("100.00"), null, null, null));
        orders.add(order("DELIVERED", "UNPAID", today.atTime(8, 30), today.atTime(9, 30), new BigDecimal("900.00"), null, null, null));
        orders.add(order("CANCELLED", "PAID", today.minusDays(2).atTime(8, 0), null, new BigDecimal("20.00"), "REFUNDED", new BigDecimal("20.00"), today.atTime(10, 0)));
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
        set(service, "inventoryItemDAO", new StubInventoryItemDAO());
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

    private Orders order(String status, String payment, LocalDateTime createdAt, LocalDateTime deliveredAt, BigDecimal finalAmount, String refundStatus, BigDecimal refundAmount, LocalDateTime refundedAt) {
        Orders order = new Orders();
        order.setOrderStatus(status);
        order.setPaymentStatus(payment);
        order.setCreatedAt(createdAt);
        order.setDeliveredAt(deliveredAt);
        order.setFinalAmount(finalAmount);
        order.setRefundStatus(refundStatus);
        order.setRefundAmount(refundAmount);
        order.setRefundedAt(refundedAt);
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
        @Override
        public long countCurrentActiveOrders() {
            if ("orders".equals(failedProvider)) throw new IllegalStateException("orders unavailable");
            return orders.stream().filter(AdminDashboardTestFixture.this::active).count();
        }

        @Override
        public Map<String, Long> countCurrentActiveOrdersByStatus() {
            Map<String, Long> counts = new HashMap<>();
            orders.stream().filter(AdminDashboardTestFixture.this::active).forEach(order -> counts.merge(order.getOrderStatus(), 1L, Long::sum));
            return counts;
        }

        @Override
        public BigDecimal sumDeliveredPaidRevenue(LocalDateTime start, LocalDateTime end) {
            LocalDateTime todayStart = LocalDate.now(BUSINESS_ZONE).atStartOfDay();
            if ("financial".equals(failedProvider) || "periodFinancial".equals(failedProvider) && start.isBefore(todayStart)) throw new IllegalStateException("financial unavailable");
            return orders.stream()
                    .filter(order -> "DELIVERED".equals(order.getOrderStatus()) && "PAID".equals(order.getPaymentStatus()))
                    .filter(order -> inRange(order.getDeliveredAt(), start, end))
                    .map(Orders::getFinalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        @Override
        public BigDecimal sumProcessedRefunds(LocalDateTime start, LocalDateTime end) {
            LocalDateTime todayStart = LocalDate.now(BUSINESS_ZONE).atStartOfDay();
            if ("periodRefund".equals(failedProvider) && start.isBefore(todayStart)) throw new IllegalStateException("refunds unavailable");
            return orders.stream().filter(order -> "REFUNDED".equals(order.getRefundStatus()))
                    .filter(order -> inRange(order.getRefundedAt(), start, end))
                    .map(Orders::getRefundAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        @Override public long count() { return orders.size(); }
        @Override public double sumRevenue() { return 150.0; }
        @Override public BigDecimal sumRevenueDecimal() { return new BigDecimal("150.00"); }
        @Override public long countByStatus(String status) { return orders.stream().filter(order -> status.equals(order.getOrderStatus())).count(); }
        @Override public long[] operationalCohortSummary(LocalDateTime start, LocalDateTime end) {
            long all = orders.stream().filter(order -> inRange(order.getCreatedAt(), start, end)).count();
            long complete = orders.stream().filter(order -> inRange(order.getCreatedAt(), start, end) && "DELIVERED".equals(order.getOrderStatus())).count();
            return new long[] {all, complete};
        }
        @Override public double sumRevenueByDateRange(LocalDateTime start, LocalDateTime end) { return sumDeliveredPaidRevenue(start, end).doubleValue(); }
        @Override public BigDecimal sumRevenueDecimalByDateRange(LocalDateTime start, LocalDateTime end) { return sumDeliveredPaidRevenue(start, end); }
        @Override public List<Map<String, Object>> sumRevenueByMonth() { return List.of(Map.of("month", 8, "year", 2026, "revenue", 150.0)); }
        @Override public List<Map<String, Object>> findTopProducts(int limit) { return List.of(); }
        @Override public long countByStatusAndDateRange(String status, LocalDateTime start, LocalDateTime end) { return orders.stream().filter(order -> status.equals(order.getOrderStatus()) && inRange(order.getDeliveredAt(), start, end)).count(); }
        @Override public long countActiveByDateRange(LocalDateTime start, LocalDateTime end) { return 0L; }
        @Override public long countAttentionOverdue(LocalDateTime now) { return 1L; }
        @Override public long countPendingRefunds() {
            if (failPendingRefunds || "refunds".equals(failedProvider)) throw new IllegalStateException("refunds unavailable");
            return 1L;
        }
        @Override public double sumRefundsInRange(LocalDateTime start, LocalDateTime end) { return sumProcessedRefunds(start, end).doubleValue(); }
        @Override public BigDecimal sumRefundsDecimalInRange(LocalDateTime start, LocalDateTime end) { return sumProcessedRefunds(start, end); }
        @Override public long countRefundsInRange(LocalDateTime start, LocalDateTime end) {
            if ("periodRefund".equals(failedProvider)) throw new IllegalStateException("refunds unavailable");
            return sumProcessedRefunds(start, end).signum() == 0 ? 0L : 1L;
        }
        @Override public List<Map<String, Object>> findTopProductsByDateRange(LocalDateTime start, LocalDateTime end, int limit) { return List.of(Map.of("productId", 1, "name", "Burger", "sold", 1, "revenue", 40.0)); }
    }

    class StubInventoryItemDAO extends InventoryItemDAO {
        @Override
        public Map<String, Long> inventoryRiskCounts() {
            if ("inventory".equals(failedProvider)) throw new IllegalStateException("inventory unavailable");
            long out = inventoryItems.stream().filter(InventoryItem::isActive).filter(item -> item.availableQuantity().signum() <= 0).count();
            long low = inventoryItems.stream().filter(InventoryItem::isActive).filter(item -> item.availableQuantity().signum() > 0 && item.availableQuantity().compareTo(item.getMinimumQuantity()) <= 0).count();
            return Map.of("outOfStock", out, "lowStock", low, "lowStockItemCount", out + low);
        }
    }

    class StubUserDAO extends UserDAO {
        @Override
        public long countByRole(String role) {
            if ("user".equals(failedProvider)) throw new IllegalStateException("user unavailable");
            return 3L;
        }
    }

    class StubProductDAO extends ProductDAO {
        @Override
        public long countAvailableProducts() {
            if ("product".equals(failedProvider)) throw new IllegalStateException("product unavailable");
            return 7L;
        }
    }

    class StubCodSettlementDAO extends CodSettlementDAO {
        @Override public BigDecimal sumPendingAmount() { return new BigDecimal("500.00"); }
        @Override public long countPending() {
            if ("cod".equals(failedProvider)) throw new IllegalStateException("cod unavailable");
            return 2L;
        }
    }

    class StubStoreConfigService extends StoreConfigService {
        @Override
        public int getLowStockThreshold() {
            if ("storeConfig".equals(failedProvider)) throw new IllegalStateException("config unavailable");
            return 5;
        }
    }

    class StubMenuPerformanceReportService extends MenuPerformanceReportService {
        @Override
        public Map<String, Object> report(LocalDate from, LocalDate to) {
            if ("menu".equals(failedProvider)) throw new IllegalStateException("menu unavailable");
            return Map.of("grossProfit", new BigDecimal("60.00"), "costComplete", true);
        }
    }

    class StubWorkShiftService extends WorkShiftService {
        @Override public long countCoverageGaps() {
            if ("staffing".equals(failedProvider)) throw new IllegalStateException("staffing unavailable");
            return 1L;
        }
    }
}
