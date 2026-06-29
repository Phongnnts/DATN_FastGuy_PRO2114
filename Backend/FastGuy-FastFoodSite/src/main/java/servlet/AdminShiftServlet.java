package servlet;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

import dao.WorkShiftDAO;
import entity.WorkShift;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;
import utils.JsonUtil;
import utils.JwtUtil;

@WebServlet("/api/admin/shifts/*")
public class AdminShiftServlet extends HttpServlet {
    private WorkShiftDAO workShiftDAO = new WorkShiftDAO();

    private boolean isAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return false;
        }
        String role = JwtUtil.getRole(authHeader.substring(7));
        if (!"ADMIN".equals(role)) { ApiResponse.error(resp, "Forbidden", 403); return false; }
        return true;
    }

    private Map<String, Object> toMap(WorkShift ws) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("id", ws.getShiftId());
        m.put("name", ws.getShiftName());
        m.put("startTime", ws.getStartTime() != null ? ws.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) : null);
        m.put("endTime", ws.getEndTime() != null ? ws.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")) : null);
        m.put("roleType", ws.getRoleType());
        m.put("staffCount", 0);
        return m;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!isAdmin(req, resp)) return;
        String path = req.getPathInfo();
        if (path != null && path.length() > 1) {
            try {
                int id = Integer.parseInt(path.substring(1));
                WorkShift ws = workShiftDAO.findById(id);
                if (ws == null) { ApiResponse.error(resp, "Not found", 404); return; }
                ApiResponse.ok(resp, toMap(ws));
                return;
            } catch (NumberFormatException e) {
                resp.sendError(404);
                return;
            }
        }
        ApiResponse.ok(resp, workShiftDAO.findAll().stream().map(this::toMap).collect(Collectors.toList()));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!isAdmin(req, resp)) return;
        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }

        WorkShift ws = new WorkShift();
        ws.setShiftName((String) body.get("name"));
        ws.setStartTime(LocalTime.parse((String) body.get("startTime")));
        ws.setEndTime(LocalTime.parse((String) body.get("endTime")));
        ws.setRoleType((String) body.getOrDefault("roleType", "STAFF"));
        workShiftDAO.save(ws);

        resp.setStatus(201);
        ApiResponse.ok(resp, toMap(ws), "Shift created");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!isAdmin(req, resp)) return;
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { resp.sendError(404); return; }
        try {
            int id = Integer.parseInt(path.substring(1));
            WorkShift ws = workShiftDAO.findById(id);
            if (ws == null) { ApiResponse.error(resp, "Not found", 404); return; }
            Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
            if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }
            if (body.containsKey("name")) ws.setShiftName((String) body.get("name"));
            if (body.containsKey("startTime")) ws.setStartTime(LocalTime.parse((String) body.get("startTime")));
            if (body.containsKey("endTime")) ws.setEndTime(LocalTime.parse((String) body.get("endTime")));
            if (body.containsKey("roleType")) ws.setRoleType((String) body.get("roleType"));
            workShiftDAO.save(ws);
            ApiResponse.ok(resp, toMap(ws), "Shift updated");
        } catch (NumberFormatException e) {
            resp.sendError(404);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!isAdmin(req, resp)) return;
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { resp.sendError(404); return; }
        try {
            int id = Integer.parseInt(path.substring(1));
            workShiftDAO.delete(id);
            ApiResponse.ok(resp, null, "Shift deleted");
        } catch (NumberFormatException e) {
            resp.sendError(404);
        }
    }
}
