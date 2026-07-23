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
    private OrderService orderService = new OrderService();
    private NotificationService notificationService = new NotificationService();
    private OrderStatusHistoryService orderStatusHistoryService = new OrderStatusHistoryService();
    private InventoryReservationService inventoryReservationService = new InventoryReservationService();

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
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null || !"READY".equals(order.getOrderStatus()) || order.getShipper() != null) {
                em.getTransaction().rollback();
                return false;
            }
            User shipper = em.find(User.class, shipperId);
            if (shipper == null || !"SHIPPER".equals(shipper.getRole())) {
                em.getTransaction().rollback();
                return false;
            }
            order.setShipper(shipper);
            order.setAssignedAt(LocalDateTime.now());
            em.getTransaction().commit();
            orderStatusHistoryService.record(orderId, shipperId, "SHIPPER", "READY", "READY", "Gán shipper " + shipper.getFullName());
            notificationService.notifyUser(shipperId, "Đơn giao mới", "Bạn được gán đơn " + order.getOrderCode(), "ORDER_ASSIGNED", "/shipper/orders/" + orderId);
            return true;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean updateStatus(int orderId, String status, int staffId, String failureReason) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null) { em.getTransaction().rollback(); return false; }

            String current = order.getOrderStatus();

            switch (status) {
                case "CONFIRMED":
                    if (!"PENDING".equals(current)) { em.getTransaction().rollback(); return false; }
                    order.setConfirmedAt(LocalDateTime.now());
                    break;
                case "PREPARING":
                    if (!"CONFIRMED".equals(current)) { em.getTransaction().rollback(); return false; }
                    if (!inventoryReservationService.transition(em, order, "CONSUMED")) { em.getTransaction().rollback(); return false; }
                    break;
                case "READY":
                    if (!"PREPARING".equals(current)) { em.getTransaction().rollback(); return false; }
                    order.setReadyAt(LocalDateTime.now());
                    break;
                case "CANCELLED":
                    em.getTransaction().rollback();
                    return orderService.cancelOrder(orderId, null, staffId, failureReason, false);
                default:
                    em.getTransaction().rollback();
                    return false;
            }

            order.setOrderStatus(status);
            User staffUser = em.find(User.class, staffId);
            if (staffUser != null) order.setStaff(staffUser);
            em.getTransaction().commit();
            orderStatusHistoryService.record(orderId, staffId, "STAFF", current, status, failureReason);
            if (order.getUser() != null) {
                notificationService.notifyUser(order.getUser().getUserId(), "Cập nhật đơn hàng", "Đơn " + order.getOrderCode() + " chuyển sang " + status, "ORDER_STATUS", "/account/orders/" + orderId);
            }
            return true;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean updateStatus(int orderId, String status, int staffId) {
        return updateStatus(orderId, status, staffId, null);
    }
}
