package servlet;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import dao.ScheduleDAO;
import entity.Schedule;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;
import utils.JwtUtil;

@WebServlet("/api/staff/shifts/*")
public class StaffShiftServlet extends HttpServlet {
    private ScheduleDAO scheduleDAO = new ScheduleDAO();

    private int getStaffId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return -1;
        }
        String role = JwtUtil.getRole(authHeader.substring(7));
        if (!"STAFF".equals(role)) { ApiResponse.error(resp, "Forbidden", 403); return -1; }
        return JwtUtil.getUserId(authHeader.substring(7));
    }

    private Map<String, Object> toMap(Schedule s) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("id", s.getScheduleId());
        m.put("date", s.getWorkDate() != null ? s.getWorkDate().toString() : null);
        m.put("shift", s.getShift() != null ? s.getShift().getShiftName() : "");
        m.put("shiftName", s.getShift() != null ? s.getShift().getShiftName() : "");
        m.put("status", s.getStatus());
        m.put("checkedInAt", s.getCheckedInAt() != null ? s.getCheckedInAt().toString() : null);
        m.put("checkedOutAt", s.getCheckedOutAt() != null ? s.getCheckedOutAt().toString() : null);
        return m;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int staffId = getStaffId(req, resp);
        if (staffId < 0) return;
        ApiResponse.ok(resp, scheduleDAO.findByUserId(staffId).stream().map(this::toMap).collect(Collectors.toList()));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int staffId = getStaffId(req, resp);
        if (staffId < 0) return;

        String path = req.getPathInfo();
        if (path != null && path.contains("check-in")) {
            Schedule s = scheduleDAO.findByUserAndDate(staffId, LocalDate.now());
            if (s == null) {
                ApiResponse.error(resp, "No schedule for today", 400);
                return;
            }
            s.setCheckedInAt(LocalDateTime.now());
            s.setStatus("CHECKED_IN");
            scheduleDAO.save(s);
            ApiResponse.ok(resp, toMap(s), "Check-in OK");
        } else if (path != null && path.contains("check-out")) {
            Schedule s = scheduleDAO.findByUserAndDate(staffId, LocalDate.now());
            if (s == null) {
                ApiResponse.error(resp, "No schedule for today", 400);
                return;
            }
            s.setCheckedOutAt(LocalDateTime.now());
            s.setStatus("CHECKED_OUT");
            scheduleDAO.save(s);
            ApiResponse.ok(resp, toMap(s), "Check-out OK");
        } else {
            ApiResponse.ok(resp, null, "OK");
        }
    }
}
