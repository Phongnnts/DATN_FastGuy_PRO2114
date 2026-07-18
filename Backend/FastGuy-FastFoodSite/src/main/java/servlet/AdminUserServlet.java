package servlet;

import dao.RoleDAO;
import dao.UserDAO;
import entity.Role;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@WebServlet("/api/admin/users/*")
public class AdminUserServlet extends HttpServlet {
    private static final Set<String> ROLES = Set.of("USER", "STAFF", "SHIPPER", "ADMIN");
    private final UserDAO userDAO = new UserDAO();
    private final RoleDAO roleDAO = new RoleDAO();

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
        result.put("roleName", user.getRole() == null ? "" : user.getRole().getRoleName()); result.put("status", user.getStatus()); result.put("loyaltyPoints", user.getLoyaltyPoints());
        return result;
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8"); if (!checkAdmin(req, resp)) return;
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { List<Map<String, Object>> list = userDAO.findAll().stream().map(this::toMap).collect(Collectors.toList()); ApiResponse.ok(resp, list); return; }
        try { User user = userDAO.findById(Integer.parseInt(path.substring(1))); if (user == null) { ApiResponse.error(resp, "Not found", 404); return; } ApiResponse.ok(resp, toMap(user)); }
        catch (NumberFormatException e) { ApiResponse.error(resp, "Invalid ID", 400); }
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
        if (creating || body.containsKey("roleName")) { Object raw = body.getOrDefault("roleName", "USER"); if (!(raw instanceof String name) || !ROLES.contains(name)) throw new IllegalArgumentException("Vai trò không hợp lệ"); Role role = roleDAO.findByName(name); if (role == null) throw new IllegalArgumentException("Vai trò không tồn tại"); user.setRole(role); }
        if (creating || body.containsKey("password")) { Object raw = body.get("password"); if (!(raw instanceof String password) || password.length() < 6 || password.length() > 72) throw new IllegalArgumentException("Mật khẩu phải từ 6 đến 72 ký tự"); user.setPasswordHash(PasswordUtil.hash(password)); }
    }
}
