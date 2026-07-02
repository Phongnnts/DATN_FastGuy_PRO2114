package service;

import dao.OrderItemDAO;
import dao.OrdersDAO;
import dao.UserDAO;
import entity.Orders;
import entity.User;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaffOrderService {
    private OrdersDAO ordersDAO = new OrdersDAO();
    private OrderItemDAO orderItemDAO = new OrderItemDAO();
    private UserDAO userDAO = new UserDAO();
    private ShippingService shippingService = new ShippingService();

    public List<Orders> getPendingOrders() {
        return ordersDAO.findByStatus("PENDING");
    }

    public List<Orders> getConfirmedOrders() {
        return ordersDAO.findByStatus("CONFIRMED");
    }

    public List<Orders> getPreparingOrders() {
        return ordersDAO.findByStatus("PREPARING");
    }

    public List<Orders> getReadyOrders() {
        return ordersDAO.findByStatus("READY");
    }

    public Orders getOrderDetail(int orderId) {
        return ordersDAO.findById(orderId);
    }

    public List<User> getAvailableShippers() {
        return userDAO.findByRoleName("SHIPPER");
    }

    public boolean assignShipper(int orderId, int shipperId) {
        Orders order = ordersDAO.findById(orderId);
        if (order == null || !"READY".equals(order.getOrderStatus())) return false;
        if (order.getShipper() != null) return false;
        User shipper = userDAO.findById(shipperId);
        if (shipper == null) return false;
        if (shipper.getRole() == null || !"SHIPPER".equals(shipper.getRole().getRoleName())) return false;
        order.setShipper(shipper);
        order.setAssignedAt(LocalDateTime.now());
        ordersDAO.save(order);
        return true;
    }

    public boolean updateStatus(int orderId, String status, int staffId, String failureReason) {
        Orders order = ordersDAO.findById(orderId);
        if (order == null) return false;

        String current = order.getOrderStatus();

        switch (status) {
            case "CONFIRMED":
                if (!"PENDING".equals(current)) return false;
                if ("BANK_TRANSFER".equals(order.getPaymentMethod()) && !"PAID".equals(order.getPaymentStatus())) {
                    return false;
                }
                order.setConfirmedAt(LocalDateTime.now());
                break;
            case "PREPARING":
                if (!"CONFIRMED".equals(current)) return false;
                break;
            case "READY":
                if (!"PREPARING".equals(current)) return false;
                order.setReadyAt(LocalDateTime.now());
                break;
            case "CANCELLED":
                if ("READY".equals(current)) return false;
                order.setCancelledAt(LocalDateTime.now());
                if (failureReason != null && !failureReason.isEmpty()) {
                    order.setFailureReason(failureReason);
                }
                break;
            default:
                return false;
        }

        order.setOrderStatus(status);
        User staffUser = userDAO.findById(staffId);
        if (staffUser != null) {
            order.setStaff(staffUser);
        }
        ordersDAO.save(order);
        return true;
    }

    public boolean updateStatus(int orderId, String status, int staffId) {
        return updateStatus(orderId, status, staffId, null);
    }
}
