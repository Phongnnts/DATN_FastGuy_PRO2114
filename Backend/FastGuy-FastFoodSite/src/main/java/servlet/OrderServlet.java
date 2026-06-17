package servlet;

import dao.OrderItemDAO;
import dao.OrdersDAO;
import entity.Orders;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.OrderService;
import utils.ApiResponse;
import utils.JwtUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/orders/*")
public class OrderServlet extends HttpServlet {
    private OrderService orderService = new OrderService();
    private OrdersDAO ordersDAO = new OrdersDAO();
    private OrderItemDAO orderItemDAO = new OrderItemDAO();

    private int getUserId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return -1;
        }
        int userId = JwtUtil.getUserId(authHeader.substring(7));
        if (userId < 0) {
            ApiResponse.error(resp, "Invalid token", 401);
            return -1;
        }
        return userId;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId < 0) return;

        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            List<Map<String, Object>> orders = ordersDAO.findByUserId(userId)
                    .stream()
                    .map(o -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("orderId", o.getOrderId());
                        m.put("orderCode", o.getOrderCode());
                        m.put("status", o.getOrderStatus());
                        m.put("finalAmount", o.getFinalAmount());
                        m.put("createdAt", o.getCreatedAt().toString());
                        return m;
                    })
                    .collect(Collectors.toList());
            ApiResponse.ok(resp, orders);
        } else {
            int orderId = Integer.parseInt(path.substring(1));
            Orders order = ordersDAO.findById(orderId);
            if (order == null || order.getUser().getUserId() != userId) {
                ApiResponse.error(resp, "Order not found", 404);
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("orderId", order.getOrderId());
            data.put("orderCode", order.getOrderCode());
            data.put("status", order.getOrderStatus());
            data.put("totalAmount", order.getTotalAmount());
            data.put("shippingFee", order.getShippingFee());
            data.put("finalAmount", order.getFinalAmount());
            data.put("paymentMethod", order.getPaymentMethod());
            data.put("paymentStatus", order.getPaymentStatus());
            data.put("customerAddress", order.getCustomerAddress());
            data.put("deliveryNote", order.getDeliveryNote());
            data.put("createdAt", order.getCreatedAt().toString());

            List<Map<String, Object>> items = orderItemDAO.findByOrderId(orderId)
                    .stream()
                    .map(oi -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("productId", oi.getProduct().getProductId());
                        m.put("productName", oi.getProductName());
                        m.put("quantity", oi.getQuantity());
                        m.put("unitPrice", oi.getUnitPrice());
                        m.put("totalPrice", oi.getTotalPrice());
                        return m;
                    })
                    .collect(Collectors.toList());
            data.put("items", items);

            ApiResponse.ok(resp, data);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId < 0) return;

        Map<String, Object> body = utils.JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) {
            ApiResponse.error(resp, "Invalid data", 400);
            return;
        }

        int zoneId = ((Number) body.get("zoneId")).intValue();
        String address = (String) body.get("address");
        String phone = (String) body.get("phone");
        String deliveryNote = (String) body.get("deliveryNote");
        String paymentMethod = (String) body.get("paymentMethod");

        if (address == null) {
            ApiResponse.error(resp, "Missing address", 400);
            return;
        }

        Orders order = orderService.checkout(userId, zoneId, address, phone, deliveryNote, paymentMethod);
        if (order == null) {
            ApiResponse.error(resp, "Cart is empty", 400);
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getOrderId());
        data.put("orderCode", order.getOrderCode());
        data.put("status", order.getOrderStatus());
        data.put("finalAmount", order.getFinalAmount());

        resp.setStatus(201);
        ApiResponse.ok(resp, data, "Order created");
    }
}
