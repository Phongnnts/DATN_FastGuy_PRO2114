package servlet;

import dao.InventoryTransactionDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;
import utils.JwtUtil;
import utils.PrivilegedAuth;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;

@WebServlet("/api/admin/inventory/transactions")
public class AdminInventoryServlet extends HttpServlet {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 200;
    private static final Set<String> ALLOWED_TYPES = Set.of("RESERVE", "RELEASE", "CONSUME", "WASTE");
    private final InventoryTransactionDAO transactionDAO = new InventoryTransactionDAO();

    static boolean isValidTransactionType(String type) {
        return type == null || type.isBlank() || ALLOWED_TYPES.contains(type);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!admin(req, resp)) return;
        resp.setContentType("application/json;charset=UTF-8");
        try {
            Integer variantId = parseInteger(req.getParameter("variantId"));
            Integer productId = parseInteger(req.getParameter("productId"));
            LocalDate fromDate = parseDate(req.getParameter("fromDate"));
            LocalDate toDate = parseDate(req.getParameter("toDate"));
            if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
                ApiResponse.error(resp, "fromDate must not be after toDate", 400);
                return;
            }
            String type = req.getParameter("transactionType");
            if (!isValidTransactionType(type)) {
                ApiResponse.error(resp, "Invalid transactionType, allowed: RESERVE, RELEASE, CONSUME, WASTE", 400);
                return;
            }
            Integer pageParam = parseInteger(req.getParameter("page"));
            int page = pageParam != null ? pageParam : DEFAULT_PAGE;
            Integer sizeParam = parseInteger(req.getParameter("size"));
            int size = sizeParam != null ? sizeParam : DEFAULT_SIZE;
            if (page < 1 || size < 1 || size > MAX_SIZE) {
                ApiResponse.error(resp, "page must be >= 1 and size must be 1.." + MAX_SIZE, 400);
                return;
            }
            Map<String, Object> result = transactionDAO.find(variantId, productId, type, fromDate, toDate, page, size);
            ApiResponse.ok(resp, result);
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "variantId, productId, page and size must be integers", 400);
        } catch (DateTimeParseException e) {
            ApiResponse.error(resp, "Invalid date format, expected yyyy-MM-dd", 400);
        }
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.isBlank()) return null;
        return Integer.parseInt(value);
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value);
    }

    private boolean admin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { ApiResponse.error(resp, "Missing token", 401); return false; }
        String token = header.substring(7);
        if (!"ADMIN".equals(JwtUtil.getRole(token)) || !PrivilegedAuth.isActiveRole(JwtUtil.getUserId(token), "ADMIN")) { ApiResponse.error(resp, "Forbidden", 403); return false; }
        return true;
    }
}
