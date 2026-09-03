package servlet;

import dao.InventoryTransactionDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;
import utils.*;

@WebServlet("/api/admin/inventory/transactions")
public class AdminInventoryServlet extends HttpServlet {

    private static final Set<String> TYPES = Set.of(
        "RECEIPT",
        "RESERVE",
        "RELEASE",
        "CONSUME",
        "ADJUSTMENT",
        "WASTE",
        "RETURN"
    );
    private final InventoryTransactionDAO dao = new InventoryTransactionDAO();

    static boolean isValidTransactionType(String t) {
        return t == null || t.isBlank() || TYPES.contains(t);
    }

    protected void doGet(HttpServletRequest q, HttpServletResponse p)
        throws IOException {
        if (!admin(q, p)) return;
        try {
            Integer item = positive(
                    q.getParameter("inventoryItemId"),
                    "inventoryItemId"
                ),
                order = positive(q.getParameter("orderId"), "orderId");
            String type = q.getParameter("transactionType");
            if (
                !isValidTransactionType(type)
            ) throw new IllegalArgumentException("Invalid transactionType");
            LocalDate from = date(q.getParameter("fromDate")),
                to = date(q.getParameter("toDate"));
            if (
                from != null && to != null && from.isAfter(to)
            ) throw new IllegalArgumentException("Invalid date range");
            int page = or(num(q.getParameter("page")), 0),
                size = or(num(q.getParameter("size")), 20);
            if (
                page < 0 || size < 1 || size > 100
            ) throw new IllegalArgumentException("Invalid pagination");
            ApiResponse.ok(
                p,
                dao.find(item, order, type, from, to, page, size)
            );
        } catch (RuntimeException e) {
            ApiResponse.error(p, e.getMessage(), 400);
        }
    }

    protected boolean admin(HttpServletRequest q, HttpServletResponse p)
        throws IOException {
        return AdminApiAuth.require(q, p, tokenReader()) >= 0;
    }

    protected AdminApiAuth.TokenReader tokenReader() {
        return AdminApiAuth.jwt();
    }

    private static Integer num(String s) {
        return s == null || s.isBlank() ? null : Integer.valueOf(s);
    }

    static Integer positive(String s, String name) {
        Integer value = num(s);
        if (value != null && value <= 0) throw new IllegalArgumentException(
            name + " must be positive"
        );
        return value;
    }

    private static LocalDate date(String s) {
        return s == null || s.isBlank() ? null : LocalDate.parse(s);
    }

    private static int or(Integer x, int d) {
        return x == null ? d : x;
    }
}
