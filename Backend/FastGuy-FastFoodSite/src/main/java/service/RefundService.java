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
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null) throw new IllegalArgumentException("Order not found");
            String error = validate(status, order.getRefundStatus(), order.getPaymentStatus(), order.getOrderStatus(), amount, order.getFinalAmount(), note);
            if (error != null) throw new IllegalArgumentException(error);
            if (isIdempotent(status, order.getRefundStatus())) {
                em.getTransaction().commit();
                return;
            }
            if (reverseForStatus(status)) {
                order.setPaymentStatus(paymentStatusFor(status));
                order.setRefundStatus(status);
                order.setRefundAmount(amount);
                order.setRefundNote(note);
                order.setRefundedAt(LocalDateTime.now());
                loyaltyService.reverseForRefund(em, order);
            } else {
                order.setRefundStatus(status);
                order.setRefundAmount(null);
                order.setRefundNote(note);
            }
            em.getTransaction().commit();
            if (order.getUser() != null) notificationService.notifyUser(order.getUser().getUserId(), "Cập nhật hoàn tiền", "Đơn " + order.getOrderCode() + " đã " + status, "REFUND", "/account/orders/" + orderId);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    static boolean isTerminal(String refundStatus) {
        return "REFUNDED".equals(refundStatus) || "REJECTED".equals(refundStatus);
    }

    static boolean isIdempotent(String requested, String current) {
        return isTerminal(current) && requested.equals(current);
    }

    static boolean reverseForStatus(String status) {
        return "REFUNDED".equals(status);
    }

    static String paymentStatusFor(String status) {
        return "REFUNDED".equals(status) ? "REFUNDED" : null;
    }

    static String validate(String status, String currentRefundStatus, String paymentStatus, String orderStatus,
                           BigDecimal amount, BigDecimal finalAmount, String note) {
        if (!"REFUNDED".equals(status) && !"REJECTED".equals(status)) return "Invalid refund status";
        if (isTerminal(currentRefundStatus)) {
            if (status.equals(currentRefundStatus)) return null;
            return "Refund already " + currentRefundStatus;
        }
        if (!"PAID".equals(paymentStatus) || !"CANCELLED".equals(orderStatus) || !"PENDING".equals(currentRefundStatus)) {
            return "Order is not eligible for refund";
        }
        if (reverseForStatus(status)) {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return "Invalid refund amount";
            if (finalAmount != null && amount.compareTo(finalAmount) > 0) return "Refund amount exceeds final amount";
        } else {
            if (note == null || note.isBlank()) return "Refund note is required for rejection";
        }
        return null;
    }
}
