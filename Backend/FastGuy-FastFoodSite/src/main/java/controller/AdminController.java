package controller;

import dto.ApiResponse;
import entity.WorkShift;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AdminService;
import service.ScheduleService;
import service.WorkShiftService;
import utils.JsonUtil;
import utils.RoleRequired;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

@WebServlet("/api/admin/*")
public class AdminController extends HttpServlet {
    private final AdminService adminService = new AdminService();
    private final WorkShiftService workShiftService = new WorkShiftService();
    private final ScheduleService scheduleService = new ScheduleService();

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "ADMIN");
        resp.setContentType("application/json; charset=UTF-8");

        String path = req.getPathInfo();
        if (path != null && path.equals("/dashboard")) {
            writeJson(resp, ApiResponse.ok(adminService.getDashboardStats()));
        } else if (path != null && path.equals("/shifts")) {
            writeJson(resp, ApiResponse.ok(workShiftService.getAll()));
        } else if (path != null && path.equals("/schedules")) {
            String dateStr = req.getParameter("date");
            if (dateStr != null) {
                writeJson(resp, ApiResponse.ok(scheduleService.getByDate(LocalDate.parse(dateStr))));
            } else {
                writeJson(resp, ApiResponse.ok(scheduleService.getByDate(LocalDate.now())));
            }
        } else {
            writeJson(resp, ApiResponse.ok(null, "Admin API"));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "ADMIN");
        resp.setContentType("application/json; charset=UTF-8");
        resp.setStatus(201);

        String path = req.getPathInfo();
        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);

        if (path != null && path.equals("/shifts")) {
            WorkShift shift = new WorkShift();
            shift.setShiftName((String) body.get("shiftName"));
            shift.setStartTime(java.time.LocalTime.parse((String) body.get("startTime")));
            shift.setEndTime(java.time.LocalTime.parse((String) body.get("endTime")));
            shift.setRoleType((String) body.get("roleType"));
            writeJson(resp, ApiResponse.ok(workShiftService.create(shift), "Tạo ca làm việc thành công"));
        } else if (path != null && path.equals("/schedules")) {
            Long userId = Long.valueOf(body.get("userId").toString());
            Long shiftId = Long.valueOf(body.get("shiftId").toString());
            LocalDate workDate = LocalDate.parse((String) body.get("workDate"));
            Long assignedById = (Long) req.getAttribute("userId");
            String note = (String) body.get("note");
            writeJson(resp, ApiResponse.ok(scheduleService.create(userId, shiftId, workDate, assignedById, note), "Phân ca thành công"));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "ADMIN");
        resp.setContentType("application/json; charset=UTF-8");

        String path = req.getPathInfo();
        if (path == null) return;

        if (path.matches("/shifts/\\d+")) {
            Long shiftId = parseIdAt(path, 2);
            Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
            WorkShift shift = workShiftService.getById(shiftId);
            if (body.containsKey("shiftName")) shift.setShiftName((String) body.get("shiftName"));
            if (body.containsKey("startTime")) shift.setStartTime(java.time.LocalTime.parse((String) body.get("startTime")));
            if (body.containsKey("endTime")) shift.setEndTime(java.time.LocalTime.parse((String) body.get("endTime")));
            if (body.containsKey("roleType")) shift.setRoleType((String) body.get("roleType"));
            writeJson(resp, ApiResponse.ok(workShiftService.update(shiftId, shift), "Cập nhật ca làm việc thành công"));
        } else if (path.matches("/schedules/\\d+")) {
            Long scheduleId = parseIdAt(path, 2);
            Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
            Long shiftId = body.containsKey("shiftId") ? Long.valueOf(body.get("shiftId").toString()) : null;
            String note = (String) body.get("note");
            writeJson(resp, ApiResponse.ok(scheduleService.update(scheduleId, shiftId, note), "Cập nhật lịch làm việc thành công"));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "ADMIN");
        resp.setContentType("application/json; charset=UTF-8");

        String path = req.getPathInfo();
        if (path != null && path.matches("/shifts/\\d+")) {
            Long shiftId = parseIdAt(path, 2);
            workShiftService.delete(shiftId);
            writeJson(resp, ApiResponse.ok(null, "Xóa ca làm việc thành công"));
        }
    }

    private Long parseIdAt(String path, int index) {
        String[] parts = path.split("/");
        return Long.parseLong(parts[index]);
    }

    private void writeJson(HttpServletResponse resp, Object data) throws IOException {
        resp.getWriter().write(JsonUtil.toJson(data));
    }
}
