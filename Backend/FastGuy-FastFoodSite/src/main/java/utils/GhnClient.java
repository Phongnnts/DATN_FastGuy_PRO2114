package utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
        this.token = AppConfig.getGhnToken();
        this.shopId = AppConfig.getGhnShopId();
        this.host = AppConfig.getGhnHost();
    }

    private HttpRequest.Builder buildRequest(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(host + path))
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
                    result.put("fee", dataMap.get("total"));
                    result.put("serviceId", dataMap.get("service_id"));
                    result.put("expectedDeliveryTime", dataMap.get("expected_delivery_time"));
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "GHN fee calculation failed");
            return error;
        }
    }

    public List<Map<String, Object>> getServices(int toDistrictId) {
        try {
            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("shop_id", Integer.parseInt(shopId));
            bodyMap.put("to_district_id", toDistrictId);
            String json = mapper.writeValueAsString(bodyMap);
            HttpRequest req = buildRequest("/shiip/public-api/v2/shipping-order/available-services")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            return extractDataList(parseResponse(res));
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public Map<String, Object> createOrder(int toDistrictId, String toWardCode, String toAddress,
            String toPhone, String toName, int weight, int length, int width, int height,
            int serviceTypeId, int codAmount, String note) {
        try {
            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("service_type_id", serviceTypeId > 0 ? serviceTypeId : 2);
            bodyMap.put("from_district_id", Integer.parseInt(AppConfig.getGhnFromDistrictId()));
            bodyMap.put("from_ward_code", AppConfig.getGhnFromWardCode());
            bodyMap.put("to_district_id", toDistrictId);
            bodyMap.put("to_ward_code", toWardCode);
            bodyMap.put("to_address", toAddress);
            bodyMap.put("to_phone", toPhone);
            bodyMap.put("to_name", toName);
            bodyMap.put("weight", weight);
            bodyMap.put("length", length);
            bodyMap.put("width", width);
            bodyMap.put("height", height);
            bodyMap.put("cod_amount", codAmount);
            bodyMap.put("content", note != null ? note : "");
            bodyMap.put("payment_type_id", 1);
            bodyMap.put("required_note", "CHOXEMHANGKHONGTHU");
            if (!shopId.isEmpty()) bodyMap.put("shop_id", Integer.parseInt(shopId));
            String json = mapper.writeValueAsString(bodyMap);
            HttpRequest req = buildRequest("/shiip/public-api/v2/shipping-order/create")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> body = parseResponse(res);
            Map<String, Object> result = new HashMap<>();
            if (body.containsKey("data")) {
                Object data = body.get("data");
                if (data instanceof Map) {
                    Map<String, Object> dm = (Map<String, Object>) data;
                    result.put("orderCode", dm.get("order_code"));
                    result.put("totalFee", dm.get("total_fee"));
                    result.put("expectedDeliveryTime", dm.get("expected_delivery_time"));
                }
            } else {
                result.put("error", body.get("message"));
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "GHN order creation failed");
            return error;
        }
    }

    public Map<String, Object> getOrderStatus(String ghnOrderCode) {
        try {
            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("order_code", ghnOrderCode);
            String json = mapper.writeValueAsString(bodyMap);
            HttpRequest req = buildRequest("/shiip/public-api/v2/shipping-order/detail")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> body = parseResponse(res);
            Map<String, Object> result = new HashMap<>();
            if (body.containsKey("data")) {
                Object data = body.get("data");
                if (data instanceof Map) {
                    Map<String, Object> dm = (Map<String, Object>) data;
                    result.put("status", dm.get("status"));
                    result.put("log", dm.get("log"));
                    result.put("expectedDeliveryTime", dm.get("expected_delivery_time"));
                    result.put("trackingUrl", "https://donhang.ghn.vn/?order_code=" + ghnOrderCode);
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "GHN tracking failed");
            return error;
        }
    }

    public boolean cancelOrder(String ghnOrderCode) {
        try {
            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("order_code", ghnOrderCode);
            String json = mapper.writeValueAsString(bodyMap);
            HttpRequest req = buildRequest("/shiip/public-api/v2/shipping-order/cancel")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            return res.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
