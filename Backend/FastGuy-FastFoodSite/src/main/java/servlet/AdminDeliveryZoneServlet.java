package servlet;

import dao.DeliveryZoneDAO;
import entity.DeliveryZone;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;
import utils.JwtUtil;
import utils.JsonUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/admin/delivery-zones/*")
public class AdminDeliveryZoneServlet extends HttpServlet {
    private DeliveryZoneDAO zoneDAO = new DeliveryZoneDAO();

    private boolean checkAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return false;
        }
        String role = JwtUtil.getRole(authHeader.substring(7));
        if (!"ADMIN".equals(role)) {
            ApiResponse.error(resp, "Forbidden", 403);
            return false;
        }
        return true;
    }

    private Map<String, Object> toMap(DeliveryZone z) {
        Map<String, Object> m = new HashMap<>();
        m.put("zoneId", z.getZoneId());
        m.put("districtName", z.getDistrictName());
        m.put("shippingFee", z.getShippingFee());
        m.put("isActive", z.getIsActive());
        return m;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            List<Map<String, Object>> list = zoneDAO.findAll().stream()
                    .map(this::toMap).collect(Collectors.toList());
            ApiResponse.ok(resp, list);
        } else {
            try {
                int id = Integer.parseInt(path.substring(1));
                DeliveryZone z = zoneDAO.findById(id);
                if (z == null) { ApiResponse.error(resp, "Not found", 404); return; }
                ApiResponse.ok(resp, toMap(z));
            } catch (NumberFormatException e) {
                resp.sendError(404);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }

        DeliveryZone z = new DeliveryZone();
        z.setDistrictName((String) body.get("districtName"));
        z.setShippingFee(BigDecimal.valueOf(((Number) body.getOrDefault("shippingFee", 0)).doubleValue()));
        z.setIsActive(body.containsKey("isActive") ? (Boolean) body.get("isActive") : true);
        zoneDAO.save(z);

        resp.setStatus(201);
        ApiResponse.ok(resp, toMap(z), "Created");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { resp.sendError(404); return; }

        int id = Integer.parseInt(path.substring(1));
        DeliveryZone z = zoneDAO.findById(id);
        if (z == null) { ApiResponse.error(resp, "Not found", 404); return; }

        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }

        if (body.containsKey("districtName")) z.setDistrictName((String) body.get("districtName"));
        if (body.containsKey("shippingFee")) z.setShippingFee(BigDecimal.valueOf(((Number) body.get("shippingFee")).doubleValue()));
        if (body.containsKey("isActive")) z.setIsActive((Boolean) body.get("isActive"));
        zoneDAO.save(z);

        ApiResponse.ok(resp, toMap(z), "Updated");
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { resp.sendError(404); return; }

        int id = Integer.parseInt(path.substring(1));
        zoneDAO.delete(id);
        ApiResponse.ok(resp, null, "Deleted");
    }
}
