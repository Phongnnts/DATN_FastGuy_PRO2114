package servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dao.OrderItemDAO;
import entity.Orders;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.StaffOrderService;
import utils.ApiResponse;
import utils.JwtUtil;

@WebServlet("/api/staff/orders/*")
public class StaffOrderServlet extends HttpServlet {
    private StaffOrderService staffOrderService = new StaffOrderService();
    private OrderItemDAO orderItemDAO = new OrderItemDAO();

    private int getStaffId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return -1;
        }
        String token = authHeader.substring(7);
        String role = JwtUtil.getRole(token);
        if (!"STAFF".equals(role)) {
            ApiResponse.error(resp, "Forbidden", 403);
            return -1;
        }
        return JwtUtil.getUserId(token);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int staffId = getStaffId(req, resp);
        if (staffId < 0) return;

        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            List<Orders> orders = staffOrderService.getPendingOrders();
            List<Map<String, Object>> result = orders.stream().map(o -> toListItem(o)).collect(Collectors.toList());
            ApiResponse.ok(resp, result);
        } else if (path.equals("/confirmed")) {
            List<Orders> orders = staffOrderService.getConfirmedOrders();
            List<Map<String, Object>> result = orders.stream().map(o -> toListItem(o)).collect(Collectors.toList());
            ApiResponse.ok(resp, result);
        } else if (path.equals("/history")) {
            ApiResponse.ok(resp, staffOrderService.getPendingOrders().stream()
                    .map(o -> toListItem(o)).collect(Collectors.toList()));
        } else {
            try {
                int orderId = Integer.parseInt(path.substring(1));
                Orders order = staffOrderService.getOrderDetail(orderId);
                if (order == null) {
                    ApiResponse.error(resp, "Order not found", 404);
                    return;
                }
                ApiResponse.ok(resp, toDetail(order));
            } catch (NumberFormatException e) {
                resp.sendError(404);
            }
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int staffId = getStaffId(req, resp);
        if (staffId < 0) return;

        String path = req.getPathInfo();
        if (path == null) {
            resp.sendError(404);
            return;
        }

        String[] parts = path.split("/");
        if (parts.length < 3) {
            resp.sendError(404);
            return;
        }

        int orderId = Integer.parseInt(parts[1]);
        String action = parts[2];

        if ("status".equals(action)) {
            Map<String, Object> body = utils.JsonUtil.fromJson(req.getReader(), Map.class);
            if (body == null) {
                ApiResponse.error(resp, "Invalid data", 400);
                return;
            }
            String status = (String) body.get("status");
            if (status == null || (!"CONFIRMED".equals(status) && !"CANCELLED".equals(status))) {
                ApiResponse.error(resp, "Invalid status", 400);
                return;
            }
            boolean ok = staffOrderService.updateStatus(orderId, status, staffId);
            if (!ok) {
                ApiResponse.error(resp, "Order not found", 404);
                return;
            }
            ApiResponse.ok(resp, null, "Status updated");
        } else {
            resp.sendError(404);
        }
    }

    private Map<String, Object> toListItem(Orders o) {
        Map<String, Object> m = new HashMap<>();
        m.put("orderId", o.getOrderId());
        m.put("orderCode", o.getOrderCode());
        m.put("userId", o.getUser().getUserId());
        m.put("customerName", o.getCustomerName());
        m.put("orderStatus", o.getOrderStatus());
        m.put("items", new java.util.ArrayList<>());
        m.put("finalAmount", o.getFinalAmount());
        m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> toDetail(Orders o) {
        Map<String, Object> m = new HashMap<>();
        m.put("orderId", o.getOrderId());
        m.put("orderCode", o.getOrderCode());
        m.put("userId", o.getUser().getUserId());
        m.put("customerName", o.getCustomerName());
        m.put("customerPhone", o.getCustomerPhone());
        m.put("orderStatus", o.getOrderStatus());
        m.put("totalAmount", o.getTotalAmount());
        m.put("shippingFee", o.getShippingFee());
        m.put("finalAmount", o.getFinalAmount());
        m.put("paymentMethod", o.getPaymentMethod());
        m.put("paymentStatus", o.getPaymentStatus());
        m.put("customerAddress", o.getCustomerAddress());
        m.put("deliveryNote", o.getDeliveryNote());
        m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);

        List<Map<String, Object>> items = orderItemDAO.findByOrderId(o.getOrderId())
                .stream()
                .map(oi -> {
                    Map<String, Object> im = new HashMap<>();
                    im.put("productId", oi.getProduct().getProductId());
                    im.put("productName", oi.getProductName());
                    im.put("quantity", oi.getQuantity());
                    im.put("unitPrice", oi.getUnitPrice());
                    im.put("totalPrice", oi.getTotalPrice());
                    im.put("imageUrl", oi.getProduct().getImageUrl() != null ? oi.getProduct().getImageUrl() : "");
                    return im;
                })
                .collect(Collectors.toList());
        m.put("items", items);

        List<Map<String, Object>> history = new java.util.ArrayList<>();
        history.add(Map.of("status", "PENDING", "time", o.getCreatedAt() != null ? o.getCreatedAt().toString() : "", "note", ""));
        if (o.getConfirmedAt() != null) {
            history.add(Map.of("status", "CONFIRMED", "time", o.getConfirmedAt().toString(), "note", ""));
        }
        if (o.getCancelledAt() != null) {
            history.add(Map.of("status", "CANCELLED", "time", o.getCancelledAt().toString(), "note", ""));
        }
        m.put("statusHistory", history);
        m.put("internalNotes", new java.util.ArrayList<>());

        return m;
    }
}
