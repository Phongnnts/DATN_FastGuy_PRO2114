package service;

import java.util.HashMap;
import java.util.Map;

import dao.OrdersDAO;

public class StaffService {
    private OrdersDAO ordersDAO = new OrdersDAO();

    public Map<String, Object> getDashboard() {
        long pendingOrders = ordersDAO.countByStatus("PENDING");
        long confirmedOrders = ordersDAO.countByStatus("CONFIRMED");

        Map<String, Object> ordersByStatus = new HashMap<>();
        ordersByStatus.put("PENDING", ordersDAO.countByStatus("PENDING"));
        ordersByStatus.put("CONFIRMED", ordersDAO.countByStatus("CONFIRMED"));
        ordersByStatus.put("PREPARING", ordersDAO.countByStatus("PREPARING"));
        ordersByStatus.put("READY", ordersDAO.countByStatus("READY"));
        ordersByStatus.put("PICKED_UP", ordersDAO.countByStatus("PICKED_UP"));
        ordersByStatus.put("DELIVERED", ordersDAO.countByStatus("DELIVERED"));
        ordersByStatus.put("CANCELLED", ordersDAO.countByStatus("CANCELLED"));

        long ordersToday = ordersDAO.countToday();

        Map<String, Object> data = new HashMap<>();
        data.put("pendingOrders", pendingOrders);
        data.put("confirmedOrders", confirmedOrders);
        data.put("ordersByStatus", ordersByStatus);
        data.put("ordersToday", ordersToday);

        return data;
    }
}
