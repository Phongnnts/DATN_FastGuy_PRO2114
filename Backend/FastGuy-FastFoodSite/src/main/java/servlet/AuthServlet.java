package servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AuthService;
import utils.ApiResponse;
import utils.JsonUtil;
import utils.JwtUtil;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {
    private AuthService authService = new AuthService();

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        setCors(resp);
        resp.setStatus(200);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        setCors(resp);

        String path = req.getPathInfo();

        try {
            if (path == null || path.equals("/profile")) {
                handleGetProfile(req, resp);
            } else {
                resp.sendError(404);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.write(resp, ApiResponse.error("Lỗi máy chủ"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        setCors(resp);

        String path = req.getPathInfo();

        try {
            if (path != null && path.equals("/logout")) {
                handleLogout(resp);
                return;
            }

            Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
            if (body == null) {
                JsonUtil.write(resp, ApiResponse.error("Dữ liệu không hợp lệ"));
                return;
            }

            if (path == null || path.equals("/login")) {
                handleLogin(body, resp);
            } else if (path.equals("/register")) {
                handleRegister(body, resp);
            } else {
                resp.sendError(404);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.write(resp, ApiResponse.error("Lỗi máy chủ"));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        setCors(resp);

        String path = req.getPathInfo();

        try {
            if (path.equals("/profile")) {
                handleUpdateProfile(req, resp);
            } else {
                resp.sendError(404);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.write(resp, ApiResponse.error("Lỗi máy chủ"));
        }
    }

    private void handleLogin(Map<String, Object> body, HttpServletResponse resp) throws IOException {
        String login = (String) body.get("login");
        String password = (String) body.get("password");

        if (login == null || password == null) {
            JsonUtil.write(resp, ApiResponse.error("Vui lòng nhập tài khoản và mật khẩu"));
            return;
        }

        User user = authService.login(login, password);
        if (user == null) {
            JsonUtil.write(resp, ApiResponse.error("Sai tài khoản hoặc mật khẩu"));
            return;
        }

        String token = JwtUtil.generate(user.getUserId(), user.getRole().getRoleName());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getUserId());
        data.put("role", user.getRole().getRoleName());
        data.put("fullName", user.getFullName());
        data.put("avatarUrl", user.getAvatarUrl());

        JsonUtil.write(resp, ApiResponse.ok(data, "Đăng nhập thành công"));
    }

    private void handleRegister(Map<String, Object> body, HttpServletResponse resp) throws IOException {
        String fullName = (String) body.get("fullName");
        String phone = (String) body.get("phone");
        String email = (String) body.get("email");
        String password = (String) body.get("password");

        if (fullName == null || phone == null || password == null) {
            JsonUtil.write(resp, ApiResponse.error("Vui lòng nhập đầy đủ thông tin"));
            return;
        }

        User user = authService.register(fullName, phone, email, password);
        if (user == null) {
            JsonUtil.write(resp, ApiResponse.error("Số điện thoại hoặc email đã tồn tại"));
            return;
        }

        String token = JwtUtil.generate(user.getUserId(), user.getRole().getRoleName());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getUserId());
        data.put("role", user.getRole().getRoleName());
        data.put("fullName", user.getFullName());

        resp.setStatus(201);
        JsonUtil.write(resp, ApiResponse.ok(data, "Đăng ký thành công"));
    }

    private void handleLogout(HttpServletResponse resp) throws IOException {
        JsonUtil.write(resp, ApiResponse.ok(null, "Đăng xuất thành công"));
    }

    private void handleGetProfile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int userId = getUserIdFromToken(req);
        if (userId == -1) {
            ApiResponse.error(resp, "Unauthorized", 401);
            return;
        }

        User user = authService.getProfile(userId);
        if (user == null) {
            ApiResponse.error(resp, "Không tìm thấy người dùng", 404);
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("fullName", user.getFullName());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone());
        data.put("avatarUrl", user.getAvatarUrl());
        data.put("role", user.getRole().getRoleName());
        data.put("status", user.getStatus());
        data.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);

        JsonUtil.write(resp, ApiResponse.ok(data));
    }

    private void handleUpdateProfile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int userId = getUserIdFromToken(req);
        if (userId == -1) {
            ApiResponse.error(resp, "Unauthorized", 401);
            return;
        }

        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) {
            JsonUtil.write(resp, ApiResponse.error("Dữ liệu không hợp lệ"));
            return;
        }

        User user = authService.updateProfile(userId, body);
        if (user == null) {
            JsonUtil.write(resp, ApiResponse.error("Số điện thoại hoặc email đã tồn tại"));
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("fullName", user.getFullName());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone());
        data.put("avatarUrl", user.getAvatarUrl());

        JsonUtil.write(resp, ApiResponse.ok(data, "Cập nhật thành công"));
    }

    private int getUserIdFromToken(HttpServletRequest req) {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return -1;
        }
        String token = authHeader.substring(7);
        return JwtUtil.getUserId(token);
    }

    private void setCors(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}
