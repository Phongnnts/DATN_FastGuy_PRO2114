package service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import entity.Orders;
import utils.AppConfig;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class PayOSService {
    private static final String API_URL = "https://api-merchant.payos.vn/v2/payment-requests";
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> createPaymentLink(Orders order) {
        if (!isConfigured()) return Map.of("error", "PayOS chưa được cấu hình");
        if (order.getFinalAmount() == null || order.getFinalAmount().signum() <= 0) return Map.of("error", "Số tiền thanh toán không hợp lệ");

        try {
            long amount = order.getFinalAmount().longValueExact();
            Map<String, Object> payload = new HashMap<>();
            payload.put("orderCode", order.getOrderId());
            payload.put("amount", amount);
            payload.put("description", "FG" + order.getOrderId());
            payload.put("buyerName", order.getCustomerName());
            payload.put("buyerAddress", order.getCustomerAddress());
            payload.put("buyerPhone", order.getCustomerPhone());
            String returnUrl = AppConfig.getAppWebUrl() + "/payment-return?orderId=" + order.getOrderId() + "&orderCode=" + order.getOrderCode();
            payload.put("returnUrl", returnUrl);
            payload.put("cancelUrl", returnUrl);
            payload.put("expiredAt", Instant.now().plusSeconds(15 * 60).getEpochSecond());
            payload.put("signature", signature(Map.of(
                    "amount", amount,
                    "cancelUrl", AppConfig.getAppWebUrl() + "/payment-return?orderId=" + order.getOrderId() + "&orderCode=" + order.getOrderCode(),
                    "description", payload.get("description"),
                    "orderCode", order.getOrderId(),
                    "returnUrl", AppConfig.getAppWebUrl() + "/payment-return?orderId=" + order.getOrderId() + "&orderCode=" + order.getOrderCode()
            )));

            HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("x-client-id", AppConfig.getPayosClientId())
                    .header("x-api-key", AppConfig.getPayosApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> body = mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
            if (response.statusCode() / 100 != 2 || !"00".equals(String.valueOf(body.get("code")))) {
                return Map.of("error", String.valueOf(body.getOrDefault("desc", "Không thể tạo link PayOS")));
            }
            Object data = body.get("data");
            if (!(data instanceof Map<?, ?> result)) return Map.of("error", "PayOS không trả link thanh toán");
            Object checkoutUrl = result.get("checkoutUrl");
            Object paymentLinkId = result.get("paymentLinkId");
            if (checkoutUrl == null || paymentLinkId == null) return Map.of("error", "PayOS không trả link thanh toán");
            return Map.of("checkoutUrl", String.valueOf(checkoutUrl), "paymentLinkId", String.valueOf(paymentLinkId));
        } catch (Exception e) {
            return Map.of("error", "Không thể kết nối PayOS");
        }
    }

    public boolean isValidWebhook(Map<String, Object> data, String receivedSignature) {
        if (!isConfigured() || data == null || receivedSignature == null) return false;
        return constantTimeEquals(signature(data), receivedSignature);
    }

    public boolean isConfigured() {
        return !AppConfig.getPayosClientId().isBlank() && !AppConfig.getPayosApiKey().isBlank() && !AppConfig.getPayosChecksumKey().isBlank();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getPaymentInfo(String paymentLinkId) {
        if (!isConfigured() || paymentLinkId == null || paymentLinkId.isBlank()) return Map.of("error", "PayOS chưa được cấu hình");
        try {
            String url = API_URL + "/" + paymentLinkId;
            Map<String, Object> payload = Map.of(
                    "paymentLinkId", paymentLinkId,
                    "signature", signature(Map.of("paymentLinkId", paymentLinkId))
            );
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("x-client-id", AppConfig.getPayosClientId())
                    .header("x-api-key", AppConfig.getPayosApiKey())
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> body = mapper.readValue(response.body(), new TypeReference<>() {});
            if (response.statusCode() / 100 != 2 || !"00".equals(String.valueOf(body.get("code")))) {
                return Map.of("error", String.valueOf(body.getOrDefault("desc", "Không thể kiểm tra thanh toán")));
            }
            Object data = body.get("data");
            if (data instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
            return Map.of("error", "Không có dữ liệu");
        } catch (Exception e) {
            return Map.of("error", "Không thể kết nối PayOS");
        }
    }


    private String signature(Map<String, Object> data) {
        String payload = data.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey() + "=" + value(entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(AppConfig.getPayosChecksumKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo chữ ký PayOS", e);
        }
    }

    private String value(Object value) {
        return value == null || "null".equals(value) || "undefined".equals(value) ? "" : String.valueOf(value);
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected.length() != actual.length()) return false;
        int diff = 0;
        for (int i = 0; i < expected.length(); i++) diff |= expected.charAt(i) ^ actual.charAt(i);
        return diff == 0;
    }
}
