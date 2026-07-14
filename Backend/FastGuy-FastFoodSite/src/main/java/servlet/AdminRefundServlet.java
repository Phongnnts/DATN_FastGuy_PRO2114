package servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.RefundService;
import utils.ApiResponse;
import utils.JwtUtil;

@WebServlet("/api/admin/refunds/*")
public class AdminRefundServlet extends HttpServlet {
    private RefundService refundService = new RefundService();

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { ApiResponse.error(resp, "Missing token", 401); return; }
        String token = header.substring(7);
        if (!"ADMIN".equals(JwtUtil.getRole(token))) { ApiResponse.error(resp, "Forbidden", 403); return; }
        try {
            int orderId = Integer.parseInt(req.getPathInfo().substring(1));
            Map<String, Object> body = utils.JsonUtil.fromJson(req.getReader(), Map.class);
            Object rawAmount = body == null ? null : body.get("refundAmount");
            BigDecimal amount = rawAmount == null ? null : new BigDecimal(String.valueOf(rawAmount));
            refundService.update(orderId, body == null ? null : (String) body.get("status"), amount, body == null ? null : (String) body.get("refundNote"), JwtUtil.getUserId(token));
            ApiResponse.ok(resp, null, "Refund updated");
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid refund amount or order ID", 400);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        }
    }
}
