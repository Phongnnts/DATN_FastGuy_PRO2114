package servlet;

import dao.UserDAO;
import dao.OrdersDAO;
import entity.User;
import jakarta.persistence.PersistenceException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;
import utils.JsonUtil;
import utils.JwtUtil;
import utils.PasswordUtil;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@WebServlet("/api/admin/users/*")
public class AdminUserServlet extends HttpServlet {
    private static final Set<String> ROLES = Set.of("USER", "STAFF", "SHIPPER", "ADMIN");
    private final UserDAO userDAO = new UserDAO();
    private final OrdersDAO ordersDAO = new OrdersDAO();

    private boolean checkAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) { ApiResponse.error(resp, "Missing token", 401); return false; }
        if (!"ADMIN".equals(JwtUtil.getRole(auth.substring(7)))) { ApiResponse.error(resp, "Forbidden", 403); return false; }
        return true;
    }

    private Map<String, Object> toMap(User user) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserId()); result.put("fullName", user.getFullName() == null ? "" : user.getFullName());
        result.put("email", user.getEmail() == null ? "" : user.getEmail()); result.put("phone", user.getPhone() == null ? "" : user.getPhone());
        result.put("roleName", user.getRole() != null ? user.getRole() : ""); result.put("status", user.getStatus()); result.put("loyaltyPoints", user.getLoyaltyPoints());
        return result;
    }

    static String orderRelationship(String role) {
        return switch (role) {
            case "USER" -> "CUSTOMER";
            case "STAFF" -> "STAFF";
            case "SHIPPER" -> "SHIPPER";
            default -> "NONE";
        };
    }

    private List<entity.Orders> ordersFor(User user) {
        return switch (orderRelationship(user.getRole())) {
            case "CUSTOMER" -> ordersDAO.findByUserId(user.getUserId());
            case "STAFF" -> ordersDAO.findDeliveredByStaffId(user.getUserId());
            case "SHIPPER" -> ordersDAO.findByShipperIdAndStatus(user.getUserId(), "DELIVERED");
            default -> Collections.emptyList();
        };
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8"); if (!checkAdmin(req, resp)) return;
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            String roleFilter = req.getParameter("role");
            List<Map<String, Object>> list = userDAO.findAll().stream().map(this::toMap).collect(Collectors.toList());
            if (roleFilter != null && !roleFilter.isBlank()) {
                String rf = roleFilter.toUpperCase();
                list = list.stream().filter(m -> rf.equals(m.get("roleName"))).collect(Collectors.toList());
            }
            ApiResponse.ok(resp, list); return;
        }
        String[] parts = path.split("/");
        try {
            int userId = Integer.parseInt(parts[1]);
            User user = userDAO.findById(userId);
            if (user == null) { ApiResponse.error(resp, "Not found", 404); return; }
            if (parts.length >= 3 && "orders".equals(parts[2])) {
                List<Map<String, Object>> orders = ordersFor(user).stream().map(o -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("orderId", o.getOrderId()); m.put("orderCode", o.getOrderCode());
                    m.put("status", o.getOrderStatus()); m.put("finalAmount", o.getFinalAmount());
                    m.put("paymentMethod", o.getPaymentMethod()); m.put("paymentStatus", o.getPaymentStatus());
                    m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
                    m.put("completedAt", o.getDeliveredAt() != null ? o.getDeliveredAt().toString() : null);
                    return m;
                }).collect(Collectors.toList());
                ApiResponse.ok(resp, orders);
            } else {
                ApiResponse.ok(resp, toMap(user));
            }
        } catch (NumberFormatException e) { ApiResponse.error(resp, "Invalid ID", 400); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8"); if (!checkAdmin(req, resp)) return;
        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class); if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }
        try {
            User user = new User(); apply(body, user, true);
            if (userDAO.findByEmail(user.getEmail()) != null) { ApiResponse.error(resp, "Email already exists", 409); return; }
            if (user.getPhone() != null && !user.getPhone().isBlank() && userDAO.findByPhone(user.getPhone()) != null) { ApiResponse.error(resp, "Số điện thoại đã tồn tại", 409); return; }
            user.setStatus("ACTIVE"); user.setCreatedAt(LocalDateTime.now()); userDAO.save(user);
            resp.setStatus(201); ApiResponse.ok(resp, toMap(user), "Created");
        } catch (IllegalArgumentException e) { ApiResponse.error(resp, e.getMessage(), 400); }
        catch (PersistenceException e) { ApiResponse.error(resp, "Không thể lưu người dùng", 409); }
    }

    @Override protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8"); if (!checkAdmin(req, resp)) return;
        String path = req.getPathInfo();
        String[] parts = path != null ? path.split("/") : new String[0];

        if (parts.length >= 3 && "status".equals(parts[2])) {
            try {
                int userId = Integer.parseInt(parts[1]);
                User user = userDAO.findById(userId);
                if (user == null) { ApiResponse.error(resp, "Not found", 404); return; }
                Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
                String status = body != null ? (String) body.get("status") : null;
                if (status == null || (!"ACTIVE".equals(status) && !"INACTIVE".equals(status))) {
                    ApiResponse.error(resp, "Status must be ACTIVE or INACTIVE", 400); return;
                }
                user.setStatus(status);
                userDAO.save(user);
                ApiResponse.ok(resp, toMap(user), "Status updated");
            } catch (NumberFormatException e) { ApiResponse.error(resp, "Invalid ID", 400); }
            return;
        }

        try {
            User user = userDAO.findById(id(req)); if (user == null) { ApiResponse.error(resp, "Not found", 404); return; }
            Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class); if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }
            String oldEmail = user.getEmail(); String oldPhone = user.getPhone(); apply(body, user, false);
            if (!oldEmail.equals(user.getEmail())) { User existing = userDAO.findByEmail(user.getEmail()); if (existing != null && existing.getUserId() != user.getUserId()) { ApiResponse.error(resp, "Email already exists", 409); return; } }
            if (user.getPhone() != null && !user.getPhone().isBlank() && !user.getPhone().equals(oldPhone)) { User existing = userDAO.findByPhone(user.getPhone()); if (existing != null && existing.getUserId() != user.getUserId()) { ApiResponse.error(resp, "Số điện thoại đã tồn tại", 409); return; } }
            userDAO.save(user); ApiResponse.ok(resp, toMap(user), "Updated");
        } catch (NumberFormatException e) { ApiResponse.error(resp, "Invalid ID", 400); }
        catch (IllegalArgumentException e) { ApiResponse.error(resp, e.getMessage(), 400); }
        catch (PersistenceException e) { ApiResponse.error(resp, "Không thể lưu người dùng", 409); }
    }

    @Override protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8"); if (!checkAdmin(req, resp)) return;
        try { int id = id(req); if (userDAO.findById(id) == null) { ApiResponse.error(resp, "Not found", 404); return; } userDAO.delete(id); ApiResponse.ok(resp, null, "Deleted"); }
        catch (NumberFormatException e) { ApiResponse.error(resp, "Invalid ID", 400); }
        catch (PersistenceException e) { ApiResponse.error(resp, "Không thể xóa người dùng đang được sử dụng", 409); }
    }

    private int id(HttpServletRequest req) { String path = req.getPathInfo(); if (path == null || path.equals("/")) throw new NumberFormatException(); return Integer.parseInt(path.substring(1)); }
    private void apply(Map<String, Object> body, User user, boolean creating) {
        if (creating || body.containsKey("fullName")) { Object raw = body.get("fullName"); if (!(raw instanceof String value) || value.trim().length() < 2 || value.trim().length() > 100) throw new IllegalArgumentException("Họ tên phải từ 2 đến 100 ký tự"); user.setFullName(value.trim()); }
        if (creating || body.containsKey("email")) { Object raw = body.get("email"); if (!(raw instanceof String value) || !value.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new IllegalArgumentException("Email không hợp lệ"); user.setEmail(value.trim().toLowerCase()); }
        if (body.containsKey("phone")) { Object raw = body.get("phone"); if (raw != null && !(raw instanceof String)) throw new IllegalArgumentException("Số điện thoại không hợp lệ"); user.setPhone(raw == null ? null : ((String) raw).trim()); }
        if (creating || body.containsKey("roleName")) { Object raw = body.getOrDefault("roleName", "USER"); if (!(raw instanceof String name) || !ROLES.contains(name)) throw new IllegalArgumentException("Vai trò không hợp lệ"); user.setRole(name); }
        if (creating || body.containsKey("password")) { Object raw = body.get("password"); if (!(raw instanceof String password) || password.length() < 6 || password.length() > 72) throw new IllegalArgumentException("Mật khẩu phải từ 6 đến 72 ký tự"); user.setPasswordHash(PasswordUtil.hash(password)); }
    }
}
