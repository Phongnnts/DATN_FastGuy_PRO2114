package servlet;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AdminService;
import utils.ApiResponse;
import utils.JwtUtil;

@WebServlet("/api/admin/*")
public class AdminServlet extends HttpServlet {
    private AdminService adminService = new AdminService();

    private boolean checkAuth(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAuth(req, resp)) return;

        String path = req.getPathInfo();
        String period = req.getParameter("period");

        if (path == null || path.equals("/") || path.equals("/dashboard")) {
            var data = period != null ? adminService.getDashboardWithPeriod(period) : adminService.getDashboard();
            ApiResponse.ok(resp, data);
        } else if (path.equals("/reports/full")) {
            String startDate = req.getParameter("startDate");
            String endDate = req.getParameter("endDate");
            try {
                ApiResponse.ok(resp, adminService.getFullReport(period, startDate, endDate));
            } catch (IllegalArgumentException e) {
                ApiResponse.error(resp, e.getMessage(), 400);
            }
        } else {
            ApiResponse.error(resp, "Not found", 404);
        }
    }
}
