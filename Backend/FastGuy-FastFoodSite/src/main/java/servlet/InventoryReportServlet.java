package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.*;
import java.util.Set;
import service.InventoryReportService;
import service.MenuPerformanceReportService;
import utils.ApiResponse;

@WebServlet("/api/admin/inventory/reports/*")
public class InventoryReportServlet extends HttpServlet {

    private final InventoryReportService service;
    private final MenuPerformanceReportService performanceService;

    public InventoryReportServlet() {
        this(new InventoryReportService(), new MenuPerformanceReportService());
    }

    InventoryReportServlet(InventoryReportService value) {
        this(value, new MenuPerformanceReportService());
    }

    InventoryReportServlet(
        InventoryReportService value,
        MenuPerformanceReportService performance
    ) {
        service = value;
        performanceService = performance;
    }

    protected void doGet(HttpServletRequest q, HttpServletResponse p)
        throws IOException {
        if (!authorized(q, p)) return;
        try {
            String path = q.getPathInfo();
            if ("/menu-cost".equals(path)) {
                ApiResponse.ok(p, service.menuCost());
                return;
            }
            if (
                !Set.of("/summary", "/item-loss", "/menu-performance").contains(
                    path
                )
            ) throw new IllegalArgumentException("Invalid path");
            String from = q.getParameter("fromDate"),
                to = q.getParameter("toDate");
            if (from == null || to == null) throw new IllegalArgumentException(
                "fromDate and toDate are required"
            );
            LocalDate start = LocalDate.parse(from),
                end = LocalDate.parse(to);
            ApiResponse.ok(
                p,
                "/summary".equals(path)
                    ? service.summary(start, end)
                    : "/item-loss".equals(path)
                      ? service.itemLoss(start, end)
                      : performanceService.report(start, end)
            );
        } catch (IllegalArgumentException | DateTimeException e) {
            ApiResponse.error(p, e.getMessage(), 400);
        } catch (RuntimeException e) {
            ApiResponse.error(p, "Internal server error", 500);
        }
    }

    protected boolean authorized(HttpServletRequest q, HttpServletResponse p)
        throws IOException {
        return AdminApiAuth.require(q, p, AdminApiAuth.jwt()) >= 0;
    }
}
