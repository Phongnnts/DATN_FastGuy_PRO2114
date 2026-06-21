package servlet;

import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.StaffService;
import utils.ApiResponse;
import utils.JwtUtil;

@WebServlet("/api/staff/dashboard")
public class StaffDashboardServlet extends HttpServlet {
    private StaffService staffService = new StaffService();

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
        if (!"STAFF".equals(role)) {
            ApiResponse.error(resp, "Forbidden", 403);
            return;
        }

        int userId = JwtUtil.getUserId(token);
        var data = staffService.getDashboard(userId);
        ApiResponse.ok(resp, data);
    }
}
