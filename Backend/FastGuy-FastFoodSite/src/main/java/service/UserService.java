package service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dao.CartDAO;
import dao.OrdersDAO;
import dao.UserDAO;
import entity.Orders;
import entity.User;

public class UserService {
    private UserDAO userDAO = new UserDAO();
    private OrdersDAO ordersDAO = new OrdersDAO();
    private CartDAO cartDAO = new CartDAO();

    public Map<String, Object> getDashboard(int userId) {
        User user = userDAO.findById(userId);
        if (user == null) return null;

        List<Orders> recentOrders = ordersDAO.findByUserId(userId)
                .stream()
                .limit(5)
                .collect(Collectors.toList());

        int cartItemCount = cartDAO.countItemsByUserId(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("user", Map.of(
                "userId", user.getUserId(),
                "fullName", user.getFullName(),
                "phone", user.getPhone(),
                "email", user.getEmail()
        ));
        data.put("recentOrders", recentOrders.stream().map(o -> Map.of(
                "orderId", o.getOrderId(),
                "status", o.getOrderStatus(),
                "finalAmount", o.getFinalAmount(),
                "createdAt", o.getCreatedAt().toString()
        )).collect(Collectors.toList()));
        data.put("cartItemCount", cartItemCount);

        return data;
    }
}
