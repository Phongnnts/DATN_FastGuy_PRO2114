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
            Object rawOrderId = body.get("orderId");
            if (rawOrderId != null && !(rawOrderId instanceof Number)) throw new IllegalArgumentException("Invalid data type");
            if (!(body.get("subject") instanceof String) || !(body.get("category") instanceof String) || !(body.get("description") instanceof String)) throw new IllegalArgumentException("Invalid data type");
            Integer orderId = rawOrderId == null ? null : ((Number) rawOrderId).intValue();
            ApiResponse.ok(resp, supportTicketService.create(userId, orderId, (String) body.get("subject"), (String) body.get("category"), (String) body.get("description")), "Ticket created");
        } catch (ClassCastException | IllegalArgumentException e) {
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
