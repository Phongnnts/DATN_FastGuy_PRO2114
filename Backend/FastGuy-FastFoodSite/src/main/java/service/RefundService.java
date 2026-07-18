package service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import entity.Orders;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

public class RefundService {
    private LoyaltyService loyaltyService = new LoyaltyService();
    private NotificationService notificationService = new NotificationService();

    public void update(int orderId, String status, BigDecimal amount, String note, int adminId) {
        if (!"REFUNDED".equals(status) && !"REJECTED".equals(status)) throw new IllegalArgumentException("Invalid refund status");
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null) throw new IllegalArgumentException("Order not found");
            if (!"PAID".equals(order.getPaymentStatus()) || !"CANCELLED".equals(order.getOrderStatus()) || !"PENDING".equals(order.getRefundStatus())) {
                throw new IllegalArgumentException("Order is not eligible for refund");
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0 || (order.getFinalAmount() != null && amount.compareTo(order.getFinalAmount()) > 0)) {
                throw new IllegalArgumentException("Invalid refund amount");
            }
            order.setRefundStatus(status);
            order.setRefundAmount(amount);
            order.setRefundNote(note);
            order.setRefundedAt(LocalDateTime.now());
            if ("REFUNDED".equals(status)) loyaltyService.reverseForRefund(em, order);
            em.getTransaction().commit();
            if (order.getUser() != null) notificationService.notifyUser(order.getUser().getUserId(), "Cập nhật hoàn tiền", "Đơn " + order.getOrderCode() + " đã " + status, "REFUND", "/account/orders/" + orderId);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
