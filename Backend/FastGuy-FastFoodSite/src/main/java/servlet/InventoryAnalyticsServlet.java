package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.InventoryReportService;
import utils.ApiResponse;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;

@WebServlet("/api/admin/inventory/analytics")
public class InventoryAnalyticsServlet extends HttpServlet {
    private final InventoryReportService service;

    public InventoryAnalyticsServlet() {
        this(new InventoryReportService());
    }

    InventoryAnalyticsServlet(InventoryReportService service) {
        this.service = service;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!authorized(request, response)) return;
        try {
            String from = request.getParameter("fromDate");
            String to = request.getParameter("toDate");
            String granularity = request.getParameter("granularity");
            if (from == null || to == null || !"DAY".equals(granularity)) throw new IllegalArgumentException("fromDate, toDate and DAY granularity are required");
            ApiResponse.ok(response, service.analytics(LocalDate.parse(from), LocalDate.parse(to)));
        } catch (IllegalArgumentException | DateTimeException exception) {
            ApiResponse.error(response, exception.getMessage(), 400);
        } catch (RuntimeException exception) {
            ApiResponse.error(response, "Internal server error", 500);
        }
    }

    protected boolean authorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return AdminApiAuth.require(request, response, AdminApiAuth.jwt()) >= 0;
    }
}
