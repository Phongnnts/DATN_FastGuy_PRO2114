package servlet;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import dao.OrderItemDAO;
import dao.OrdersDAO;
import entity.Orders;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;
import utils.JwtUtil;

@WebServlet("/api/admin/orders")
public class AdminOrderServlet extends HttpServlet {
    private OrdersDAO ordersDAO = new OrdersDAO();
    private OrderItemDAO orderItemDAO = new OrderItemDAO();

    public List<java.util.Map<String, Object>> getOrdersData() {
        return ordersDAO.findAll().stream().map(o -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("orderId", o.getOrderId());
            m.put("orderCode", o.getOrderCode());
            m.put("status", o.getOrderStatus());
            m.put("customerName", o.getCustomerName());
            m.put("paymentMethod", o.getPaymentMethod());
            m.put("paymentStatus", o.getPaymentStatus());
            m.put("itemCount", orderItemDAO.findByOrderId(o.getOrderId()).stream().mapToInt(item -> item.getQuantity()).sum());
            m.put("finalAmount", o.getFinalAmount());
            m.put("serviceFee", o.getServiceFee());
            m.put("cancelledBy", o.getCancelledBy());
            m.put("failureReason", o.getFailureReason());
            m.put("refundStatus", o.getRefundStatus());
            m.put("refundAmount", o.getRefundAmount());
            m.put("refundedAt", o.getRefundedAt());
            m.put("refundNote", o.getRefundNote());
            m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return;
        }
        String role = JwtUtil.getRole(authHeader.substring(7));
        if (!"ADMIN".equals(role)) { ApiResponse.error(resp, "Forbidden", 403); return; }

        ApiResponse.ok(resp, getOrdersData());
    }
}
