package service;

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

        return data;
    }
}
