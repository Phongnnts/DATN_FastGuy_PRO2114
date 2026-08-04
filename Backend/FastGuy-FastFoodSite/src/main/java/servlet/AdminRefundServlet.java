package servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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

        try {
            LocalDate from = toLocalDate(req.getParameter("fromDate"));
            LocalDate to = toLocalDate(req.getParameter("toDate"));
            if (from != null && to != null && from.isAfter(to)) {
                ApiResponse.error(resp, "fromDate must not be after toDate", 400);
                return;
            }
            List<Orders> pending = ordersDAO.findRefunds(
                    req.getParameter("status"), from, to, req.getParameter("search"));
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
                m.put("refundAmount", o.getRefundAmount());
                m.put("cancelledAt", o.getCancelledAt() != null ? o.getCancelledAt().toString() : null);
                m.put("paidAt", o.getPaidAt() != null ? o.getPaidAt().toString() : null);
                m.put("refundedAt", o.getRefundedAt() != null ? o.getRefundedAt().toString() : null);
                m.put("failureReason", o.getFailureReason());
                m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
                return m;
            }).collect(Collectors.toList());
            ApiResponse.ok(resp, result);
        } catch (DateTimeParseException e) {
            ApiResponse.error(resp, "Invalid date format, expected yyyy-MM-dd", 400);
        }
    }

    private LocalDate toLocalDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { ApiResponse.error(resp, "Missing token", 401); return; }
        String token = header.substring(7);
        if (!"ADMIN".equals(JwtUtil.getRole(token))) { ApiResponse.error(resp, "Forbidden", 403); return; }
        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.length() < 2) { ApiResponse.error(resp, "Invalid order ID", 400); return; }
            int orderId = Integer.parseInt(pathInfo.substring(1));
            Map<String, Object> body = utils.JsonUtil.fromJson(req.getReader(), Map.class);
            Object rawStatus = body == null ? null : body.get("status");
            Object rawNote = body == null ? null : body.get("refundNote");
            Object rawAmount = body == null ? null : body.get("refundAmount");
            String status = rawStatus instanceof String s ? s : null;
            String note = rawNote instanceof String s ? s : null;
            BigDecimal amount = rawAmount == null ? null : new BigDecimal(String.valueOf(rawAmount));
            refundService.update(orderId, status, amount, note, JwtUtil.getUserId(token));
            ApiResponse.ok(resp, null, "Refund updated");
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid refund amount or order ID", 400);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        }
    }
}
