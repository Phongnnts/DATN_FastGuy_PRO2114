package utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GhnClient {
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final String token;
    private final String shopId;
    private final String host;

    public GhnClient() {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.mapper = new ObjectMapper();
        this.token = AppConfig.getGhnToken();
        this.shopId = AppConfig.getGhnShopId();
        this.host = AppConfig.getGhnHost();
    }

    private HttpRequest.Builder buildRequest(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(host + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json");
        if (!token.isEmpty()) {
            builder.header("Token", token);
        }
        if (!shopId.isEmpty()) {
            builder.header("ShopId", shopId);
        }
        return builder;
    }

    private Map<String, Object> parseResponse(HttpResponse<String> response) {
        try {
            Map<String, Object> body = mapper.readValue(response.body(),
                    new TypeReference<Map<String, Object>>() {});
            return body;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to parse GHN response");
            return error;
        }
    }

    private List<Map<String, Object>> extractDataList(Map<String, Object> response) {
        if (response.containsKey("data")) {
            Object data = response.get("data");
            if (data instanceof List) {
                return (List<Map<String, Object>>) data;
            }
        }
        return new ArrayList<>();
    }

    public List<Map<String, Object>> getProvinces() {
        try {
            HttpRequest req = buildRequest("/shiip/public-api/master-data/province")
                    .GET()
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> body = parseResponse(res);
            return extractDataList(body);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getDistricts(int provinceId) {
        try {
            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("province_id", provinceId);
            String json = mapper.writeValueAsString(bodyMap);

            HttpRequest req = buildRequest("/shiip/public-api/master-data/district")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> body = parseResponse(res);
            return extractDataList(body);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getWards(int districtId) {
        try {
            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("district_id", districtId);
            String json = mapper.writeValueAsString(bodyMap);

            HttpRequest req = buildRequest("/shiip/public-api/master-data/ward")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> body = parseResponse(res);
            return extractDataList(body);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public Map<String, Object> calculateFee(int toDistrictId, String toWardCode, int weight, int length, int width, int height) {
        try {
            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("service_type_id", 2);
            bodyMap.put("to_district_id", toDistrictId);
            bodyMap.put("to_ward_code", toWardCode);
            bodyMap.put("weight", weight);
            bodyMap.put("length", length);
            bodyMap.put("width", width);
            bodyMap.put("height", height);
            if (!shopId.isEmpty()) {
                bodyMap.put("shop_id", Integer.parseInt(shopId));
            }
            String json = mapper.writeValueAsString(bodyMap);

            HttpRequest req = buildRequest("/shiip/public-api/v2/shipping-order/fee")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> body = parseResponse(res);

            Map<String, Object> result = new HashMap<>();
            if (body.containsKey("data")) {
                Object data = body.get("data");
                if (data instanceof Map) {
                    Map<String, Object> dataMap = (Map<String, Object>) data;
                    Object total = dataMap.get("total");
                    if (total instanceof Number) {
                        result.put("fee", total);
                        result.put("serviceId", dataMap.get("service_id"));
                        result.put("expectedDeliveryTime", dataMap.get("expected_delivery_time"));
                        return result;
                    }
                }
            }
            result.put("error", String.valueOf(body.getOrDefault("message", "GHN fee calculation failed")));
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "GHN fee calculation failed");
            return error;
        }
    }


}
