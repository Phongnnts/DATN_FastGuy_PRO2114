package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.DateTimeException;
import java.util.*;
import service.OperatingFinanceService;
import utils.ApiResponse;

@WebServlet("/api/admin/fixed-assets/*")
public class FixedAssetServlet extends HttpServlet {

    private static final Set<String> KEYS = Set.of(
        "assetName",
        "acquisitionCost",
        "salvageValue",
        "depreciationStartDate",
        "usefulLifeMonths"
    );
    private final OperatingFinanceService service;

    public FixedAssetServlet() {
        this(new OperatingFinanceService());
    }

    FixedAssetServlet(OperatingFinanceService service) {
        this.service = service;
    }

    protected void doGet(HttpServletRequest q, HttpServletResponse p)
        throws IOException {
        int user = admin(q, p);
        if (user < 0) return;
        try {
            String path = q.getPathInfo();
            ApiResponse.ok(
                p,
                path == null || "/".equals(path)
                    ? service.listAssets()
                    : service.getAsset(OperatingExpenseServlet.id(path))
            );
        } catch (NoSuchElementException e) {
            ApiResponse.error(p, e.getMessage(), 404);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(p, e.getMessage(), 400);
        } catch (RuntimeException e) {
            ApiResponse.error(p, "Internal server error", 500);
        }
    }

    protected void doPost(HttpServletRequest q, HttpServletResponse p)
        throws IOException {
        int user = admin(q, p);
        if (user < 0) return;
        try {
            Map<String, Object> b = OperatingExpenseServlet.body(q);
            OperatingExpenseServlet.exact(b, KEYS);
            p.setStatus(201);
            ApiResponse.ok(
                p,
                service.createAsset(
                    OperatingExpenseServlet.str(b, "assetName"),
                    OperatingExpenseServlet.money(b, "acquisitionCost"),
                    OperatingExpenseServlet.money(b, "salvageValue"),
                    OperatingExpenseServlet.date(b, "depreciationStartDate"),
                    months(b),
                    user
                )
            );
        } catch (IllegalArgumentException | DateTimeException e) {
            ApiResponse.error(p, e.getMessage(), 400);
        } catch (RuntimeException e) {
            ApiResponse.error(p, "Internal server error", 500);
        }
    }

    protected void doPut(HttpServletRequest q, HttpServletResponse p)
        throws IOException {
        int user = admin(q, p);
        if (user < 0) return;
        try {
            String path = q.getPathInfo();
            Map<String, Object> b = OperatingExpenseServlet.body(q);
            if (path != null && path.matches("/[1-9]\\d*/retire")) {
                OperatingExpenseServlet.exact(b, Set.of("expectedStatus"));
                int id = Integer.parseInt(
                    path.substring(1, path.indexOf('/', 1))
                );
                ApiResponse.ok(
                    p,
                    service.retireAsset(
                        id,
                        OperatingExpenseServlet.str(b, "expectedStatus")
                    )
                );
                return;
            }
            OperatingExpenseServlet.exact(b, KEYS);
            ApiResponse.ok(
                p,
                service.updateAsset(
                    OperatingExpenseServlet.id(path),
                    OperatingExpenseServlet.str(b, "assetName"),
                    OperatingExpenseServlet.money(b, "acquisitionCost"),
                    OperatingExpenseServlet.money(b, "salvageValue"),
                    OperatingExpenseServlet.date(b, "depreciationStartDate"),
                    months(b)
                )
            );
        } catch (NoSuchElementException e) {
            ApiResponse.error(p, e.getMessage(), 404);
        } catch (IllegalStateException e) {
            ApiResponse.error(p, e.getMessage(), 409);
        } catch (IllegalArgumentException | DateTimeException e) {
            ApiResponse.error(p, e.getMessage(), 400);
        } catch (RuntimeException e) {
            ApiResponse.error(p, "Internal server error", 500);
        }
    }

    protected int admin(HttpServletRequest q, HttpServletResponse p)
        throws IOException {
        return AdminApiAuth.require(q, p, AdminApiAuth.jwt());
    }

    private static int months(Map<String, Object> b) {
        int value = OperatingExpenseServlet.integer(b, "usefulLifeMonths");
        if (value < 1) throw new IllegalArgumentException(
            "Invalid usefulLifeMonths"
        );
        return value;
    }
}
