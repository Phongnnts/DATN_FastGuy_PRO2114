package servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dao.OrdersDAO;
import entity.Orders;
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
    private OrdersDAO ordersDAO = new OrdersDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { ApiResponse.error(resp, "Missing token", 401); return; }
        if (!"ADMIN".equals(JwtUtil.getRole(header.substring(7)))) { ApiResponse.error(resp, "Forbidden", 403); return; }

        List<Orders> pending = ordersDAO.findPendingRefunds();
        List<Map<String, Object>> result = pending.stream().map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("orderId", o.getOrderId());
            m.put("orderCode", o.getOrderCode());
            m.put("customerName", o.getCustomerName());
            m.put("customerPhone", o.getCustomerPhone());
            m.put("finalAmount", o.getFinalAmount());
            m.put("paymentMethod", o.getPaymentMethod());
            m.put("paymentStatus", o.getPaymentStatus());
            m.put("refundStatus", o.getRefundStatus());
            m.put("cancelledAt", o.getCancelledAt() != null ? o.getCancelledAt().toString() : null);
            m.put("failureReason", o.getFailureReason());
            m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
            return m;
        }).collect(Collectors.toList());
        ApiResponse.ok(resp, result);
    }

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
