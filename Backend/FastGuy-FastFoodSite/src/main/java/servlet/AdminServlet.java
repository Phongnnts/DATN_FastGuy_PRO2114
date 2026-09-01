package servlet;

import java.io.IOException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AdminService;
import utils.ApiResponse;

@WebServlet("/api/admin/*")
public class AdminServlet extends HttpServlet {
    private final AdminService adminService;
    private final AdminApiAuth.TokenReader tokenReader;

    public AdminServlet() {
        this(new AdminService(), AdminApiAuth.jwt());
    }

    AdminServlet(AdminService adminService, AdminApiAuth.TokenReader tokenReader) {
        this.adminService = adminService;
        this.tokenReader = tokenReader;
    }

    private boolean checkAuth(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        return AdminApiAuth.require(req, resp, tokenReader) >= 0;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAuth(req, resp)) return;

        String path = req.getPathInfo();
        String period = req.getParameter("period");

        if (path == null || path.equals("/") || path.equals("/dashboard")) {
            Object data;
            try {
                data = period != null ? adminService.getDashboardWithPeriod(period) : adminService.getDashboard();
            } catch (RuntimeException e) {
                ApiResponse.error(resp, "Internal server error", 500);
                return;
            }
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
