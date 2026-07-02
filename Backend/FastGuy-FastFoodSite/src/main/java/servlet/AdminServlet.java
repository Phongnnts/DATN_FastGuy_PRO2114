package servlet;

import java.io.IOException;

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
        } else if (path.equals("/reports/revenue")) {
            var data = period != null ? adminService.getDashboardWithPeriod(period) : adminService.getDashboard();
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("revenueByMonth", data.get("revenueByMonth"));
            result.put("periodRevenue", data.get("periodRevenue"));
            result.put("periodOrders", data.get("periodOrders"));
            result.put("totalRevenue", data.get("totalRevenue"));
            result.put("revenueToday", data.get("revenueToday"));
            result.put("ordersToday", data.get("ordersToday"));
            ApiResponse.ok(resp, result);
        } else if (path.equals("/reports/top-products")) {
            var data = period != null ? adminService.getDashboardWithPeriod(period) : adminService.getDashboard();
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("topProducts", data.get("topProducts"));
            result.put("periodTopProducts", data.get("periodTopProducts"));
            ApiResponse.ok(resp, result);
        } else if (path.equals("/orders")) {
            ApiResponse.ok(resp, new AdminOrderServlet().getOrdersData());
        } else {
            ApiResponse.error(resp, "Not found", 404);
        }
    }
}
