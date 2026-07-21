package servlet;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AuthService;
import service.CartService;
import service.PasswordResetService;
import utils.ApiResponse;
import utils.JsonUtil;
import utils.JwtUtil;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)(3|5|7|8|9)[0-9]{8}$");
    private AuthService authService = new AuthService();
    private CartService cartService = new CartService();
    private PasswordResetService passwordResetService = new PasswordResetService();

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
            } else if (path.equals("/forgot-password")) {
                handleForgotPassword(body, resp);
            } else if (path.equals("/reset-password")) {
                handleResetPassword(body, resp);
            } else if (path.equals("/cart/migrate")) {
                handleCartMigrate(req, body, resp);
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
            } else if (path.equals("/password")) {
                handleChangePassword(req, resp);
            } else {
                resp.sendError(404);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.write(resp, ApiResponse.error("Lỗi máy chủ"));
        }
    }

    private void handleLogin(Map<String, Object> body, HttpServletResponse resp) throws IOException {
        String login = trim((String) body.get("login"));
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

        String token = JwtUtil.generate(user.getUserId(), user.getRole());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getUserId());
        data.put("role", user.getRole());
        data.put("fullName", user.getFullName());
        data.put("avatarUrl", user.getAvatarUrl());

        JsonUtil.write(resp, ApiResponse.ok(data, "Đăng nhập thành công"));
    }

    private void handleRegister(Map<String, Object> body, HttpServletResponse resp) throws IOException {
        String fullName = trim((String) body.get("fullName"));
        String phone = trim((String) body.get("phone"));
        String email = trim((String) body.get("email"));
        String password = (String) body.get("password");

        String validationError = validateAccount(fullName, phone, email, password, true);
        if (validationError != null) {
            JsonUtil.write(resp, ApiResponse.error(validationError));
            return;
        }

        User user = authService.register(fullName, phone, email, password);
        if (user == null) {
            JsonUtil.write(resp, ApiResponse.error("Số điện thoại hoặc email đã tồn tại"));
            return;
        }

        String token = JwtUtil.generate(user.getUserId(), user.getRole());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getUserId());
        data.put("role", user.getRole());
        data.put("fullName", user.getFullName());

        resp.setStatus(201);
        JsonUtil.write(resp, ApiResponse.ok(data, "Đăng ký thành công"));
    }

    private void handleLogout(HttpServletResponse resp) throws IOException {
        JsonUtil.write(resp, ApiResponse.ok((Object) null, "Đăng xuất thành công"));
    }

    private void handleForgotPassword(Map<String, Object> body, HttpServletResponse resp) throws IOException {
        String email = trim((String) body.get("email"));
        if (email != null && EMAIL_PATTERN.matcher(email).matches()) {
            try {
                passwordResetService.request(email);
            } catch (IllegalStateException ignored) {
            }
        }
        JsonUtil.write(resp, ApiResponse.ok((Object) null, "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi."));
    }

    private void handleResetPassword(Map<String, Object> body, HttpServletResponse resp) throws IOException {
        String token = trim((String) body.get("token"));
        String password = (String) body.get("password");
        if (token == null || token.length() < 32 || password == null) {
            ApiResponse.error(resp, "Liên kết hoặc mật khẩu không hợp lệ", 400);
            return;
        }
        try {
            passwordResetService.reset(token, password);
            JsonUtil.write(resp, ApiResponse.ok((Object) null, "Đặt lại mật khẩu thành công"));
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        }
    }

    private void handleChangePassword(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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

        String currentPassword = (String) body.get("currentPassword");
        String newPassword = (String) body.get("newPassword");

        if (currentPassword == null || currentPassword.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            JsonUtil.write(resp, ApiResponse.error("Vui lòng nhập đầy đủ mật khẩu"));
            return;
        }

        try {
            authService.changePassword(userId, currentPassword, newPassword);
            JsonUtil.write(resp, ApiResponse.ok((Object) null, "Đổi mật khẩu thành công"));
        } catch (IllegalArgumentException e) {
            JsonUtil.write(resp, ApiResponse.error(e.getMessage()));
        }
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
        data.put("role", user.getRole());
        data.put("status", user.getStatus());
        data.put("loyaltyPoints", user.getLoyaltyPoints());
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

        String fullName = trim((String) body.get("fullName"));
        String phone = trim((String) body.get("phone"));
        String email = trim((String) body.get("email"));
        String validationError = validateAccount(fullName, phone, email, null, false);
        if (validationError != null) {
            JsonUtil.write(resp, ApiResponse.error(validationError));
            return;
        }
        body.put("fullName", fullName);
        body.put("phone", phone);
        body.put("email", email);

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

    private String validateAccount(String fullName, String phone, String email, String password, boolean requirePassword) {
        if (fullName == null || fullName.length() < 2 || fullName.length() > 100) return "Họ tên phải từ 2 đến 100 ký tự";
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) return "Số điện thoại không hợp lệ (VD: 0912345678 hoặc +84912345678)";
        if (email != null && !email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) return "Email không hợp lệ";
        if (requirePassword) {
            if (password == null || password.length() < 8 || password.length() > 72) return "Mật khẩu phải từ 8 đến 72 ký tự";
            if (!password.matches(".*[a-zA-Z].*") || !password.matches(".*[0-9].*"))
                return "Mật khẩu phải có ít nhất 1 chữ và 1 số";
        }
        return null;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
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

    @SuppressWarnings("unchecked")
    private void handleCartMigrate(HttpServletRequest req, Map<String, Object> body, HttpServletResponse resp) throws IOException {
        int userId = getUserIdFromToken(req);
        if (userId < 0) { JsonUtil.write(resp, ApiResponse.error("Unauthorized")); return; }

        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        if (items == null || items.isEmpty()) { ApiResponse.ok(resp, (Object) null); return; }

        List<Object> migrated = new ArrayList<>();
        List<Map<String, Object>> rejected = new ArrayList<>();
        User user = new User() {{ setUserId(userId); }};

        for (Map<String, Object> item : items) {
            int productId = ((Number) item.get("productId")).intValue();
            int variantId = ((Number) item.get("variantId")).intValue();
            int quantity = item.get("quantity") instanceof Number ? ((Number) item.get("quantity")).intValue() : 1;
            List<Integer> modifierOptionIds = null;
            if (item.get("modifierOptionIds") instanceof List) {
                modifierOptionIds = ((List<?>) item.get("modifierOptionIds")).stream()
                        .map(id -> ((Number) id).intValue()).collect(java.util.stream.Collectors.toList());
            }
            boolean ok = cartService.addItem(user, productId, variantId, quantity, modifierOptionIds);
            if (ok) {
                migrated.add(productId);
            } else {
                rejected.add(Map.of("productId", productId, "variantId", variantId, "message", "Cannot migrate item"));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("migrated", migrated);
        result.put("rejected", rejected);
        ApiResponse.ok(resp, (Object) result);
    }
}
