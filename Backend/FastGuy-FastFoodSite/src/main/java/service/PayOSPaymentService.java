package service;

import entity.Orders;
import entity.PaymentAttempt;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class PayOSPaymentService {
    private final PayOSService payOSService = new PayOSService();

    public boolean isConfigured() {
        return payOSService.isConfigured();
    }

    static void markPaid(Orders order, LocalDateTime paidAt) {
        order.setPaymentStatus("PAID");
        order.setPaidAt(paidAt);
        if ("CANCELLED".equals(order.getOrderStatus())) order.setRefundStatus("PENDING");
    }

    static boolean shouldCreateProviderLink(boolean newAttempt, String status) {
        return newAttempt && "CREATING".equals(status);
    }

    static boolean shouldMarkPaid(String paymentStatus, String refundStatus) {
        return !"PAID".equals(paymentStatus) && !RefundService.isTerminal(refundStatus);
    }

    static boolean matchesProviderResponse(Map<String, Object> result, int orderId, long amount) {
        return result.get("orderCode") instanceof Number code && code.intValue() == orderId
                && result.get("amount") instanceof Number value && value.longValue() == amount
                && result.get("paymentLinkId") != null && !String.valueOf(result.get("paymentLinkId")).isBlank();
    }

    public void storeGuestReturnProof(int orderId, String token) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null || order.getUser() != null) throw new IllegalArgumentException("Guest order not found");
            order.setGuestReturnProofHash(GuestReturnProof.hash(token));
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.close();
        }
    }

    public String createPaymentLink(int orderId) {
        return createPaymentLink(orderId, null);
    }

    public String createPaymentLink(int orderId, String guestReturnProof) {
        boolean ownsAttempt = false;
        String leaseToken = null;
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null || !"BANK_TRANSFER".equals(order.getPaymentMethod()) || !"PENDING".equals(order.getOrderStatus())) {
                em.getTransaction().rollback();
                return null;
            }
            if (order.getPayosCheckoutUrl() != null && !order.getPayosCheckoutUrl().isBlank()) {
                em.getTransaction().commit();
                return order.getPayosCheckoutUrl();
            }
            PaymentAttempt attempt = findAttempt(em, orderId);
            if (attempt == null) {
                attempt = new PaymentAttempt();
                attempt.setOrder(order);
                attempt.setProvider("PAYOS");
                attempt.setAmount(order.getFinalAmount());
                attempt.setStatus("CREATING");
                leaseToken = UUID.randomUUID().toString();
                attempt.setLeaseToken(leaseToken);
                em.persist(attempt);
                ownsAttempt = true;
            } else if ("READY".equals(attempt.getStatus())) {
                applyAttempt(order, attempt);
                em.getTransaction().commit();
                return order.getPayosCheckoutUrl();
            } else if ("FAILED".equals(attempt.getStatus())
                    || attempt.getUpdatedAt() == null
                    || attempt.getUpdatedAt().isBefore(LocalDateTime.now().minusSeconds(30))) {
                attempt.setStatus("CREATING");
                leaseToken = UUID.randomUUID().toString();
                attempt.setLeaseToken(leaseToken);
                ownsAttempt = true;
            }
            em.getTransaction().commit();

            if (!ownsAttempt) return null;

            Map<String, Object> result = payOSService.getPaymentInfo(String.valueOf(orderId));
            if (result.containsKey("error") && ownsAttempt) {
                result = payOSService.createPaymentLink(order, guestReturnProof);
            }
            if (result.containsKey("error")) {
                markAttemptFailed(orderId, leaseToken);
                return null;
            }
            return saveAttempt(orderId, result, leaseToken);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            return null;
        } finally {
            em.close();
        }
    }

    private PaymentAttempt findAttempt(EntityManager em, int orderId) {
        var attempts = em.createQuery("SELECT p FROM PaymentAttempt p WHERE p.order.orderId = :orderId", PaymentAttempt.class)
                .setParameter("orderId", orderId).setMaxResults(1).getResultList();
        return attempts.isEmpty() ? null : attempts.get(0);
    }

    private void applyAttempt(Orders order, PaymentAttempt attempt) {
        order.setPayosPaymentLinkId(attempt.getProviderReference());
        order.setPayosCheckoutUrl(attempt.getCheckoutUrl());
    }

    private String saveAttempt(int orderId, Map<String, Object> result, String leaseToken) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            PaymentAttempt attempt = findAttempt(em, orderId);
            if (order == null || attempt == null || !leaseToken.equals(attempt.getLeaseToken())
                    || !"BANK_TRANSFER".equals(order.getPaymentMethod())
                    || !"PENDING".equals(order.getOrderStatus()) || !"UNPAID".equals(order.getPaymentStatus())) {
                em.getTransaction().rollback(); return null;
            }
            if (!matchesProviderResponse(result, orderId, order.getFinalAmount().longValueExact())) {
                em.getTransaction().rollback(); return null;
            }
            String reference = String.valueOf(result.get("paymentLinkId"));
            String checkoutUrl = String.valueOf(result.getOrDefault("checkoutUrl", ""));
            if (checkoutUrl.isBlank()) { em.getTransaction().rollback(); return null; }
            attempt.setProviderReference(reference);
            attempt.setCheckoutUrl(checkoutUrl);
            attempt.setStatus("READY");
            applyAttempt(order, attempt);
            em.getTransaction().commit();
            return checkoutUrl;
        } finally {
            em.close();
        }
    }

    private void markAttemptFailed(int orderId, String leaseToken) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            PaymentAttempt attempt = findAttempt(em, orderId);
            if (attempt != null && leaseToken.equals(attempt.getLeaseToken()) && "CREATING".equals(attempt.getStatus())) attempt.setStatus("FAILED");
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public boolean verifyPayment(int orderId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            Orders order = em.find(Orders.class, orderId);
            if (order == null || !"BANK_TRANSFER".equals(order.getPaymentMethod())) return false;
            if ("PAID".equals(order.getPaymentStatus())) return true;
            if (order.getPayosPaymentLinkId() == null || order.getPayosPaymentLinkId().isBlank()) return false;

            Map<String, Object> info = payOSService.getPaymentInfo(order.getPayosPaymentLinkId());
            if (info.containsKey("error")) return false;

            String status = String.valueOf(info.getOrDefault("status", ""));
            if ("PAID".equals(status)) {
                em.getTransaction().begin();
                Orders locked = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
                if (shouldMarkPaid(locked.getPaymentStatus(), locked.getRefundStatus())) {
                    markPaid(locked, LocalDateTime.now());
                    PaymentAttempt attempt = findAttempt(em, orderId);
                    if (attempt != null) attempt.setStatus("PAID");
                }
                em.getTransaction().commit();
                return true;
            }
            return false;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            return false;
        } finally {
            em.close();
        }
    }

    public boolean isValidWebhook(Map<String, Object> payload) {
        Object rawData = payload.get("data");
        if (!(rawData instanceof Map<?, ?> map) || !(payload.get("signature") instanceof String signature)) return false;
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) map;
        return payOSService.isValidWebhook(data, signature);
    }

    public boolean processWebhook(Map<String, Object> payload) {
        if (!Boolean.TRUE.equals(payload.get("success")) || !isValidWebhook(payload)) return false;
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        if (!"00".equals(String.valueOf(data.get("code")))) return false;
        Object rawOrderCode = data.get("orderCode");
        Object rawAmount = data.get("amount");
        if (!(rawOrderCode instanceof Number) || !(rawAmount instanceof Number)) return false;

        int orderId = ((Number) rawOrderCode).intValue();
        long amount = ((Number) rawAmount).longValue();
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null || !"BANK_TRANSFER".equals(order.getPaymentMethod()) || order.getFinalAmount() == null
                    || order.getFinalAmount().longValue() != amount) {
                em.getTransaction().rollback();
                return false;
            }
            if ("PAID".equals(order.getPaymentStatus())) {
                em.getTransaction().commit();
                return true;
            }
            if (RefundService.isTerminal(order.getRefundStatus())) {
                em.getTransaction().commit();
                return true;
            }
            String paymentLinkId = String.valueOf(data.getOrDefault("paymentLinkId", ""));
            if (order.getPayosPaymentLinkId() != null && !order.getPayosPaymentLinkId().equals(paymentLinkId)) {
                em.getTransaction().rollback();
                return false;
            }
            markPaid(order, LocalDateTime.now());
            PaymentAttempt attempt = findAttempt(em, orderId);
            if (attempt != null) attempt.setStatus("PAID");
            em.getTransaction().commit();
            return true;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            return false;
        } finally {
            em.close();
        }
    }
}
