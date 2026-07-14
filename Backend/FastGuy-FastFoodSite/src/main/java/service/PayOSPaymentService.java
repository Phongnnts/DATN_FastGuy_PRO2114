package service;

import entity.Orders;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

import java.time.LocalDateTime;
import java.util.Map;

public class PayOSPaymentService {
    private final PayOSService payOSService = new PayOSService();
    private final OrderStatusHistoryService historyService = new OrderStatusHistoryService();

    public boolean isConfigured() {
        return payOSService.isConfigured();
    }

    public String createPaymentLink(int orderId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            Orders order = em.find(Orders.class, orderId);
            if (order == null || !"BANK_TRANSFER".equals(order.getPaymentMethod()) || !"PENDING".equals(order.getOrderStatus())) return null;
            if (order.getPayosCheckoutUrl() != null && !order.getPayosCheckoutUrl().isBlank()) return order.getPayosCheckoutUrl();
            Map<String, Object> result = payOSService.createPaymentLink(order);
            if (result.containsKey("error")) return null;

            em.getTransaction().begin();
            Orders locked = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (locked.getPayosCheckoutUrl() == null || locked.getPayosCheckoutUrl().isBlank()) {
                locked.setPayosPaymentLinkId(String.valueOf(result.get("paymentLinkId")));
                locked.setPayosCheckoutUrl(String.valueOf(result.get("checkoutUrl")));
            }
            em.getTransaction().commit();
            return locked.getPayosCheckoutUrl();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            return null;
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
            if (!"PENDING".equals(order.getOrderStatus())) {
                em.getTransaction().rollback();
                return false;
            }
            String paymentLinkId = String.valueOf(data.getOrDefault("paymentLinkId", ""));
            if (order.getPayosPaymentLinkId() != null && !order.getPayosPaymentLinkId().equals(paymentLinkId)) {
                em.getTransaction().rollback();
                return false;
            }
            order.setPaymentStatus("PAID");
            order.setPaidAt(LocalDateTime.now());
            em.getTransaction().commit();
            historyService.record(orderId, null, "PAYOS", "PENDING", "PENDING", "PayOS xác nhận thanh toán");
            return true;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            return false;
        } finally {
            em.close();
        }
    }
}
