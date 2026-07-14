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

@WebServlet("/api/staff/support/*")
public class StaffSupportTicketServlet extends HttpServlet {
    private SupportTicketService supportTicketService = new SupportTicketService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (getStaffId(req, resp) < 0) return;
        boolean all = "true".equals(req.getParameter("all")) || "/all".equals(req.getPathInfo());
        ApiResponse.ok(resp, supportTicketService.getForStaff(!all));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int staffId = getStaffId(req, resp);
        if (staffId < 0) return;
        try {
            String path = req.getPathInfo();
            if (path == null || path.length() < 2 || path.indexOf('/', 1) >= 0) throw new NumberFormatException();
            int ticketId = Integer.parseInt(path.substring(1));
            Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
            if (body == null) throw new IllegalArgumentException("Invalid data");
            ApiResponse.ok(resp, supportTicketService.update(ticketId, staffId, (String) body.get("status"), (String) body.get("resolution")), "Ticket updated");
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid ticket ID", 400);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), e.getMessage().equals("Ticket not found") ? 404 : 400);
        }
    }

    private int getStaffId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return -1;
        }
        String token = header.substring(7);
        if (!"STAFF".equals(JwtUtil.getRole(token))) {
            ApiResponse.error(resp, "Forbidden", 403);
            return -1;
        }
        int userId = JwtUtil.getUserId(token);
        if (userId < 0) ApiResponse.error(resp, "Invalid token", 401);
        return userId;
    }
}
