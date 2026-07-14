package servlet;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.WorkShiftService;
import utils.ApiResponse;
import utils.JwtUtil;

@WebServlet("/api/admin/shifts/*")
public class AdminShiftServlet extends HttpServlet {
    private WorkShiftService workShiftService = new WorkShiftService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!admin(req, resp)) return;
        resp.setContentType("application/json;charset=UTF-8");
        ApiResponse.ok(resp, workShiftService.list(null));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!admin(req, resp)) return;
        resp.setContentType("application/json;charset=UTF-8");
        try {
            ApiResponse.ok(resp, workShiftService.create(utils.JsonUtil.fromJson(req.getReader(), Map.class)), "Shift created");
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!admin(req, resp)) return;
        resp.setContentType("application/json;charset=UTF-8");
        try {
            String path = req.getPathInfo();
            int shiftId = Integer.parseInt(path.substring(1));
            ApiResponse.ok(resp, workShiftService.update(shiftId, utils.JsonUtil.fromJson(req.getReader(), Map.class)), "Shift updated");
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid shift ID", 400);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!admin(req, resp)) return;
        resp.setContentType("application/json;charset=UTF-8");
        try {
            String path = req.getPathInfo();
            int shiftId = Integer.parseInt(path.substring(1));
            workShiftService.delete(shiftId);
            ApiResponse.ok(resp, null, "Shift deleted");
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid shift ID", 400);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 409);
        }
    }

    private boolean admin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { ApiResponse.error(resp, "Missing token", 401); return false; }
        if (!"ADMIN".equals(JwtUtil.getRole(header.substring(7)))) { ApiResponse.error(resp, "Forbidden", 403); return false; }
        return true;
    }
}
