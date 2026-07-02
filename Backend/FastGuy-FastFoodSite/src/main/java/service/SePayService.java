package service;

import dao.OrdersDAO;
import entity.Orders;
import utils.SePayConfig;

import java.time.LocalDateTime;
import java.util.Map;

public class SePayService {
    private OrdersDAO ordersDAO = new OrdersDAO();

    public String generateQrUrl(int orderId, long amount, String orderCode) {
        String description = "TT " + orderCode;
        return SePayConfig.buildQrUrl(amount, description);
    }

    public boolean processWebhook(Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> transaction = (Map<String, Object>) payload.get("transaction");
            if (transaction == null) return false;
            String description = (String) transaction.get("description");
            if (description == null || !description.startsWith("TT ")) return false;
            String orderCode = description.substring(3).trim();
            Orders order = ordersDAO.findByOrderCode(orderCode);
            if (order == null) return false;
            if ("PAID".equals(order.getPaymentStatus())) return false;
            Object amountObj = transaction.get("amount");
            if (amountObj instanceof Number) {
                long paidAmount = ((Number) amountObj).longValue();
                if (order.getFinalAmount() != null && paidAmount < order.getFinalAmount().longValue()) {
                    return false;
                }
            }
            order.setPaymentStatus("PAID");
            order.setPaidAt(LocalDateTime.now());
            ordersDAO.save(order);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
