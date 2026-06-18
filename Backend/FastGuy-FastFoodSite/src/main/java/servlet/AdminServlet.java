package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AdminService;
import utils.ApiResponse;
import utils.JwtUtil;

import java.io.IOException;

@WebServlet("/api/admin/dashboard")
public class AdminServlet extends HttpServlet {
    private AdminService adminService = new AdminService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return;
        }

        String token = authHeader.substring(7);
        String role = JwtUtil.getRole(token);
        if (!"ADMIN".equals(role)) {
            ApiResponse.error(resp, "Forbidden", 403);
            return;
        }

        var data = adminService.getDashboard();
        ApiResponse.ok(resp, data);
    }
}
