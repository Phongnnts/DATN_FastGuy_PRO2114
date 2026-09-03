package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDateTime;
import service.ActivityLogService;
import utils.ApiResponse;

@WebServlet("/api/admin/activity-logs")
public class AdminActivityLogServlet extends HttpServlet {

    private final ActivityLogService service;

    public AdminActivityLogServlet() {
        this(new ActivityLogService());
    }

    AdminActivityLogServlet(ActivityLogService value) {
        service = value;
    }

    protected void doGet(HttpServletRequest q, HttpServletResponse p)
        throws IOException {
        if (AdminApiAuth.require(q, p, AdminApiAuth.jwt()) < 0) return;
        try {
            ApiResponse.ok(
                p,
                service.list(
                    date(q.getParameter("from")),
                    date(q.getParameter("to")),
                    q.getParameter("actionType"),
                    integer(q.getParameter("actorUserId"), null),
                    integer(q.getParameter("page"), 1),
                    integer(q.getParameter("pageSize"), 20)
                )
            );
        } catch (RuntimeException e) {
            ApiResponse.error(p, "Invalid activity log filters", 400);
        }
    }

    static LocalDateTime date(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return java.time.OffsetDateTime.parse(value)
                .withOffsetSameInstant(java.time.ZoneOffset.UTC)
                .toLocalDateTime();
        } catch (java.time.format.DateTimeParseException e) {
            return LocalDateTime.parse(value);
        }
    }

    static Integer integer(String value, Integer fallback) {
        if (value == null || value.isBlank()) return fallback;
        return Integer.valueOf(value);
    }
}
