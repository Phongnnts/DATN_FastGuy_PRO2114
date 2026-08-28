package service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dao.CodSettlementDAO;
import dao.OrdersDAO;
import dao.ProductDAO;
import dao.UserDAO;

public class AdminService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<String> ORDER_STATUSES = List.of("PENDING", "CONFIRMED", "PREPARING", "READY", "ASSIGNED", "PICKED_UP", "DELIVERY_FAILED", "RETURNED_TO_STORE", "DELIVERED", "CANCELLED");

    private UserDAO userDAO = new UserDAO();
    private OrdersDAO ordersDAO = new OrdersDAO();
    private ProductDAO productDAO = new ProductDAO();
    private CodSettlementDAO codSettlementDAO = new CodSettlementDAO();
    private StoreConfigService storeConfigService = new StoreConfigService();
    private MenuPerformanceReportService menuPerformanceReportService = new MenuPerformanceReportService();

    public Map<String, Object> getDashboard() {
        return getDashboardWithPeriod(null);
    }

    public Map<String, Object> getDashboardWithPeriod(String period) {
        long customerCount = userDAO.countByRole("USER");
        long totalOrders = ordersDAO.count();
        long activeProductCount = productDAO.countAvailableProducts();
        double totalRevenue = ordersDAO.sumRevenue();

        Map<String, Object> ordersByStatus = new HashMap<>();
        for (String status : ORDER_STATUSES) ordersByStatus.put(status, ordersDAO.countByStatus(status));
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDateTime operationalStart = today.atStartOfDay();
        LocalDateTime operationalEnd = today.plusDays(1).atStartOfDay();
        long[] operational = ordersDAO.operationalCohortSummary(operationalStart, operationalEnd);

        Map<String, Object> data = new HashMap<>();
        data.put("customerCount", customerCount);
        data.put("totalUsers", customerCount);
        data.put("totalOrders", totalOrders);
        data.put("activeProductCount", activeProductCount);
        data.put("totalProducts", activeProductCount);
        data.put("operationalOrderCount", operational[0]);
        data.put("operationalCompletedCount", operational[1]);
        data.put("completionRate", operational[0] == 0 ? 0.0 : operational[1] * 100.0 / operational[0]);
        data.put("totalRevenue", totalRevenue);
        data.put("ordersByStatus", ordersByStatus);
        data.put("pendingOrders", ordersDAO.countByStatus("PENDING"));
        data.put("pendingCodAmount", codSettlementDAO.sumPendingAmount());
        data.put("pendingCodCount", codSettlementDAO.countPending());
        data.put("ordersToday", operational[0]);
        data.put("revenueToday", ordersDAO.sumRevenueByDateRange(operationalStart, operationalEnd));
        data.put("revenueByMonth", ordersDAO.sumRevenueByMonth());
        data.put("topProducts", ordersDAO.findTopProducts(5));

        if (period != null) {
            LocalDate now = LocalDate.now(BUSINESS_ZONE);
            LocalDateTime start, end = now.plusDays(1).atStartOfDay();
            switch (period) {
                case "7d": start = now.minusDays(6).atStartOfDay(); break;
                case "30d": start = now.minusDays(29).atStartOfDay(); break;
                case "1y": start = now.minusYears(1).atStartOfDay(); break;
                default: start = now.minusMonths(6).atStartOfDay();
            }
            double periodRevenue = ordersDAO.sumRevenueByDateRange(start, end);
            long periodOrders = ordersDAO.countByStatusAndDateRange("DELIVERED", start, end);
            var periodTopProducts = ordersDAO.findTopProductsByDateRange(start, end, 5);
            double refundTotal = ordersDAO.sumRefundsInRange(start, end);
            data.put("grossRevenue", periodRevenue);
            data.put("periodRevenue", periodRevenue);
            data.put("refundTotal", refundTotal);
            data.put("refundCount", ordersDAO.countRefundsInRange(start, end));
            data.put("netRevenue", periodRevenue - refundTotal);
            data.put("periodOrders", periodOrders);
            data.put("periodTopProducts", periodTopProducts);
        }

        int lowStockThreshold = storeConfigService.getLowStockThreshold();
        long[] stockRiskCounts = productDAO.countStockRiskSkus(lowStockThreshold);
        data.put("lowStockThreshold", lowStockThreshold);
        data.put("outOfStockSkuCount", stockRiskCounts[0]);
        data.put("lowStockSkuCount", stockRiskCounts[1]);
        return data;
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
