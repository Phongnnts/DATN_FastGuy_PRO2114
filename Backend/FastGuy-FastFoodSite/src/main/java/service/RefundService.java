package service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.function.Supplier;

import entity.Orders;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

public class RefundService {
    private LoyaltyService loyaltyService = new LoyaltyService();
    private final ActivityLogService activityLogService = new ActivityLogService();
    private final Supplier<EntityManager> entityManagers;
    private final RefundProofStorage proofStorage;

    public RefundService() { this(DatabaseUtil::getEntityManager, null); }
    RefundService(Supplier<EntityManager> entityManagers, RefundProofStorage proofStorage) { this.entityManagers = entityManagers; this.proofStorage = proofStorage; }

    public void update(int orderId, String status, BigDecimal amount, String note, String reference, int adminId) {
        update(orderId, null, status, amount, note, reference, null, adminId);
    }

    public void update(int orderId, String expectedStatus, String status, BigDecimal amount, String note, String reference,
                       RefundProofStorage.UploadedProof proof, int adminId) {
        EntityManager em = entityManagers.get();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null) throw new IllegalArgumentException("Order not found");
            if (expectedStatus != null && !java.util.Objects.equals(expectedStatus, order.getRefundStatus())) throw new RefundConflictException("Refund request changed");
            String normalizedNote = normalize(note);
            String normalizedReference = normalize(reference);
            if (isTerminal(order.getRefundStatus())) {
                if (matchesTerminalRequest(order, status, amount, normalizedNote, normalizedReference)
                        && (!"REFUNDED".equals(status) || proof != null && java.util.Objects.equals(proof.publicId(), order.getRefundProofPublicId()))) {
                    em.getTransaction().commit();
                    return;
                }
                throw new RefundConflictException("Refund request conflicts with terminal result");
            }
            String error = validate(status, order.getRefundStatus(), order.getPaymentStatus(), order.getOrderStatus(), amount, order.getFinalAmount(), normalizedNote, normalizedReference);
            if (error != null) throw new IllegalArgumentException(error);
            if ("REFUNDED".equals(status) && proof == null) throw new IllegalArgumentException("Refund proof is required");
            if ("REJECTED".equals(status) && proof != null) throw new IllegalArgumentException("Rejected refund must not include proof");
            if (reverseForStatus(status)) {
                order.setPaymentStatus(paymentStatusFor(status));
                order.setRefundStatus(status);
                order.setRefundAmount(amount);
                order.setRefundNote(normalizedNote);
                order.setRefundReference(normalizedReference);
                order.setRefundProofPublicId(proof.publicId());
                order.setRefundProofContentType(proof.contentType());
                order.setRefundProofUploadedAt(LocalDateTime.ofInstant(proof.uploadedAt(), ZoneOffset.UTC));
                order.setRefundedAt(LocalDateTime.now());
                loyaltyService.reverseForRefund(em, order);
            } else {
                order.setRefundStatus(status);
                order.setRefundAmount(null);
                order.setRefundNote(normalizedNote);
                order.setRefundReference(null);
                order.setRefundProofPublicId(null);
                order.setRefundProofContentType(null);
                order.setRefundProofUploadedAt(null);
            }
            order.setRefundProcessedBy(adminId);
            activityLogService.append(em,adminId,"ORDER_REFUND_RECORDED","ORDER",orderId,java.util.Map.of("refundStatus",status,"refundAmount",amount==null?"0":amount.toPlainString()));
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public RefundProofStorage.SignedProofUrl proofViewUrl(int orderId) { return proofViewUrl(orderId, proofStorage); }

    public RefundProofStorage.SignedProofUrl proofViewUrl(int orderId, RefundProofStorage storage) {
        EntityManager em = entityManagers.get();
        try {
            Orders order = em.find(Orders.class, orderId);
            if (order == null) throw new IllegalArgumentException("Order not found");
            if (storage == null) throw new IllegalStateException("Refund proof storage unavailable");
            return storage.signedViewUrl(order.getRefundProofPublicId(), Duration.ofMinutes(5));
        } finally { em.close(); }
    }

    static boolean isTerminal(String refundStatus) {
        return "REFUNDED".equals(refundStatus) || "REJECTED".equals(refundStatus);
    }

    static boolean isIdempotent(String requested, String current) {
        return isTerminal(current) && requested.equals(current);
    }

    static boolean matchesTerminalRequest(Orders order, String status, BigDecimal amount, String note, String reference) {
        if (!java.util.Objects.equals(status, order.getRefundStatus())) return false;
        if (!java.util.Objects.equals(note, normalize(order.getRefundNote()))) return false;
        if (!java.util.Objects.equals(reference, normalize(order.getRefundReference()))) return false;
        return java.util.Objects.equals(amount, order.getRefundAmount())
                || amount != null && order.getRefundAmount() != null && amount.compareTo(order.getRefundAmount()) == 0;
    }

    static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static boolean reverseForStatus(String status) {
        return "REFUNDED".equals(status);
    }

    static String paymentStatusFor(String status) {
        return "REFUNDED".equals(status) ? "REFUNDED" : null;
    }

    static String validate(String status, String currentRefundStatus, String paymentStatus, String orderStatus,
                           BigDecimal amount, BigDecimal finalAmount, String note) {
        return validate(status, currentRefundStatus, paymentStatus, orderStatus, amount, finalAmount, note, null);
    }

    static String validate(String status, String currentRefundStatus, String paymentStatus, String orderStatus,
                           BigDecimal amount, BigDecimal finalAmount, String note, String reference) {
        if (!"REFUNDED".equals(status) && !"REJECTED".equals(status)) return "Invalid refund status";
        if (isTerminal(currentRefundStatus)) return "Refund already " + currentRefundStatus;
        if (!"PAID".equals(paymentStatus) || !Set.of("CANCELLED", "RETURNED_TO_STORE").contains(orderStatus) || !"PENDING".equals(currentRefundStatus)) {
            return "Order is not eligible for refund";
        }
        if (reverseForStatus(status)) {
            if (amount == null || finalAmount == null || amount.compareTo(finalAmount) != 0) return "Full refund amount must equal final amount";
            if (reference == null || reference.isBlank()) return "Refund reference is required";
        } else if (note == null || note.isBlank()) {
            return "Refund note is required for rejection";
        }
        return null;
    }

    public static class RefundConflictException extends RuntimeException {
        public RefundConflictException(String message) { super(message); }
    }
}
