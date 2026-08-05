package servlet;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.InventoryAdjustmentService;
import utils.ApiResponse;
import utils.JsonUtil;
import utils.JwtUtil;
import utils.PrivilegedAuth;

@WebServlet("/api/admin/inventory/transactions/*")
public class AdminInventoryAdjustmentServlet extends HttpServlet {
    private final InventoryAdjustmentService adjustmentService = new InventoryAdjustmentService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int adminId = requireAdmin(req, resp);
        if (adminId < 0) return;

        String path = req.getPathInfo();
        if (!"/adjustments".equals(path) && !"/waste".equals(path)) {
            resp.sendError(404);
            return;
        }
        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) {
            ApiResponse.error(resp, "Invalid data", 400);
            return;
        }
        try {
            int variantId = intOf(body.get("variantId"), "variantId");
            String reasonCode = strOf(body.get("reasonCode"));
            String note = strOf(body.get("note"));
            if ("/waste".equals(path)) {
                int quantity = intOf(body.get("quantity"), "quantity");
                ApiResponse.ok(resp, adjustmentService.waste(variantId, quantity, reasonCode, note, adminId), "Đã ghi nhận lãng phí");
            } else {
                int newQuantity = intOf(body.get("newQuantity"), "newQuantity");
                ApiResponse.ok(resp, adjustmentService.adjust(variantId, newQuantity, reasonCode, note, adminId), "Đã điều chỉnh tồn kho");
            }
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        }
    }

    private int requireAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { ApiResponse.error(resp, "Missing token", 401); return -1; }
        String token = header.substring(7);
        int userId = JwtUtil.getUserId(token);
        if (!"ADMIN".equals(JwtUtil.getRole(token)) || !PrivilegedAuth.isActiveRole(userId, "ADMIN")) { ApiResponse.error(resp, "Forbidden", 403); return -1; }
        return userId;
    }

    private int intOf(Object value, String name) {
        if (!(value instanceof Number)) throw new NumberFormatException(name + " phải là số nguyên");
        return ((Number) value).intValue();
    }

    private String strOf(Object value) {
        if (value == null) return null;
        return value instanceof String ? ((String) value).trim() : null;
    }
}
