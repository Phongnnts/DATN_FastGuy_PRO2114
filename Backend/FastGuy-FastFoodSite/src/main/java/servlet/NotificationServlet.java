package servlet;

import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.NotificationService;
import utils.ApiResponse;
import utils.JwtUtil;

@WebServlet("/api/notifications/*")
public class NotificationServlet extends HttpServlet {
    private NotificationService notificationService = new NotificationService();

    private String getToken(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return null;
        }
        return authHeader.substring(7);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String token = getToken(req, resp);
        if (token == null) return;
        int userId = JwtUtil.getUserId(token);
        String role = JwtUtil.getRole(token);
        if (userId < 0 || role == null) {
            ApiResponse.error(resp, "Invalid token", 401);
            return;
        }
        ApiResponse.ok(resp, notificationService.getForUser(userId, role));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String token = getToken(req, resp);
        if (token == null) return;
        int userId = JwtUtil.getUserId(token);
        String role = JwtUtil.getRole(token);
        if (userId < 0 || role == null) {
            ApiResponse.error(resp, "Invalid token", 401);
            return;
        }
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            ApiResponse.error(resp, "Missing notification ID", 400);
            return;
        }
        if ("/read-all".equals(path)) {
            notificationService.markAllRead(userId, role);
            ApiResponse.ok(resp, null, "Read all");
            return;
        }
        try {
            int id = Integer.parseInt(path.substring(1).replace("/read", ""));
            if (!notificationService.markRead(id, userId, role)) {
                ApiResponse.error(resp, "Not found", 404);
                return;
            }
            ApiResponse.ok(resp, null, "Read");
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid ID", 400);
        }
    }
}
