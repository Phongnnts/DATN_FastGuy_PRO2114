package servlet;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import dao.ScheduleDAO;
import dao.UserDAO;
import dao.WorkShiftDAO;
import entity.Schedule;
import entity.User;
import entity.WorkShift;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;
import utils.JsonUtil;
import utils.JwtUtil;

@WebServlet("/api/admin/schedules/*")
public class AdminScheduleServlet extends HttpServlet {
    private ScheduleDAO scheduleDAO = new ScheduleDAO();
    private UserDAO userDAO = new UserDAO();
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

    private int getAdminId(HttpServletRequest req) {
        String authHeader = req.getHeader("Authorization");
        return JwtUtil.getUserId(authHeader.substring(7));
    }

    private Map<String, Object> toMap(Schedule s) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("id", s.getScheduleId());
        m.put("userId", s.getUser().getUserId());
        m.put("userName", s.getUser().getFullName());
        m.put("shiftId", s.getShift().getShiftId());
        m.put("shiftName", s.getShift().getShiftName());
        m.put("date", s.getWorkDate() != null ? s.getWorkDate().toString() : null);
        m.put("status", s.getStatus());
        m.put("checkedInAt", s.getCheckedInAt() != null ? s.getCheckedInAt().toString() : null);
        m.put("checkedOutAt", s.getCheckedOutAt() != null ? s.getCheckedOutAt().toString() : null);
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
                Schedule s = scheduleDAO.findById(id);
                if (s == null) { ApiResponse.error(resp, "Not found", 404); return; }
                ApiResponse.ok(resp, toMap(s));
                return;
            } catch (NumberFormatException e) {
                resp.sendError(404);
                return;
            }
        }
        ApiResponse.ok(resp, scheduleDAO.findAll().stream().map(this::toMap).collect(Collectors.toList()));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!isAdmin(req, resp)) return;
        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }

        User user = userDAO.findById(((Number) body.get("userId")).intValue());
        WorkShift shift = workShiftDAO.findById(((Number) body.get("shiftId")).intValue());
        if (user == null || shift == null) { ApiResponse.error(resp, "Invalid user or shift", 400); return; }

        Schedule s = new Schedule();
        s.setUser(user);
        s.setShift(shift);
        s.setWorkDate(LocalDate.parse((String) body.get("date")));
        s.setStatus((String) body.getOrDefault("status", "PENDING"));
        s.setAssignedBy(userDAO.findById(getAdminId(req)));
        s.setCreatedAt(LocalDateTime.now());
        scheduleDAO.save(s);

        resp.setStatus(201);
        ApiResponse.ok(resp, toMap(s), "Schedule created");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!isAdmin(req, resp)) return;
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { resp.sendError(404); return; }
        try {
            int id = Integer.parseInt(path.substring(1));
            Schedule s = scheduleDAO.findById(id);
            if (s == null) { ApiResponse.error(resp, "Not found", 404); return; }
            Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
            if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }
            if (body.containsKey("status")) s.setStatus((String) body.get("status"));
            if (body.containsKey("shiftId"))
                s.setShift(workShiftDAO.findById(((Number) body.get("shiftId")).intValue()));
            if (body.containsKey("date"))
                s.setWorkDate(LocalDate.parse((String) body.get("date")));
            scheduleDAO.save(s);
            ApiResponse.ok(resp, toMap(s), "Updated");
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
            scheduleDAO.delete(id);
            ApiResponse.ok(resp, null, "Deleted");
        } catch (NumberFormatException e) {
            resp.sendError(404);
        }
    }
}
