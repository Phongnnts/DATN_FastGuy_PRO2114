package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import service.GoodsReceiptService;
import utils.*;

@WebServlet("/api/admin/inventory/receipts/*")
public class GoodsReceiptServlet extends HttpServlet {

    private final GoodsReceiptService service;

    public GoodsReceiptServlet() {
        this(new GoodsReceiptService());
    }

    GoodsReceiptServlet(GoodsReceiptService value) {
        service = value;
    }

    protected void doGet(HttpServletRequest q, HttpServletResponse p)
        throws IOException {
        if (!authorized(q, p)) return;
        try {
            String info = q.getPathInfo();
            if (info == null || "/".equals(info)) {
                ApiResponse.ok(p, service.list());
                return;
            }
            String[] parts = path(info);
            if (!parts[1].isEmpty()) throw new IllegalArgumentException(
                "Invalid path"
            );
            ApiResponse.ok(p, service.get(Integer.parseInt(parts[0])));
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
            String info = q.getPathInfo();
            if (info == null || "/".equals(info)) {
                Map<String, Object> body = AdminInventoryItemServlet.body(q);
                Map<String, Object> result = service.create(body, user);
                p.setStatus(201);
                ApiResponse.ok(p, result);
            } else {
                String[] parts = path(info);
                if (
                    !"approve".equals(parts[1])
                ) throw new IllegalArgumentException("Invalid path");
                ApiResponse.ok(
                    p,
                    service.approve(Integer.parseInt(parts[0]), user)
                );
            }
        } catch (NoSuchElementException e) {
            ApiResponse.error(p, e.getMessage(), 404);
        } catch (IllegalStateException e) {
            ApiResponse.error(p, e.getMessage(), 409);
        } catch (IllegalArgumentException e) {
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
            String[] parts = path(q.getPathInfo());
            if (!parts[1].isEmpty()) throw new IllegalArgumentException(
                "Invalid path"
            );
            ApiResponse.ok(
                p,
                service.update(
                    Integer.parseInt(parts[0]),
                    AdminInventoryItemServlet.body(q),
                    user
                )
            );
        } catch (NoSuchElementException e) {
            ApiResponse.error(p, e.getMessage(), 404);
        } catch (IllegalStateException e) {
            ApiResponse.error(p, e.getMessage(), 409);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(p, e.getMessage(), 400);
        } catch (RuntimeException e) {
            ApiResponse.error(p, "Internal server error", 500);
        }
    }

    protected void doDelete(HttpServletRequest q, HttpServletResponse p)
        throws IOException {
        if (!authorized(q, p)) return;
        try {
            String[] parts = path(q.getPathInfo());
            if (!parts[1].isEmpty()) throw new IllegalArgumentException(
                "Invalid path"
            );
            service.delete(Integer.parseInt(parts[0]));
            p.setStatus(204);
        } catch (NoSuchElementException e) {
            ApiResponse.error(p, e.getMessage(), 404);
        } catch (IllegalStateException e) {
            ApiResponse.error(p, e.getMessage(), 409);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(p, e.getMessage(), 400);
        } catch (RuntimeException e) {
            ApiResponse.error(p, "Internal server error", 500);
        }
    }

    static String[] path(String value) {
        if (
            value == null || !value.matches("/[1-9]\\d*(/approve)?")
        ) throw new IllegalArgumentException("Invalid path");
        String[] raw = value.substring(1).split("/", -1);
        return new String[] { raw[0], raw.length == 2 ? raw[1] : "" };
    }

    protected int admin(HttpServletRequest q, HttpServletResponse p)
        throws IOException {
        return AdminApiAuth.require(q, p, tokenReader());
    }

    protected boolean authorized(HttpServletRequest q, HttpServletResponse p)
        throws IOException {
        return admin(q, p) >= 0;
    }

    protected AdminApiAuth.TokenReader tokenReader() {
        return AdminApiAuth.jwt();
    }
}
