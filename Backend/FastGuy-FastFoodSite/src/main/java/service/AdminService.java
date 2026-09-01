package service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dao.CodSettlementDAO;
import dao.InventoryItemDAO;
import dao.OrdersDAO;
import dao.ProductDAO;
import dao.UserDAO;
import entity.Product;
import entity.ProductVariant;

public class AdminService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<String> ORDER_STATUSES = List.of("PENDING", "CONFIRMED", "PREPARING", "READY", "ASSIGNED", "PICKED_UP", "DELIVERY_FAILED", "RETURNED_TO_STORE", "DELIVERED", "CANCELLED");

    private UserDAO userDAO = new UserDAO();
    private OrdersDAO ordersDAO = new OrdersDAO();
    private ProductDAO productDAO = new ProductDAO();
    private CodSettlementDAO codSettlementDAO = new CodSettlementDAO();
    private InventoryItemDAO inventoryItemDAO = new InventoryItemDAO();
    private StoreConfigService storeConfigService = new StoreConfigService();
    private MenuPerformanceReportService menuPerformanceReportService = new MenuPerformanceReportService();
    private WorkShiftService workShiftService = new WorkShiftService();
    private InventoryAvailabilityService inventoryAvailabilityService = new InventoryAvailabilityService();

    public Map<String, Object> getDashboard() {
        return getDashboardWithPeriod(null);
    }

    public Map<String, Object> getDashboardWithPeriod(String period) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDateTime operationalStart = today.atStartOfDay();
        LocalDateTime operationalEnd = today.plusDays(1).atStartOfDay();
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, String> sectionAvailability = new LinkedHashMap<>();
        for (String section : List.of("financial", "orders", "refunds", "cod", "inventory", "staffing")) sectionAvailability.put(section, "AVAILABLE");

        data.put("customerCount", null);
        data.put("totalUsers", null);
        data.put("totalOrders", 0L);
        data.put("ordersByStatus", Map.of());
        data.put("pendingOrders", 0L);
        data.put("ordersToday", 0L);
        data.put("topProducts", List.of());
        data.put("operationalOrderCount", 0L);
        data.put("operationalCompletedCount", 0L);
        data.put("completionRate", 0.0);
        data.put("activeOrderCount", 0L);
        data.put("activeOrdersByStatus", Map.of());
        data.put("operationalOrderCountToday", 0L);
        data.put("operationalCompletedCountToday", 0L);
        data.put("completionRateToday", 0.0);
        data.put("deliveredOrdersToday", 0L);
        data.put("activeOrdersToday", 0L);
        data.put("totalRevenue", BigDecimal.ZERO);
        data.put("revenueToday", BigDecimal.ZERO);
        data.put("revenueByMonth", List.of());
        data.put("grossProfitToday", null);
        data.put("costComplete", false);
        data.put("pendingRefundCount", 0L);
        data.put("pendingCodAmount", BigDecimal.ZERO);
        data.put("pendingCodCount", 0L);
        data.put("netCashRevenueToday", BigDecimal.ZERO);
        data.put("aovToday", BigDecimal.ZERO);
        data.put("activeProductCount", null);
        data.put("totalProducts", null);
        data.put("lowStockThreshold", null);
        data.put("outOfStockSkuCount", 0L);
        data.put("lowStockSkuCount", 0L);
        data.put("lowStockItemCount", 0L);
        data.put("staffingGapCount", 0L);
        data.put("revenueLast7Days", List.of());
        data.put("topProductsLast7Days", List.of());
        data.put("lowStockProducts", List.of());

        try {
            long customerCount = userDAO.countByRole("USER");
            data.put("customerCount", customerCount);
            data.put("totalUsers", customerCount);
        } catch (RuntimeException exception) {
        }

        long deliveredToday = 0L;
        long overduePendingOrders = 0L;
        long deliveryFailedOrders = 0L;
        try {
            Map<String, Object> ordersByStatus = new LinkedHashMap<>();
            for (String status : ORDER_STATUSES) ordersByStatus.put(status, ordersDAO.countByStatus(status));
            long[] operational = ordersDAO.operationalCohortSummary(operationalStart, operationalEnd);
            Map<String, Long> activeOrdersByStatus = ordersDAO.countCurrentActiveOrdersByStatus();
            long activeOrderCount = ordersDAO.countCurrentActiveOrders();
            deliveredToday = ordersDAO.countByStatusAndDateRange("DELIVERED", operationalStart, operationalEnd);
            overduePendingOrders = ordersDAO.countAttentionOverdue(LocalDateTime.now(BUSINESS_ZONE));
            deliveryFailedOrders = ((Number) ordersByStatus.get("DELIVERY_FAILED")).longValue();
            double completionRate = operational[0] == 0 ? 0.0 : operational[1] * 100.0 / operational[0];
            data.put("totalOrders", ordersDAO.count());
            data.put("ordersByStatus", ordersByStatus);
            data.put("pendingOrders", ordersByStatus.get("PENDING"));
            data.put("ordersToday", operational[0]);
            data.put("topProducts", ordersDAO.findTopProducts(5));
            data.put("operationalOrderCount", operational[0]);
            data.put("operationalCompletedCount", operational[1]);
            data.put("completionRate", completionRate);
            data.put("activeOrderCount", activeOrderCount);
            data.put("activeOrdersByStatus", activeOrdersByStatus);
            data.put("operationalOrderCountToday", operational[0]);
            data.put("operationalCompletedCountToday", operational[1]);
            data.put("completionRateToday", completionRate);
            data.put("deliveredOrdersToday", deliveredToday);
            data.put("activeOrdersToday", activeOrderCount);
        } catch (RuntimeException exception) {
            sectionAvailability.put("orders", "UNAVAILABLE");
        }

        BigDecimal revenueToday = BigDecimal.ZERO;
        boolean revenueAvailable = false;
        try {
            revenueToday = ordersDAO.sumDeliveredPaidRevenue(operationalStart, operationalEnd);
            revenueAvailable = true;
            data.put("revenueToday", revenueToday);
        } catch (RuntimeException exception) {
            sectionAvailability.put("financial", "UNAVAILABLE");
        }
        try {
            data.put("totalRevenue", ordersDAO.sumRevenueDecimal());
        } catch (RuntimeException exception) {
            sectionAvailability.put("financial", "UNAVAILABLE");
        }
        try {
            data.put("revenueByMonth", moneyRows(ordersDAO.sumRevenueByMonth()));
        } catch (RuntimeException exception) {
            sectionAvailability.put("financial", "UNAVAILABLE");
        }
        try {
            Map<String, Object> menuToday = menuPerformanceReportService.report(today, today);
            boolean costComplete = Boolean.TRUE.equals(menuToday.get("costComplete"));
            data.put("grossProfitToday", costComplete ? decimal(menuToday.get("grossProfit")) : null);
            data.put("costComplete", costComplete);
        } catch (RuntimeException exception) {
        }

        long pendingRefundCount = 0L;
        try {
            pendingRefundCount = ordersDAO.countPendingRefunds();
            data.put("pendingRefundCount", pendingRefundCount);
        } catch (RuntimeException exception) {
            sectionAvailability.put("refunds", "UNAVAILABLE");
        }
        BigDecimal refundsToday = BigDecimal.ZERO;
        boolean refundsAvailable = false;
        try {
            refundsToday = ordersDAO.sumProcessedRefunds(operationalStart, operationalEnd);
            refundsAvailable = true;
        } catch (RuntimeException exception) {
            sectionAvailability.put("refunds", "UNAVAILABLE");
        }

        long pendingCodCount = 0L;
        try {
            data.put("pendingCodAmount", codSettlementDAO.sumPendingAmount());
            data.put("pendingCodCount", codSettlementDAO.countPending());
            pendingCodCount = ((Number) data.get("pendingCodCount")).longValue();
        } catch (RuntimeException exception) {
            sectionAvailability.put("cod", "UNAVAILABLE");
        }

        if (revenueAvailable && refundsAvailable) data.put("netCashRevenueToday", revenueToday.subtract(refundsToday));
        if (revenueAvailable && deliveredToday > 0) data.put("aovToday", revenueToday.divide(BigDecimal.valueOf(deliveredToday), 2, RoundingMode.HALF_UP));

        LocalDate weekStart = today.minusDays(6);
        try {
            List<Map<String, Object>> revenueLast7Days = new ArrayList<>();
            for (int day = 0; day < 7; day++) {
                LocalDate date = weekStart.plusDays(day);
                revenueLast7Days.add(Map.of("date", date.toString(), "revenue", ordersDAO.sumDeliveredPaidRevenue(date.atStartOfDay(), date.plusDays(1).atStartOfDay())));
            }
            data.put("revenueLast7Days", revenueLast7Days);
        } catch (RuntimeException exception) {
            sectionAvailability.put("financial", "UNAVAILABLE");
        }
        try {
            data.put("topProductsLast7Days", moneyRows(ordersDAO.findTopProductsByDateRange(weekStart.atStartOfDay(), today.plusDays(1).atStartOfDay(), 5)));
        } catch (RuntimeException exception) {
            sectionAvailability.put("orders", "UNAVAILABLE");
        }

        if (period != null) {
            LocalDate now = LocalDate.now(BUSINESS_ZONE);
            LocalDateTime start, end = now.plusDays(1).atStartOfDay();
            switch (period) {
                case "7d": start = now.minusDays(6).atStartOfDay(); break;
                case "30d": start = now.minusDays(29).atStartOfDay(); break;
                case "1y": start = now.minusYears(1).atStartOfDay(); break;
                default: start = now.minusMonths(6).atStartOfDay();
            }
            BigDecimal periodRevenue = BigDecimal.ZERO;
            boolean periodRevenueAvailable = false;
            try {
                periodRevenue = ordersDAO.sumDeliveredPaidRevenue(start, end);
                periodRevenueAvailable = true;
                data.put("grossRevenue", periodRevenue);
                data.put("periodRevenue", periodRevenue);
            } catch (RuntimeException exception) {
                sectionAvailability.put("financial", "UNAVAILABLE");
            }
            BigDecimal refundTotal = BigDecimal.ZERO;
            boolean periodRefundsAvailable = false;
            try {
                refundTotal = ordersDAO.sumProcessedRefunds(start, end);
                long refundCount = ordersDAO.countRefundsInRange(start, end);
                periodRefundsAvailable = true;
                data.put("refundTotal", refundTotal);
                data.put("refundCount", refundCount);
            } catch (RuntimeException exception) {
                sectionAvailability.put("refunds", "UNAVAILABLE");
            }
            if (periodRevenueAvailable && periodRefundsAvailable) data.put("netRevenue", periodRevenue.subtract(refundTotal));
            try {
                data.put("periodOrders", ordersDAO.countByStatusAndDateRange("DELIVERED", start, end));
                data.put("periodTopProducts", moneyRows(ordersDAO.findTopProductsByDateRange(start, end, 5)));
            } catch (RuntimeException exception) {
                sectionAvailability.put("orders", "UNAVAILABLE");
                data.put("periodOrders", 0L);
                data.put("periodTopProducts", List.of());
            }
        }

        long lowStockItemCount = 0L;
        boolean inventoryAvailable = false;
        try {
            Map<String, Long> inventoryRiskCounts = inventoryItemDAO.inventoryRiskCounts();
            lowStockItemCount = inventoryRiskCounts.get("lowStockItemCount");
            inventoryAvailable = true;
            data.put("outOfStockSkuCount", inventoryRiskCounts.get("outOfStock"));
            data.put("lowStockSkuCount", inventoryRiskCounts.get("lowStock"));
            data.put("lowStockItemCount", lowStockItemCount);
        } catch (RuntimeException exception) {
            sectionAvailability.put("inventory", "UNAVAILABLE");
        }
        try {
            data.put("lowStockThreshold", storeConfigService.getLowStockThreshold());
        } catch (RuntimeException exception) {
        }
        try {
            long activeProductCount = productDAO.countAvailableProducts();
            data.put("activeProductCount", activeProductCount);
            data.put("totalProducts", activeProductCount);
        } catch (RuntimeException exception) {
        }
        try {
            data.put("lowStockProducts", lowestProductCapacities());
        } catch (RuntimeException exception) {
            sectionAvailability.put("inventory", "UNAVAILABLE");
        }

        long staffingGapCount = 0L;
        try {
            staffingGapCount = workShiftService.countCoverageGaps();
            data.put("staffingGapCount", staffingGapCount);
        } catch (RuntimeException exception) {
            sectionAvailability.put("staffing", "UNAVAILABLE");
        }

        List<Map<String, Object>> attentionItems = new ArrayList<>();
        if ("AVAILABLE".equals(sectionAvailability.get("orders"))) {
            addAttention(attentionItems, "OVERDUE_PENDING_ORDERS", "WARNING", overduePendingOrders);
            addAttention(attentionItems, "DELIVERY_FAILED_ORDERS", "CRITICAL", deliveryFailedOrders);
        }
        if ("AVAILABLE".equals(sectionAvailability.get("refunds"))) addAttention(attentionItems, "PENDING_REFUNDS", "WARNING", pendingRefundCount);
        if ("AVAILABLE".equals(sectionAvailability.get("staffing"))) addAttention(attentionItems, "STAFF_COVERAGE_GAPS", "CRITICAL", staffingGapCount);
        if (inventoryAvailable) addAttention(attentionItems, "LOW_STOCK_ITEMS", "WARNING", lowStockItemCount);
        if ("AVAILABLE".equals(sectionAvailability.get("cod"))) addAttention(attentionItems, "PENDING_COD_SETTLEMENTS", "WARNING", pendingCodCount);
        data.put("attentionItems", attentionItems);
        data.put("sectionAvailability", sectionAvailability);
        return data;
    }

    private List<Map<String, Object>> lowestProductCapacities() {
        List<Product> products = productDAO.findAll().stream().filter(product -> "AVAILABLE".equals(product.getStatus())).toList();
        Map<Integer, Product> productByVariant = new LinkedHashMap<>();
        List<Integer> variantIds = new ArrayList<>();
        for (Product product : products) {
            for (ProductVariant variant : productDAO.findVariantsByProductId(product.getProductId())) {
                if ("AVAILABLE".equals(variant.getStatus()) && "INGREDIENT".equals(variant.getInventoryMode())) {
                    variantIds.add(variant.getVariantId());
                    productByVariant.put(variant.getVariantId(), product);
                }
            }
        }
        Map<Integer, Map<String, Object>> availability = inventoryAvailabilityService.publicAvailability(variantIds);
        Map<Integer, Map<String, Object>> byProduct = new LinkedHashMap<>();
        for (int variantId : variantIds) {
            Object remaining = availability.getOrDefault(variantId, Map.of()).get("remainingServings");
            if (!(remaining instanceof Number number)) continue;
            Product product = productByVariant.get(variantId);
            int servings = number.intValue();
            Map<String, Object> current = byProduct.get(product.getProductId());
            if (current == null || servings < ((Number) current.get("remainingServings")).intValue()) {
                byProduct.put(product.getProductId(), Map.of("productId", product.getProductId(), "name", product.getName(), "remainingServings", servings));
            }
        }
        return byProduct.values().stream()
                .sorted((left, right) -> Integer.compare(((Number) left.get("remainingServings")).intValue(), ((Number) right.get("remainingServings")).intValue()))
                .limit(5).toList();
    }

    private static List<Map<String, Object>> moneyRows(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>(row);
            if (item.containsKey("revenue")) item.put("revenue", decimal(item.get("revenue")));
            result.add(item);
        }
        return result;
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal amount) return amount;
        return new BigDecimal(value.toString());
    }

    private static void addAttention(List<Map<String, Object>> items, String type, String severity, long count) {
        if (count > 0) items.add(Map.of("type", type, "severity", severity, "count", count));
    }

    public Map<String, Object> getFullReport(String period, String startDate, String endDate) {
        boolean hasStart = startDate != null && !startDate.isBlank();
        boolean hasEnd = endDate != null && !endDate.isBlank();
        if (hasStart != hasEnd) throw new IllegalArgumentException("Vui lòng chọn đủ từ ngày và đến ngày");

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDateTime start;
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        if (hasStart) {
            LocalDate from = LocalDate.parse(startDate);
            LocalDate to = LocalDate.parse(endDate);
            if (from.isAfter(to)) throw new IllegalArgumentException("Từ ngày không được sau đến ngày");
            if (to.isAfter(today)) throw new IllegalArgumentException("Đến ngày không được sau hôm nay");
            start = from.atStartOfDay();
            end = to.plusDays(1).atStartOfDay();
        } else {
            String selectedPeriod = period == null || period.isBlank() ? "6m" : period;
            switch (selectedPeriod) {
                case "7d": start = today.minusDays(6).atStartOfDay(); break;
                case "30d": start = today.minusDays(29).atStartOfDay(); break;
                case "6m": start = today.minusMonths(6).atStartOfDay(); break;
                case "1y": start = today.minusYears(1).atStartOfDay(); break;
                default: throw new IllegalArgumentException("Kỳ báo cáo không hợp lệ");
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("revenueByMonth", ordersDAO.sumRevenueByCustomRange(start, end));
        data.put("revenueByDay", ordersDAO.revenueByDay(start, end));
        Map<String, Double> financial = ordersDAO.financialBreakdown(start, end);
        double revenueBeforeDiscount = financial.get("itemRevenue") + financial.get("shippingRevenue") + financial.get("serviceFeeRevenue");
        double revenueAfterDiscount = revenueBeforeDiscount - financial.get("discountTotal");
        double refundTotal = ordersDAO.sumRefundsInRange(start, end);
        double netRevenue = revenueAfterDiscount - refundTotal;
        Map<String, Object> menu = menuPerformanceReportService.report(start.toLocalDate(), end.minusDays(1).toLocalDate());
        Number cogsValue = (Number) menu.get("cost");
        Double cogs = Boolean.TRUE.equals(menu.get("costComplete")) && cogsValue != null ? cogsValue.doubleValue() : null;
        Double grossProfit = cogs == null ? null : netRevenue - cogs;
        data.put("itemRevenue", financial.get("itemRevenue"));
        data.put("shippingRevenue", financial.get("shippingRevenue"));
        data.put("serviceFeeRevenue", financial.get("serviceFeeRevenue"));
        data.put("discountTotal", financial.get("discountTotal"));
        data.put("revenueBeforeDiscount", revenueBeforeDiscount);
        data.put("revenueAfterDiscount", revenueAfterDiscount);
        data.put("grossRevenue", revenueAfterDiscount);
        data.put("periodRevenue", revenueAfterDiscount);
        data.put("refundTotal", refundTotal);
        data.put("refundCount", ordersDAO.countRefundsInRange(start, end));
        data.put("netCashRevenue", netRevenue);
        data.put("netRevenue", netRevenue);
        data.put("cogs", cogs);
        data.put("grossProfit", grossProfit);
        data.put("foodCostPercent", cogs == null || netRevenue == 0 ? null : cogs * 100 / netRevenue);
        data.put("grossMarginPercent", grossProfit == null || netRevenue == 0 ? null : grossProfit * 100 / netRevenue);
        long deliveredOrders = ordersDAO.countByStatusAndDateRange("DELIVERED", start, end);
        data.put("aov", deliveredOrders == 0 ? 0.0 : netRevenue / deliveredOrders);
        data.put("periodOrders", deliveredOrders);
        long[] operational = ordersDAO.operationalCohortSummary(start, end);
        data.put("operationalOrderCount", operational[0]);
        data.put("operationalCompletedCount", operational[1]);
        data.put("completionRate", operational[0] == 0 ? 0.0 : operational[1] * 100.0 / operational[0]);
        data.put("totalOrdersInPeriod", operational[0]);
        data.put("avgOrderValue", ordersDAO.avgOrderValue(start, end));
        data.put("topProducts", ordersDAO.findTopProductsByDateRange(start, end, 10));
        data.put("ordersByStatus", ordersDAO.ordersByStatusInPeriod(start, end));
        data.put("revenueByCategory", ordersDAO.revenueByCategory(start, end));
        data.put("paymentMethodStats", ordersDAO.paymentMethodStats(start, end));
        data.put("monthlyFinancialTrend", ordersDAO.monthlyFinancialTrend(start, end));
        data.put("revenueByHour", ordersDAO.revenueByHour(start, end));
        data.put("performanceByWeekday", ordersDAO.performanceByWeekday(start, end));
        data.put("refundTrend", ordersDAO.refundTrend(start, end));
        data.put("exceptionReasons", ordersDAO.exceptionReasons(start, end));
        data.put("revenueToday", ordersDAO.sumRevenueToday());
        data.put("ordersToday", ordersDAO.countToday());
        return data;
    }
}
