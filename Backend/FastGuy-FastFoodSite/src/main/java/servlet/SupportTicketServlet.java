package servlet;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.SupportTicketService;
import utils.ApiResponse;
import utils.JsonUtil;
import utils.JwtUtil;

@WebServlet("/api/support/*")
public class SupportTicketServlet extends HttpServlet {
    private SupportTicketService supportTicketService = new SupportTicketService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId >= 0) ApiResponse.ok(resp, supportTicketService.getForUser(userId));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId < 0) return;
        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) {
            ApiResponse.error(resp, "Invalid data", 400);
            return;
        }
        try {
            String subject = body.get("subject") instanceof String ? ((String) body.get("subject")).trim() : "";
            String category = body.get("category") instanceof String ? ((String) body.get("category")).trim() : "";
            String description = body.get("description") instanceof String ? ((String) body.get("description")).trim() : "";
            Integer orderId = body.get("orderId") instanceof Number ? ((Number) body.get("orderId")).intValue() : null;

            if (subject.isEmpty()) {
                ApiResponse.error(resp, "Tieu de khong duoc de trong", 400);
                return;
            }
            if (subject.length() > 255) {
                ApiResponse.error(resp, "Tieu de toi da 255 ky tu", 400);
                return;
            }
            if (description.isEmpty()) {
                ApiResponse.error(resp, "Mo ta khong duoc de trong", 400);
                return;
            }
            if (description.length() > 2000) {
                ApiResponse.error(resp, "Mo ta toi da 2000 ky tu", 400);
                return;
            }
            if (!java.util.Set.of("MISSING_ITEM", "COLD_FOOD", "WRONG_ITEM", "LATE_DELIVERY", "OTHER").contains(category)) {
                ApiResponse.error(resp, "Loai van de khong hop le", 400);
                return;
            }

            ApiResponse.ok(resp, supportTicketService.create(userId, orderId, subject, category, description), "Ticket created");
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        }
    }

    private int getUserId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return -1;
        }
        String token = header.substring(7);
        if (!"USER".equals(JwtUtil.getRole(token))) {
            ApiResponse.error(resp, "Forbidden", 403);
            return -1;
        }
        int userId = JwtUtil.getUserId(token);
        if (userId < 0) ApiResponse.error(resp, "Invalid token", 401);
        return userId;
    }
}
