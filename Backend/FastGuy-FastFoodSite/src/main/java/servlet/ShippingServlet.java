package servlet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.ShippingService;
import utils.ApiResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/shipping/*")
public class ShippingServlet extends HttpServlet {
    private ShippingService shippingService = new ShippingService();
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            ApiResponse.error(resp, "Invalid endpoint", 400);
            return;
        }

        switch (path) {
            case "/provinces": {
                ApiResponse.ok(resp, shippingService.getProvinces());
                break;
            }
            case "/districts": {
                String provinceIdParam = req.getParameter("provinceId");
                if (provinceIdParam == null) {
                    ApiResponse.error(resp, "Missing provinceId", 400);
                    return;
                }
                int provinceId = Integer.parseInt(provinceIdParam);
                ApiResponse.ok(resp, shippingService.getDistricts(provinceId));
                break;
            }
            case "/wards": {
                String districtIdParam = req.getParameter("districtId");
                if (districtIdParam == null) {
                    ApiResponse.error(resp, "Missing districtId", 400);
                    return;
                }
                int districtId = Integer.parseInt(districtIdParam);
                ApiResponse.ok(resp, shippingService.getWards(districtId));
                break;
            }
            case "/services": {
                String districtIdParam = req.getParameter("toDistrictId");
                if (districtIdParam == null) {
                    ApiResponse.error(resp, "Missing toDistrictId", 400);
                    return;
                }
                int toDistrictId = Integer.parseInt(districtIdParam);
                ApiResponse.ok(resp, shippingService.getServices(toDistrictId));
                break;
            }
            default:
                ApiResponse.error(resp, "Not found", 404);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String path = req.getPathInfo();
        if ("/fee".equals(path)) {
            Map<String, Object> body = mapper.readValue(req.getReader(),
                    new TypeReference<Map<String, Object>>() {});
            if (body == null) {
                ApiResponse.error(resp, "Invalid request body", 400);
                return;
            }

            int toDistrictId = toInt(body.get("toDistrictId"));
            String toWardCode = (String) body.get("toWardCode");
            int weight = toInt(body.getOrDefault("weight", 500));
            int length = toInt(body.getOrDefault("length", 20));
            int width = toInt(body.getOrDefault("width", 20));
            int height = toInt(body.getOrDefault("height", 10));

            Map<String, Object> result = shippingService.calculateFee(toDistrictId, toWardCode, weight, length, width, height);
            ApiResponse.ok(resp, result);
        } else if ("/create-order".equals(path)) {
            Map<String, Object> body = mapper.readValue(req.getReader(),
                    new TypeReference<Map<String, Object>>() {});
            if (body == null || !body.containsKey("orderId")) {
                ApiResponse.error(resp, "Missing orderId", 400);
                return;
            }
            int orderId = ((Number) body.get("orderId")).intValue();
            Map<String, Object> result = shippingService.createGHNOrder(orderId);
            if (result.containsKey("error")) {
                ApiResponse.error(resp, (String) result.get("error"), 400);
                return;
            }
            ApiResponse.ok(resp, result, "GHN order created");
        } else if ("/services".equals(path)) {
            Map<String, Object> body = mapper.readValue(req.getReader(),
                    new TypeReference<Map<String, Object>>() {});
            if (body == null || !body.containsKey("toDistrictId")) {
                ApiResponse.error(resp, "Missing toDistrictId", 400);
                return;
            }
            int toDistrictId = ((Number) body.get("toDistrictId")).intValue();
            ApiResponse.ok(resp, shippingService.getServices(toDistrictId));
        } else {
            ApiResponse.error(resp, "Not found", 404);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String path = req.getPathInfo();
        if (path != null && path.startsWith("/track/")) {
            String ghnCode = path.substring("/track/".length());
            if (ghnCode.isEmpty()) {
                ApiResponse.error(resp, "Missing GHN order code", 400);
                return;
            }
            ApiResponse.ok(resp, shippingService.trackGHNOrder(ghnCode));
        } else {
            ApiResponse.error(resp, "Not found", 404);
        }
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
