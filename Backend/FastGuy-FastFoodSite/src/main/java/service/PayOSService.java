package service;

import dao.OrdersDAO;
import entity.Orders;
import utils.PayOSClient;
import utils.PayOSConfig;

import java.time.LocalDateTime;
import java.util.Map;

public class PayOSService {
    private OrdersDAO ordersDAO = new OrdersDAO();

    public Map<String, Object> createPayment(Orders order) throws Exception {
        if (!PayOSConfig.isConfigured()) {
            throw new IllegalStateException("PayOS chưa được cấu hình. Vui lòng thêm biến môi trường PAYOS_CLIENT_ID, PAYOS_API_KEY, PAYOS_CHECKSUM_KEY");
        }
        PayOSClient client = new PayOSClient(
            PayOSConfig.getClientId(),
            PayOSConfig.getApiKey(),
            PayOSConfig.getChecksumKey()
        );

        long amount = order.getFinalAmount().longValueExact();
        String description = "TT " + order.getOrderCode();
        if (description.length() > 25) {
            description = description.substring(0, 25);
        }

        Map<String, Object> result = client.createPaymentLink(
            order.getOrderId(),
            amount,
            description,
            PayOSConfig.getReturnUrl(),
            PayOSConfig.getReturnUrl()
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        return data;
    }

    public boolean handleWebhook(Map<String, Object> payload) {
        try {
            if (!PayOSConfig.isConfigured()) return false;

            String code = (String) payload.get("code");
            if (!"00".equals(code)) return false;

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            if (data == null) return false;

            String signature = (String) payload.get("signature");
            if (signature == null) return false;

            boolean valid = PayOSClient.verifyWebhookData(data, signature, PayOSConfig.getChecksumKey());
            if (!valid) return false;

            Object orderCodeObj = data.get("orderCode");
            if (orderCodeObj == null) return false;

            int orderId;
            if (orderCodeObj instanceof Number) {
                orderId = ((Number) orderCodeObj).intValue();
            } else {
                orderId = Integer.parseInt(orderCodeObj.toString());
            }

            Orders order = ordersDAO.findById(orderId);
            if (order == null) return false;
            if ("PAID".equals(order.getPaymentStatus())) return false;

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
