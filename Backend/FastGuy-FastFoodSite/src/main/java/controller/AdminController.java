package controller;

import dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AdminService;
import utils.JsonUtil;
import utils.RoleRequired;

import java.io.IOException;

@WebServlet("/api/admin/*")
public class AdminController extends HttpServlet {
    private final AdminService adminService = new AdminService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "ADMIN");
        resp.setContentType("application/json; charset=UTF-8");

        String path = req.getPathInfo();
        if (path != null && path.equals("/dashboard")) {
            writeJson(resp, ApiResponse.ok(adminService.getDashboardStats()));
        } else {
            writeJson(resp, ApiResponse.ok(null, "Admin API"));
        }
    }

    private void writeJson(HttpServletResponse resp, Object data) throws IOException {
        resp.getWriter().write(JsonUtil.toJson(data));
    }
}
