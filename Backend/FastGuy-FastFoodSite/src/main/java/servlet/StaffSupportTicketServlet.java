package servlet;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.StaffShiftAccessService;
import service.SupportTicketService;
import utils.ApiResponse;
import utils.JsonUtil;
import utils.JwtUtil;

@WebServlet("/api/staff/support/*")
public class StaffSupportTicketServlet extends HttpServlet {
    private SupportTicketService supportTicketService = new SupportTicketService();
    private StaffShiftAccessService staffShiftAccessService = new StaffShiftAccessService();

    public static boolean hasRouteAccess(String method, boolean validIdentity, boolean checkedIn) {
        return validIdentity && ("GET".equals(method) || checkedIn);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int staffId = getStaffId(req, resp);
        if (staffId < 0 || !requireAccess(req, resp, staffId)) return;
        boolean all = "true".equals(req.getParameter("all")) || "/all".equals(req.getPathInfo());
        ApiResponse.ok(resp, supportTicketService.getForStaff(!all));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int staffId = getStaffId(req, resp);
        if (staffId < 0 || !requireAccess(req, resp, staffId)) return;
        try {
            String path = req.getPathInfo();
            if (path == null || path.length() < 2 || path.indexOf('/', 1) >= 0) throw new NumberFormatException();
            int ticketId = Integer.parseInt(path.substring(1));
            Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
            if (body == null || !(body.get("status") instanceof String) || (body.get("resolution") != null && !(body.get("resolution") instanceof String))) throw new IllegalArgumentException("Invalid data type");
            ApiResponse.ok(resp, supportTicketService.update(ticketId, staffId, (String) body.get("status"), (String) body.get("resolution")), "Ticket updated");
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid ticket ID", 400);
        } catch (SupportTicketService.OwnershipConflictException e) {
            ApiResponse.error(resp, e.getMessage(), 409);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), e.getMessage().equals("Ticket not found") ? 404 : 400);
        }
    }

    private boolean requireAccess(HttpServletRequest req, HttpServletResponse resp, int staffId) throws IOException {
        boolean validIdentity = staffShiftAccessService.hasValidStaffIdentity(staffId);
        boolean checkedIn = "GET".equals(req.getMethod()) || staffShiftAccessService.hasCheckedInShift(staffId);
        if (hasRouteAccess(req.getMethod(), validIdentity, checkedIn)) return true;
        ApiResponse.error(resp, validIdentity ? "Checked-in shift required" : "Forbidden", 403);
        return false;
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
