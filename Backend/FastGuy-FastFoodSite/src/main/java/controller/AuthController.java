package controller;

import dto.ApiResponse;
import dto.ChangePasswordRequest;
import dto.LoginRequest;
import dto.LoginResponse;
import dto.RegisterRequest;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AuthService;
import utils.JsonUtil;
import utils.RoleRequired;

import java.io.IOException;

@WebServlet("/api/auth/*")
public class AuthController extends HttpServlet {
    private final AuthService authService = new AuthService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getPathInfo();
        resp.setContentType("application/json; charset=UTF-8");

        try {
            if (path == null || path.equals("/login")) {
                LoginRequest loginReq = JsonUtil.fromJson(req.getReader(), LoginRequest.class);
                LoginResponse loginRes = authService.login(loginReq);
                writeJson(resp, ApiResponse.ok(loginRes));
            } else if (path.equals("/register")) {
                RegisterRequest registerReq = JsonUtil.fromJson(req.getReader(), RegisterRequest.class);
                LoginResponse loginRes = authService.register(registerReq);
                resp.setStatus(201);
                writeJson(resp, ApiResponse.ok(loginRes, "Đăng ký thành công"));
            } else {
                resp.sendError(404);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "USER");
        String path = req.getPathInfo();

        if (path != null && path.equals("/change-password")) {
            Long userId = (Long) req.getAttribute("userId");
            ChangePasswordRequest changeReq = JsonUtil.fromJson(req.getReader(), ChangePasswordRequest.class);
            authService.changePassword(userId, changeReq);
            resp.setContentType("application/json; charset=UTF-8");
            writeJson(resp, ApiResponse.ok(null, "Đổi mật khẩu thành công"));
        } else {
            resp.sendError(404);
        }
    }

    private void writeJson(HttpServletResponse resp, Object data) throws IOException {
        resp.getWriter().write(JsonUtil.toJson(data));
    }
}
