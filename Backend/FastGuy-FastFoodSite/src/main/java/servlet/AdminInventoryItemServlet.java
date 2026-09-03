package servlet;

import exception.InventoryItemConflictException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import service.AdminInventoryService;
import utils.*;

@WebServlet("/api/admin/inventory/items/*")
public class AdminInventoryItemServlet extends HttpServlet {

    private final AdminInventoryService service;

    public AdminInventoryItemServlet() {
        this(new AdminInventoryService());
    }

    AdminInventoryItemServlet(AdminInventoryService service) {
        this.service = service;
    }

    protected void doGet(HttpServletRequest q, HttpServletResponse p)
        throws IOException {
        if (!admin(q, p)) return;
        try {
            String x = q.getPathInfo();
            ApiResponse.ok(
                p,
                x == null || x.equals("/") ? service.list() : service.get(id(x))
            );
        } catch (NoSuchElementException e) {
            ApiResponse.error(p, e.getMessage(), 404);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(p, e.getMessage(), 400);
        }
    }

    protected void doPost(HttpServletRequest q, HttpServletResponse p)
        throws IOException {
        if (!admin(q, p)) return;
        try {
            Map<String, Object> b = body(q);
            requireExactKeys(
                b,
                Set.of(
                    "inventoryCode",
                    "name",
                    "itemType",
                    "baseUnit",
                    "minimumQuantity",
                    "countFrequency",
                    "active"
                )
            );
            Map<String, Object> d = service.create(
                str(b, "inventoryCode"),
                str(b, "name"),
                str(b, "itemType"),
                str(b, "baseUnit"),
                dec(b, "minimumQuantity", false),
                str(b, "countFrequency"),
                bool(b, "active")
            );
            p.setStatus(201);
            ApiResponse.ok(p, d);
        } catch (InventoryItemConflictException e) {
            conflict(p, e);
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
            Map<String, Object> b = body(q);
            requireExactKeys(
                b,
                Set.of(
                    "inventoryCode",
                    "name",
                    "itemType",
                    "baseUnit",
                    "minimumQuantity",
                    "countFrequency",
                    "active"
                )
            );
            ApiResponse.ok(
                p,
                service.update(
                    id(q.getPathInfo()),
                    str(b, "inventoryCode"),
                    str(b, "name"),
                    str(b, "itemType"),
                    str(b, "baseUnit"),
                    dec(b, "minimumQuantity", false),
                    str(b, "countFrequency"),
                    bool(b, "active")
                )
            );
        } catch (NoSuchElementException e) {
            ApiResponse.error(p, e.getMessage(), 404);
        } catch (InventoryItemConflictException e) {
            conflict(p, e);
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

    static Map<String, Object> body(HttpServletRequest q) throws IOException {
        try {
            Map<String, Object> b = JsonUtil.getMapper().readValue(
                q.getReader(),
                Map.class
            );
            if (b == null) throw new IllegalArgumentException("Invalid JSON");
            return b;
        } catch (com.fasterxml.jackson.core.JacksonException e) {
            throw new IllegalArgumentException("Invalid JSON", e);
        }
    }

    static void requireKeys(Map<String, Object> body, Set<String> allowed) {
        if (
            !allowed.containsAll(body.keySet())
        ) throw new IllegalArgumentException("Unknown request field");
    }

    static void requireExactKeys(
        Map<String, Object> body,
        Set<String> required
    ) {
        requireKeys(body, required);
        if (
            !body.keySet().containsAll(required)
        ) throw new IllegalArgumentException("Missing request field");
    }

    static int positiveInt(Object value, String name) {
        if (
            !(value instanceof Number number)
        ) throw new IllegalArgumentException("Invalid " + name);
        try {
            java.math.BigDecimal decimal = new java.math.BigDecimal(
                number.toString()
            );
            if (
                (decimal.scale() > 0 &&
                    decimal.stripTrailingZeros().scale() > 0) ||
                decimal.compareTo(java.math.BigDecimal.ONE) < 0 ||
                decimal.compareTo(
                    java.math.BigDecimal.valueOf(Integer.MAX_VALUE)
                ) > 0
            ) throw new IllegalArgumentException("Invalid " + name);
            return decimal.intValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Invalid " + name);
        }
    }

    static int id(String x) {
        if (
            x == null || !x.matches("/[1-9]\\d*")
        ) throw new IllegalArgumentException("Invalid ID");
        try {
            return Integer.parseInt(x.substring(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid ID");
        }
    }

    static String str(Map<String, Object> b, String k) {
        Object v = b.get(k);
        if (!(v instanceof String s)) throw new IllegalArgumentException(
            "Invalid " + k
        );
        return s.trim();
    }

    static boolean bool(Map<String, Object> b, String k) {
        if (
            !(b.get(k) instanceof Boolean v)
        ) throw new IllegalArgumentException("Invalid " + k);
        return v;
    }

    static BigDecimal dec(Map<String, Object> b, String k, boolean positive) {
        if (!b.containsKey(k)) throw new IllegalArgumentException(
            "Missing " + k
        );
        return AdminInventoryService.decimal(b.get(k), positive);
    }

    static void conflict(
        HttpServletResponse p,
        InventoryItemConflictException e
    ) throws IOException {
        p.setStatus(409);
        JsonUtil.write(
            p,
            Map.of(
                "status",
                "error",
                "message",
                e.getMessage(),
                "currentOnHandQuantity",
                e.getCurrentOnHandQuantity()
            )
        );
    }
}
