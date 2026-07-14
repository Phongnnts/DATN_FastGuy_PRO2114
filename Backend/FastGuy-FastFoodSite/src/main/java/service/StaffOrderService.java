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
    private OrderService orderService = new OrderService();
    private PayOSPaymentService payOSPaymentService = new PayOSPaymentService();
    private NotificationService notificationService = new NotificationService();
    private OrderStatusHistoryService orderStatusHistoryService = new OrderStatusHistoryService();

    public List<Orders> getPendingOrders() {
        List<Orders> pending = ordersDAO.findByStatus("PENDING");
        List<Orders> waiting = ordersDAO.findByStatus("WAITING_STOCK_CONFIRM");
        waiting.addAll(pending);
        return waiting;
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
        orderStatusHistoryService.record(orderId, shipperId, "SHIPPER", order.getOrderStatus(), order.getOrderStatus(), "Gán shipper " + shipper.getFullName());
        notificationService.notifyUser(shipperId, "Đơn giao mới", "Bạn được gán đơn " + order.getOrderCode(), "ORDER_ASSIGNED", "/shipper/orders/" + orderId);
        return true;
    }

    public boolean updateStatus(int orderId, String status, int staffId, String failureReason) {
        Orders order = ordersDAO.findById(orderId);
        if (order == null) return false;

        String current = order.getOrderStatus();

        switch (status) {
            case "PENDING":
                if (!"WAITING_STOCK_CONFIRM".equals(current)) return false;
                if ("BANK_TRANSFER".equals(order.getPaymentMethod())) {
                    if (!payOSPaymentService.isConfigured()) return false;
                    boolean confirmed = orderService.confirmStock(orderId, staffId);
                    if (!confirmed) return false;
                    if (payOSPaymentService.createPaymentLink(orderId) != null) return true;
                    orderService.revertStockConfirmation(orderId, staffId, "Không thể tạo link PayOS, vui lòng xác nhận lại");
                    return false;
                }
                order.setConfirmedAt(LocalDateTime.now());
                break;
            case "CONFIRMED":
                if (!"PENDING".equals(current)) return false;
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
                return orderService.cancelOrder(orderId, null, staffId, failureReason, false);
            default:
                return false;
        }

        order.setOrderStatus(status);
        User staffUser = userDAO.findById(staffId);
        if (staffUser != null) {
            order.setStaff(staffUser);
        }
        ordersDAO.save(order);
        orderStatusHistoryService.record(orderId, staffId, "STAFF", current, status, failureReason);
        if (order.getUser() != null) {
            notificationService.notifyUser(order.getUser().getUserId(), "Cập nhật đơn hàng", "Đơn " + order.getOrderCode() + " chuyển sang " + status, "ORDER_STATUS", "/account/orders/" + orderId);
        }
        return true;
    }

    public boolean updateStatus(int orderId, String status, int staffId) {
        return updateStatus(orderId, status, staffId, null);
    }
}
