package servlet;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

import entity.WorkShift;
import jakarta.persistence.EntityManager;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.WorkShiftService;
import utils.ApiResponse;
import utils.DatabaseUtil;
import utils.JwtUtil;

@WebServlet("/api/shifts/*")
public class ShiftServlet extends HttpServlet {
    private WorkShiftService workShiftService = new WorkShiftService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int userId = worker(req, resp);
        if (userId < 0) return;
        resp.setContentType("application/json;charset=UTF-8");
        if ("/mine".equals(req.getPathInfo())) ApiResponse.ok(resp, workShiftService.list(userId, null, null, null));
        else if ("/has-today".equals(req.getPathInfo())) {
            EntityManager em = DatabaseUtil.getEntityManager();
            try {
                Long count = em.createQuery("SELECT COUNT(ws) FROM WorkShift ws WHERE ws.user.userId = :uid AND ws.shiftDate = :today", Long.class)
                        .setParameter("uid", userId).setParameter("today", LocalDate.now()).getSingleResult();
                Map<String, Object> result = new java.util.HashMap<>();
                result.put("hasShift", count > 0);
                ApiResponse.ok(resp, result);
            } finally { em.close(); }
        }
        else ApiResponse.error(resp, "Not found", 404);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int userId = worker(req, resp);
        if (userId < 0) return;
        resp.setContentType("application/json;charset=UTF-8");
        try {
            String[] parts = req.getPathInfo().split("/");
            if (parts.length != 3 || (!"check-in".equals(parts[2]) && !"check-out".equals(parts[2]))) { ApiResponse.error(resp, "Not found", 404); return; }
            ApiResponse.ok(resp, workShiftService.check(Integer.parseInt(parts[1]), userId, "check-in".equals(parts[2])));
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid shift ID", 400);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        }
    }

    private int worker(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { ApiResponse.error(resp, "Missing token", 401); return -1; }
        String token = header.substring(7);
        String role = JwtUtil.getRole(token);
        if (!"STAFF".equals(role) && !"SHIPPER".equals(role)) { ApiResponse.error(resp, "Forbidden", 403); return -1; }
        return JwtUtil.getUserId(token);
    }
}
