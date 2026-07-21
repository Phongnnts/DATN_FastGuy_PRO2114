package service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import dao.OrdersDAO;
import dao.ProductDAO;
import dao.UserDAO;

public class AdminService {
    private UserDAO userDAO = new UserDAO();
    private OrdersDAO ordersDAO = new OrdersDAO();
    private ProductDAO productDAO = new ProductDAO();

    public Map<String, Object> getDashboard() {
        return getDashboardWithPeriod(null);
    }

    public Map<String, Object> getDashboardWithPeriod(String period) {
        long totalUsers = userDAO.count();
        long totalOrders = ordersDAO.count();
        long totalProducts = productDAO.count();
        double totalRevenue = ordersDAO.sumRevenue();

        Map<String, Object> ordersByStatus = new HashMap<>();
        ordersByStatus.put("PENDING", ordersDAO.countByStatus("PENDING"));
        ordersByStatus.put("CONFIRMED", ordersDAO.countByStatus("CONFIRMED"));
        ordersByStatus.put("PREPARING", ordersDAO.countByStatus("PREPARING"));
        ordersByStatus.put("READY", ordersDAO.countByStatus("READY"));
        ordersByStatus.put("PICKED_UP", ordersDAO.countByStatus("PICKED_UP"));
        ordersByStatus.put("DELIVERED", ordersDAO.countByStatus("DELIVERED"));
        ordersByStatus.put("CANCELLED", ordersDAO.countByStatus("CANCELLED"));

        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", totalUsers);
        data.put("totalOrders", totalOrders);
        data.put("totalProducts", totalProducts);
        data.put("totalRevenue", totalRevenue);
        data.put("ordersByStatus", ordersByStatus);
        data.put("pendingOrders", ordersDAO.countByStatus("PENDING"));
        data.put("ordersToday", ordersDAO.countToday());
        data.put("revenueToday", ordersDAO.sumRevenueToday());
        data.put("revenueByMonth", ordersDAO.sumRevenueByMonth());
        data.put("topProducts", ordersDAO.findTopProducts(5));

        if (period != null) {
            LocalDate now = LocalDate.now();
            LocalDateTime start, end = now.plusDays(1).atStartOfDay();
            switch (period) {
                case "7d": start = now.minusDays(7).atStartOfDay(); break;
                case "30d": start = now.minusDays(30).atStartOfDay(); break;
                case "1y": start = now.minusYears(1).atStartOfDay(); break;
                default: start = now.minusMonths(6).atStartOfDay();
            }
            double periodRevenue = ordersDAO.sumRevenueByDateRange(start, end);
            long periodOrders = ordersDAO.countByStatusAndDateRange("DELIVERED", start, end);
            var periodTopProducts = ordersDAO.findTopProductsByDateRange(start, end, 5);
            data.put("periodRevenue", periodRevenue);
            data.put("periodOrders", periodOrders);
            data.put("periodTopProducts", periodTopProducts);
        }

        return data;
    }

    public Map<String, Object> getFullReport(String period, String startDate, String endDate) {
        boolean hasStart = startDate != null && !startDate.isBlank();
        boolean hasEnd = endDate != null && !endDate.isBlank();
        if (hasStart != hasEnd) throw new IllegalArgumentException("Vui lòng chọn đủ từ ngày và đến ngày");

        LocalDate today = LocalDate.now();
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
        data.put("periodRevenue", ordersDAO.sumRevenueByDateRange(start, end));
        data.put("periodOrders", ordersDAO.countByStatusAndDateRange("DELIVERED", start, end));
        data.put("totalOrdersInPeriod", ordersDAO.countAllByDateRange(start, end));
        data.put("avgOrderValue", ordersDAO.avgOrderValue(start, end));
        data.put("topProducts", ordersDAO.findTopProductsByDateRange(start, end, 10));
        data.put("ordersByStatus", ordersDAO.ordersByStatusInPeriod(start, end));
        data.put("revenueByCategory", ordersDAO.revenueByCategory(start, end));
        data.put("paymentMethodStats", ordersDAO.paymentMethodStats(start, end));
        data.put("revenueToday", ordersDAO.sumRevenueToday());
        data.put("ordersToday", ordersDAO.countToday());
        return data;
    }
}
