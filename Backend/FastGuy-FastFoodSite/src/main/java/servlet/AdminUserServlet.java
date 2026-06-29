package servlet;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dao.RoleDAO;
import dao.UserDAO;
import entity.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;
import utils.JsonUtil;
import utils.JwtUtil;

@WebServlet("/api/admin/users/*")
public class AdminUserServlet extends HttpServlet {
    private UserDAO userDAO = new UserDAO();
    private RoleDAO roleDAO = new RoleDAO();

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

    private Map<String, Object> toMap(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", u.getUserId());
        m.put("fullName", u.getFullName() != null ? u.getFullName() : "");
        m.put("email", u.getEmail() != null ? u.getEmail() : "");
        m.put("phone", u.getPhone() != null ? u.getPhone() : "");
        m.put("roleName", u.getRole() != null ? u.getRole().getRoleName() : "");
        return m;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            List<Map<String, Object>> list = userDAO.findAll().stream()
                    .map(this::toMap).collect(Collectors.toList());
            ApiResponse.ok(resp, list);
        } else {
            try {
                int id = Integer.parseInt(path.substring(1));
                User u = userDAO.findById(id);
                if (u == null) { ApiResponse.error(resp, "Not found", 404); return; }
                ApiResponse.ok(resp, toMap(u));
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

        String email = (String) body.get("email");
        if (userDAO.findByEmail(email) != null) {
            ApiResponse.error(resp, "Email already exists", 400);
            return;
        }
        String phone = (String) body.get("phone");
        if (phone != null && userDAO.findByPhone(phone) != null) {
            ApiResponse.error(resp, "Số điện thoại đã tồn tại", 400);
            return;
        }

        User u = new User();
        u.setFullName((String) body.get("fullName"));
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(utils.PasswordUtil.hash((String) body.get("password")));
        u.setRole(roleDAO.findByName((String) body.getOrDefault("roleName", "USER")));
        u.setStatus("ACTIVE");
        u.setCreatedAt(LocalDateTime.now());
        userDAO.save(u);

        resp.setStatus(201);
        ApiResponse.ok(resp, toMap(u), "Created");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { resp.sendError(404); return; }

        int id = Integer.parseInt(path.substring(1));
        User u = userDAO.findById(id);
        if (u == null) { ApiResponse.error(resp, "Not found", 404); return; }

        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }

        if (body.containsKey("fullName")) u.setFullName((String) body.get("fullName"));
        if (body.containsKey("email")) u.setEmail((String) body.get("email"));
        if (body.containsKey("phone")) {
            String newPhone = (String) body.get("phone");
            if (!newPhone.equals(u.getPhone()) && userDAO.findByPhone(newPhone) != null) {
                ApiResponse.error(resp, "Số điện thoại đã tồn tại", 400);
                return;
            }
            u.setPhone(newPhone);
        }
        if (body.containsKey("roleName")) {
            var role = roleDAO.findByName((String) body.get("roleName"));
            if (role != null) u.setRole(role);
        }
        if (body.containsKey("password")) {
            String pass = (String) body.get("password");
            if (!pass.isEmpty()) u.setPasswordHash(utils.PasswordUtil.hash(pass));
        }
        userDAO.save(u);
        ApiResponse.ok(resp, toMap(u), "Updated");
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { resp.sendError(404); return; }

        int id = Integer.parseInt(path.substring(1));
        userDAO.delete(id);
        ApiResponse.ok(resp, null, "Deleted");
    }
}
