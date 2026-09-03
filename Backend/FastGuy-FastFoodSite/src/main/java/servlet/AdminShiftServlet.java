package servlet;

import jakarta.persistence.PersistenceException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import service.StaffPayRateService;
import service.WorkShiftService;
import utils.ApiResponse;
import utils.JwtUtil;
import utils.PrivilegedAuth;

@WebServlet("/api/admin/shifts/*")
public class AdminShiftServlet extends HttpServlet {

    private WorkShiftService workShiftService = new WorkShiftService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
        if (!admin(req, resp)) return;
        resp.setContentType("application/json;charset=UTF-8");
        try {
            if ("/week".equals(req.getPathInfo())) {
                ApiResponse.ok(
                    resp,
                    workShiftService.week(req.getParameter("weekStart"), null)
                );
                return;
            }
            if ("/monitoring".equals(req.getPathInfo())) {
                ApiResponse.ok(resp, workShiftService.monitoring());
                return;
            }
            if ("/attendance".equals(req.getPathInfo())) {
                String value = req.getParameter("userId");
                Integer userId =
                    value == null || value.isBlank()
                        ? null
                        : Integer.parseInt(value);
                ApiResponse.ok(
                    resp,
                    workShiftService.attendance(
                        req.getParameter("month"),
                        userId,
                        req.getParameter("status")
                    )
                );
                return;
            }
            String userIdParam = req.getParameter("userId");
            Integer userId =
                userIdParam != null && !userIdParam.isBlank()
                    ? Integer.parseInt(userIdParam)
                    : null;
            ApiResponse.ok(
                resp,
                workShiftService.list(
                    userId,
                    req.getParameter("role"),
                    req.getParameter("fromDate"),
                    req.getParameter("toDate")
                )
            );
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid userId", 400);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
        if (!admin(req, resp)) return;
        resp.setContentType("application/json;charset=UTF-8");
        try {
            ApiResponse.ok(
                resp,
                workShiftService.create(
                    utils.JsonUtil.fromJson(req.getReader(), Map.class)
                ),
                "Shift created"
            );
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        } catch (PersistenceException e) {
            ApiResponse.error(resp, "Không thể lưu ca làm việc", 409);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
        if (!admin(req, resp)) return;
        resp.setContentType("application/json;charset=UTF-8");
        try {
            String path = req.getPathInfo();
            if ("/week".equals(path)) {
                ApiResponse.ok(
                    resp,
                    workShiftService.replaceWeek(
                        utils.JsonUtil.fromJson(req.getReader(), Map.class)
                    ),
                    "Weekly schedule replaced"
                );
                return;
            }
            if (path != null && path.matches("/\\d+/attendance-approval")) {
                int shiftId = Integer.parseInt(path.split("/")[1]);
                int adminId = JwtUtil.getUserId(
                    req.getHeader("Authorization").substring(7)
                );
                ApiResponse.ok(
                    resp,
                    workShiftService.approveAttendance(
                        shiftId,
                        adminId,
                        utils.JsonUtil.fromJson(req.getReader(), Map.class)
                    )
                );
                return;
            }
            if (
                path == null || !path.matches("/\\d+")
            ) throw new NumberFormatException();
            int shiftId = Integer.parseInt(path.substring(1));
            ApiResponse.ok(
                resp,
                workShiftService.update(
                    shiftId,
                    utils.JsonUtil.fromJson(req.getReader(), Map.class)
                ),
                "Shift updated"
            );
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid shift ID", 400);
        } catch (WorkShiftService.AttendanceNotFound e) {
            ApiResponse.error(resp, e.getMessage(), 404);
        } catch (StaffPayRateService.MissingRate e) {
            ApiResponse.error(resp, e.getMessage(), 422);
        } catch (
            WorkShiftService.ScheduleReferenceConflict
            | WorkShiftService.StaleAttendanceConflict
            | IllegalStateException e
        ) {
            ApiResponse.error(resp, e.getMessage(), 409);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        } catch (PersistenceException e) {
            ApiResponse.error(resp, "Không thể lưu lịch làm việc", 409);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
        if (!admin(req, resp)) return;
        resp.setContentType("application/json;charset=UTF-8");
        try {
            String path = req.getPathInfo();
            if (
                path == null || !path.matches("/\\d+")
            ) throw new NumberFormatException();
            int shiftId = Integer.parseInt(path.substring(1));
            workShiftService.delete(shiftId);
            ApiResponse.ok(resp, null, "Shift deleted");
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid shift ID", 400);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 409);
        }
    }

    private boolean admin(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return false;
        }
        String token = header.substring(7);
        if (
            !"ADMIN".equals(JwtUtil.getRole(token)) ||
            !PrivilegedAuth.isActiveRole(JwtUtil.getUserId(token), "ADMIN")
        ) {
            ApiResponse.error(resp, "Forbidden", 403);
            return false;
        }
        return true;
    }
}
