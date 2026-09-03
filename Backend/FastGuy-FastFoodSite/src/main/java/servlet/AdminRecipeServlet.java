package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import service.AdminInventoryService;
import service.AdminRecipeService;
import utils.*;

@WebServlet("/api/admin/product-variants/*")
public class AdminRecipeServlet extends HttpServlet {

    private final AdminRecipeService service;

    public AdminRecipeServlet() {
        this(new AdminRecipeService());
    }

    AdminRecipeServlet(AdminRecipeService service) {
        this.service = service;
    }

    protected void doGet(HttpServletRequest q, HttpServletResponse p)
        throws IOException {
        if (!admin(q, p)) return;
        try {
            Path x = path(q.getPathInfo());
            ApiResponse.ok(
                p,
                switch (x.resource) {
                    case "recipe" -> service.get(x.id);
                    case "inventory-settings" -> service.settings(x.id);
                    case "inventory-capacity" -> service.capacity(x.id);
                    case "availability" -> service.availability(x.id);
                    default -> throw new IllegalArgumentException(
                        "Invalid path"
                    );
                }
            );
        } catch (NoSuchElementException e) {
            ApiResponse.error(p, e.getMessage(), 404);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(p, e.getMessage(), 400);
        } catch (RuntimeException e) {
            ApiResponse.error(p, "Internal server error", 500);
        }
    }

    protected void doPut(HttpServletRequest q, HttpServletResponse p)
        throws IOException {
        if (!admin(q, p)) return;
        try {
            Path x = path(q.getPathInfo());
            Map<String, Object> b = AdminInventoryItemServlet.body(q);
            if ("inventory-settings".equals(x.resource)) {
                AdminInventoryItemServlet.requireExactKeys(
                    b,
                    Set.of("inventoryMode", "expectedUpdatedAt")
                );
                ApiResponse.ok(
                    p,
                    service.updateSettings(
                        x.id,
                        AdminInventoryItemServlet.str(b, "inventoryMode"),
                        dateTime(b, "expectedUpdatedAt")
                    )
                );
                return;
            }
            if (
                !"recipe".equals(x.resource)
            ) throw new IllegalArgumentException("Invalid path");
            AdminInventoryItemServlet.requireExactKeys(
                b,
                Set.of("yieldQuantity", "active", "items", "expectedUpdatedAt")
            );
            if (
                !(b.get("items") instanceof List<?> raw)
            ) throw new IllegalArgumentException("Invalid recipe items");
            List<Integer> ids = new ArrayList<>();
            List<BigDecimal> qs = new ArrayList<>();
            for (Object o : raw) {
                if (
                    !(o instanceof Map<?, ?> m)
                ) throw new IllegalArgumentException("Invalid recipe item");
                if (
                    !m.keySet().equals(Set.of("inventoryItemId", "quantity"))
                ) throw new IllegalArgumentException(
                    "Unknown recipe item field"
                );
                ids.add(
                    AdminInventoryItemServlet.positiveInt(
                        m.get("inventoryItemId"),
                        "inventoryItemId"
                    )
                );
                qs.add(AdminInventoryService.decimal(m.get("quantity"), true));
            }
            ApiResponse.ok(
                p,
                service.replace(
                    x.id,
                    AdminInventoryItemServlet.dec(b, "yieldQuantity", true),
                    AdminInventoryItemServlet.bool(b, "active"),
                    ids,
                    qs,
                    b.get("expectedUpdatedAt") == null
                        ? null
                        : dateTime(b, "expectedUpdatedAt")
                )
            );
        } catch (NoSuchElementException e) {
            ApiResponse.error(p, e.getMessage(), 404);
        } catch (
            AdminRecipeService.OptimisticConflictException
            | AdminRecipeService.ModeNotReadyException e
        ) {
            ApiResponse.error(p, e.getMessage(), 409);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(p, e.getMessage(), 400);
        } catch (RuntimeException e) {
            ApiResponse.error(p, "Internal server error", 500);
        }
    }

    protected boolean admin(HttpServletRequest q, HttpServletResponse p)
        throws IOException {
        return AdminApiAuth.require(q, p, tokenReader()) >= 0;
    }

    protected AdminApiAuth.TokenReader tokenReader() {
        return AdminApiAuth.jwt();
    }

    private static LocalDateTime dateTime(Map<String, Object> b, String key) {
        try {
            return LocalDateTime.parse(AdminInventoryItemServlet.str(b, key));
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid " + key);
        }
    }

    private static Path path(String s) {
        if (
            s == null ||
            !s.matches(
                "/[1-9]\\d*/(recipe|inventory-settings|inventory-capacity|availability)"
            )
        ) throw new IllegalArgumentException("Invalid path");
        String[] p = s.substring(1).split("/");
        try {
            return new Path(Integer.parseInt(p[0]), p[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid path");
        }
    }

    private record Path(int id, String resource) {}
}
