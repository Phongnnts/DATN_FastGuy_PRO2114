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
        ordersByStatus.put("DELIVERING", ordersDAO.countByStatus("DELIVERING"));
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
}
