package servlet;

import dao.CouponDAO;
import entity.Coupon;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;
import utils.JwtUtil;
import utils.JsonUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet("/api/admin/coupons/*")
public class AdminCouponServlet extends HttpServlet {
    private CouponDAO couponDAO = new CouponDAO();

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

    private Map<String, Object> toMap(Coupon c) {
        Map<String, Object> m = new HashMap<>();
        m.put("couponId", c.getCouponId());
        m.put("code", c.getCode());
        m.put("type", c.getType());
        m.put("value", c.getValue());
        m.put("minOrder", c.getMinOrder() != null ? c.getMinOrder() : BigDecimal.ZERO);
        m.put("maxDiscount", c.getMaxDiscount());
        m.put("maxUses", c.getMaxUses());
        m.put("usedCount", c.getUsedCount());
        m.put("expiresAt", c.getExpiresAt() != null ? c.getExpiresAt().toString() : null);
        m.put("isActive", c.getIsActive() != null ? c.getIsActive() : true);
        m.put("isPublic", c.getIsPublic() != null ? c.getIsPublic() : true);
        m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
        return m;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            List<Map<String, Object>> list = couponDAO.findAll().stream()
                    .map(this::toMap).collect(Collectors.toList());
            ApiResponse.ok(resp, list);
        } else {
            try {
                int id = Integer.parseInt(path.substring(1));
                Coupon c = couponDAO.findById(id);
                if (c == null) { ApiResponse.error(resp, "Not found", 404); return; }
                ApiResponse.ok(resp, toMap(c));
            } catch (NumberFormatException e) {
                ApiResponse.error(resp, "Invalid ID", 400);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }

        Coupon c = new Coupon();
        c.setCode(((String) body.get("code")).toUpperCase().trim());
        c.setType((String) body.get("type"));
        if (body.containsKey("value")) c.setValue(BigDecimal.valueOf(((Number) body.get("value")).doubleValue()));
        if (body.containsKey("minOrder")) c.setMinOrder(BigDecimal.valueOf(((Number) body.get("minOrder")).doubleValue()));
        if (body.containsKey("maxDiscount")) c.setMaxDiscount(BigDecimal.valueOf(((Number) body.get("maxDiscount")).doubleValue()));
        if (body.containsKey("maxUses")) c.setMaxUses(((Number) body.get("maxUses")).intValue());
        if (body.containsKey("expiresAt")) {
            try { c.setExpiresAt(LocalDateTime.parse((String) body.get("expiresAt"), DateTimeFormatter.ISO_LOCAL_DATE_TIME)); } catch (Exception e) {}
        }
        c.setIsActive(true);
        c.setIsPublic(body.containsKey("isPublic") ? (Boolean) body.get("isPublic") : true);
        c.setUsedCount(0);
        c.setCreatedAt(LocalDateTime.now());
        couponDAO.save(c);

        resp.setStatus(201);
        ApiResponse.ok(resp, toMap(c), "Created");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { ApiResponse.error(resp, "Missing ID", 400); return; }
        try {
            int id = Integer.parseInt(path.substring(1));
            Coupon c = couponDAO.findById(id);
            if (c == null) { ApiResponse.error(resp, "Not found", 404); return; }

            Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
            if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }

            if (body.containsKey("code")) c.setCode(((String) body.get("code")).toUpperCase().trim());
            if (body.containsKey("type")) c.setType((String) body.get("type"));
            if (body.containsKey("value")) c.setValue(BigDecimal.valueOf(((Number) body.get("value")).doubleValue()));
            if (body.containsKey("minOrder")) c.setMinOrder(BigDecimal.valueOf(((Number) body.get("minOrder")).doubleValue()));
            if (body.containsKey("maxDiscount")) c.setMaxDiscount(BigDecimal.valueOf(((Number) body.get("maxDiscount")).doubleValue()));
            if (body.containsKey("maxUses")) c.setMaxUses(((Number) body.get("maxUses")).intValue());
            if (body.containsKey("expiresAt")) {
                try { c.setExpiresAt(LocalDateTime.parse((String) body.get("expiresAt"), DateTimeFormatter.ISO_LOCAL_DATE_TIME)); } catch (Exception e) {}
            }
            if (body.containsKey("isActive")) c.setIsActive((Boolean) body.get("isActive"));
            if (body.containsKey("isPublic")) c.setIsPublic((Boolean) body.get("isPublic"));
            couponDAO.save(c);
            ApiResponse.ok(resp, toMap(c), "Updated");
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid ID", 400);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { ApiResponse.error(resp, "Missing ID", 400); return; }
        try {
            int id = Integer.parseInt(path.substring(1));
            couponDAO.delete(id);
            ApiResponse.ok(resp, null, "Deleted");
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid ID", 400);
        }
    }
}
