package utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class PayOSClient {
    private static final String API_HOST = "https://api-merchant.payos.vn";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String clientId;
    private final String apiKey;
    private final String checksumKey;

    public PayOSClient(String clientId, String apiKey, String checksumKey) {
        this.clientId = clientId;
        this.apiKey = apiKey;
        this.checksumKey = checksumKey;
    }

    public Map<String, Object> createPaymentLink(int orderCode, long amount, String description,
                                                  String returnUrl, String cancelUrl) throws IOException {
        Map<String, String> sorted = new TreeMap<>();
        sorted.put("amount", String.valueOf(amount));
        sorted.put("cancelUrl", cancelUrl);
        sorted.put("description", description);
        sorted.put("orderCode", String.valueOf(orderCode));
        sorted.put("returnUrl", returnUrl);

        String signData = sorted.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        String signature = hmacSha256(signData, checksumKey);

        Map<String, Object> body = new HashMap<>();
        body.put("orderCode", orderCode);
        body.put("amount", amount);
        body.put("description", description);
        body.put("cancelUrl", cancelUrl);
        body.put("returnUrl", returnUrl);
        body.put("signature", signature);
        body.put("expiredAt", (System.currentTimeMillis() / 1000) + 1800);

        String json = MAPPER.writeValueAsString(body);

        HttpURLConnection conn = (HttpURLConnection) URI.create(API_HOST + "/v2/payment-requests").toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("x-client-id", clientId);
        conn.setRequestProperty("x-api-key", apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        String respBody;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                status >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                StandardCharsets.UTF_8))) {
            respBody = br.lines().collect(Collectors.joining("\n"));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> result = MAPPER.readValue(respBody, Map.class);
        if (status != 200) {
            String desc = result.containsKey("desc") ? (String) result.get("desc") : "PayOS error";
            throw new IOException("PayOS " + status + ": " + desc);
        }
        return result;
    }

    public static boolean verifyWebhookData(Map<String, Object> data, String signature, String checksumKey) {
        String computed = buildSignatureString(data, checksumKey);
        return computed.equals(signature);
    }

    private static String buildSignatureString(Map<String, Object> data, String checksumKey) {
        Map<String, Object> sorted = new TreeMap<>(data);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            if (!first) sb.append("&");
            first = false;
            sb.append(entry.getKey()).append("=");
            Object value = entry.getValue();
            if (value == null || "undefined".equals(value) || "null".equals(value)) {
                sb.append("");
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                List<Object> sortedList = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map) {
                        sortedList.add(new TreeMap<>((Map<String, Object>) item));
                    } else {
                        sortedList.add(item);
                    }
                }
                try {
                    sb.append(MAPPER.writeValueAsString(sortedList));
                } catch (Exception e) {
                    sb.append("[]");
                }
            } else {
                sb.append(value);
            }
        }
        return hmacSha256(sb.toString(), checksumKey);
    }

    private static String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC error", e);
        }
    }
}
