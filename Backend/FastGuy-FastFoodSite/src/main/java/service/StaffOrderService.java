package service;

import dao.OrderItemDAO;
import dao.OrdersDAO;
import dao.UserDAO;
import entity.Orders;
import entity.User;
import entity.WorkShift;
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

    public List<WorkShift> getAvailableShipperShifts() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            LocalDateTime now = WorkShiftService.businessNow();
            return em.createQuery("SELECT ws FROM WorkShift ws WHERE ws.user.role = 'SHIPPER' AND ws.user.status = 'ACTIVE' " +
                            "AND ws.shiftDate = :today AND ws.status = 'CHECKED_IN' AND ws.checkInAt IS NOT NULL " +
                            "AND ws.checkOutAt IS NULL ORDER BY ws.checkInAt DESC, ws.shiftId DESC", WorkShift.class)
                    .setParameter("today", now.toLocalDate())
                    .getResultList().stream()
                    .filter(shift -> WorkShiftService.isValidCheckedInShift(shift, now))
                    .collect(java.util.stream.Collectors.toMap(shift -> shift.getUser().getUserId(), shift -> shift, (first, ignored) -> first, java.util.LinkedHashMap::new))
                    .values().stream().toList();
        } finally {
            em.close();
        }
    }

    public long countActiveOrders(int shipperId, LocalDateTime shiftStart) {
        return ordersDAO.countActiveByShipper(shipperId, shiftStart);
    }

    public OrderTransitionService.MutationResult assignShipper(int orderId, int shipperId, int staffId, String expectedStatus) {
        Orders order = getOrderDetail(orderId);
        OrderTransitionService.MutationResult result = transitionService.transition(orderId, "ASSIGNED", "STAFF", staffId, "Gán shipper", shipperId, null, expectedStatus);
        return result;
    }

    public OrderTransitionService.MutationResult updateStatus(int orderId, String status, int staffId, String failureReason, String expectedStatus) {
        Orders order = getOrderDetail(orderId);
        OrderTransitionService.MutationResult result = transitionService.transition(orderId, status, "STAFF", staffId, failureReason, null, null, expectedStatus);
        if (result == OrderTransitionService.MutationResult.SUCCESS && order != null && order.getUser() != null) {
        }
        return result;
    }

    public boolean updateStatus(int orderId, String status, int staffId) {
        return transitionService.transition(orderId, status, "STAFF", staffId, null, null, null);
    }

    public List<Orders> getDeliveryFailureQueue() {
        return ordersDAO.findDeliveryFailureQueue();
    }

    public OrderTransitionService.MutationResult retryDelivery(int orderId, int staffId, String expectedStatus, int shipperId, String retryMode, LocalDateTime scheduledAt, String note) {
        return transitionService.retryDelivery(orderId, staffId, expectedStatus, shipperId, retryMode, scheduledAt, note);
    }

    public OrderTransitionService.MutationResult startScheduledRetry(int orderId, int staffId, String expectedStatus) {
        return transitionService.startScheduledRetry(orderId, staffId, expectedStatus);
    }

    public OrderTransitionService.MutationResult returnToStore(int orderId, int staffId, String expectedStatus, String note) {
        return transitionService.returnToStore(orderId, staffId, expectedStatus, note);
    }
}
