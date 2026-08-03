package service;

import dao.OrderItemDAO;
import dao.OrdersDAO;
import dao.UserDAO;
import entity.Orders;
import entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaffOrderService {
    private OrdersDAO ordersDAO = new OrdersDAO();
    private OrderItemDAO orderItemDAO = new OrderItemDAO();
    private UserDAO userDAO = new UserDAO();
    private OrderTransitionService transitionService = new OrderTransitionService();
    private NotificationService notificationService = new NotificationService();

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
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT DISTINCT ws.user FROM WorkShift ws WHERE ws.user.role = 'SHIPPER' AND ws.user.status = 'ACTIVE' AND ws.status = 'CHECKED_IN' AND ws.checkInAt IS NOT NULL AND ws.checkOutAt IS NULL", User.class).getResultList();
        } finally {
            em.close();
        }
    }

    public long countActiveOrders(int shipperId) {
        return ordersDAO.countActiveByShipper(shipperId);
    }

    public boolean assignShipper(int orderId, int shipperId, int staffId) {
        Orders order = getOrderDetail(orderId);
        boolean ok = transitionService.transition(orderId, "ASSIGNED", "STAFF", staffId, "Gán shipper", shipperId, null);
        if (ok && order != null) notificationService.notifyUser(shipperId, "Đơn giao mới", "Bạn được gán đơn " + order.getOrderCode(), "ORDER_ASSIGNED", "/shipper/orders/" + orderId);
        return ok;
    }

    public boolean updateStatus(int orderId, String status, int staffId, String failureReason) {
        Orders order = getOrderDetail(orderId);
        boolean ok = transitionService.transition(orderId, status, "STAFF", staffId, failureReason, null, null);
        if (ok && order != null && order.getUser() != null) {
            notificationService.notifyUser(order.getUser().getUserId(), "Cập nhật đơn hàng", "Đơn " + order.getOrderCode() + " chuyển sang " + status, "ORDER_STATUS", "/account/orders/" + orderId);
        }
        return ok;
    }

    public boolean updateStatus(int orderId, String status, int staffId) {
        return updateStatus(orderId, status, staffId, null);
    }
}
