package servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        if ("/track".equals(path)) {
            String code = req.getParameter("code");
            if (code == null) {
                ApiResponse.error(resp, "Missing order code", 400);
                return;
            }
            List<Orders> orders = ordersDAO.findByUserId(userId);
            Orders order = orders.stream().filter(o -> code.equals(o.getOrderCode())).findFirst().orElse(null);
            if (order == null) {
                ApiResponse.error(resp, "Order not found", 404);
                return;
            }
            ApiResponse.ok(resp, toDetail(order));
            return;
        }

        if ("/history".equals(path)) {
            List<Map<String, Object>> orders = ordersDAO.findByUserId(userId)
                    .stream()
                    .map(this::toListItem)
                    .collect(Collectors.toList());
            ApiResponse.ok(resp, orders);
            return;
        }

        if (path == null || path.equals("/")) {
            List<Map<String, Object>> orders = ordersDAO.findByUserId(userId)
                    .stream()
                    .map(this::toListItem)
                    .collect(Collectors.toList());
            ApiResponse.ok(resp, orders);
            return;
        }

        try {
            int orderId = Integer.parseInt(path.substring(1));
            Orders order = ordersDAO.findById(orderId);
            if (order == null || order.getUser().getUserId() != userId) {
                ApiResponse.error(resp, "Order not found", 404);
                return;
            }
            ApiResponse.ok(resp, toDetail(order));
        } catch (NumberFormatException e) {
            resp.sendError(404);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId < 0) return;

        String path = req.getPathInfo();
        Map<String, Object> body = utils.JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) {
            ApiResponse.error(resp, "Invalid data", 400);
            return;
        }

        if (path != null && path.endsWith("/review")) {
            String idStr = path.substring(1, path.length() - "/review".length());
            try {
                int orderId = Integer.parseInt(idStr);
                Orders order = ordersDAO.findById(orderId);
                if (order == null || order.getUser().getUserId() != userId) {
                    ApiResponse.error(resp, "Order not found", 404);
                    return;
                }
                ordersDAO.save(order);
                ApiResponse.ok(resp, null, "Review submitted");
            } catch (NumberFormatException e) {
                ApiResponse.error(resp, "Invalid order ID", 400);
            }
            return;
        }

        int zoneId = body.containsKey("zoneId") ? ((Number) body.get("zoneId")).intValue() : 0;
        String address = (String) body.get("address");
        String phone = (String) body.get("phone");
        String deliveryNote = (String) body.get("deliveryNote");
        String paymentMethod = (String) body.get("paymentMethod");
        Integer ghnProvinceId = body.containsKey("ghnProvinceId") ? ((Number) body.get("ghnProvinceId")).intValue() : null;
        Integer ghnDistrictId = body.containsKey("ghnDistrictId") ? ((Number) body.get("ghnDistrictId")).intValue() : null;
        String ghnWardCode = (String) body.get("ghnWardCode");
        String toProvinceName = (String) body.get("toProvinceName");
        String toDistrictName = (String) body.get("toDistrictName");
        String toWardName = (String) body.get("toWardName");
        BigDecimal shippingFee = body.containsKey("shippingFee")
                ? BigDecimal.valueOf(((Number) body.get("shippingFee")).doubleValue())
                : BigDecimal.ZERO;

        if (address == null) {
            ApiResponse.error(resp, "Missing address", 400);
            return;
        }

        Orders order = orderService.checkout(userId, zoneId, address, phone, deliveryNote, paymentMethod,
                ghnProvinceId, ghnDistrictId, ghnWardCode, toProvinceName, toDistrictName, toWardName, shippingFee);
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

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId < 0) return;

        String path = req.getPathInfo();
        if (path == null || !path.endsWith("/cancel")) {
            resp.sendError(404);
            return;
        }

        String orderIdStr = path.substring(1, path.length() - "/cancel".length());
        int orderId = Integer.parseInt(orderIdStr);

        Orders order = ordersDAO.findById(orderId);
        if (order == null || order.getUser().getUserId() != userId) {
            ApiResponse.error(resp, "Order not found", 404);
            return;
        }
        if (!"PENDING".equals(order.getOrderStatus())) {
            ApiResponse.error(resp, "Cannot cancel order in current status", 400);
            return;
        }

        order.setOrderStatus("CANCELLED");
        order.setCancelledAt(LocalDateTime.now());
        ordersDAO.save(order);

        ApiResponse.ok(resp, null, "Order cancelled");
    }

    private Map<String, Object> toListItem(Orders o) {
        Map<String, Object> m = new HashMap<>();
        m.put("orderId", o.getOrderId());
        m.put("orderCode", o.getOrderCode());
        m.put("status", o.getOrderStatus());
        m.put("finalAmount", o.getFinalAmount());
        m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        m.put("items", new ArrayList<>());
        return m;
    }

    private Map<String, Object> toDetail(Orders o) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", o.getOrderId());
        data.put("orderCode", o.getOrderCode());
        data.put("userId", o.getUser().getUserId());
        data.put("status", o.getOrderStatus());
        data.put("totalAmount", o.getTotalAmount());
        data.put("shippingFee", o.getShippingFee());
        data.put("finalAmount", o.getFinalAmount());
        data.put("paymentMethod", o.getPaymentMethod());
        data.put("paymentStatus", o.getPaymentStatus());
        data.put("customerAddress", o.getCustomerAddress());
        data.put("deliveryNote", o.getDeliveryNote());
        data.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        data.put("updatedAt", o.getConfirmedAt() != null ? o.getConfirmedAt().toString() : o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);

        List<Map<String, Object>> items = orderItemDAO.findByOrderId(o.getOrderId())
                .stream()
                .map(oi -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("productId", oi.getProduct().getProductId());
                    m.put("variantId", oi.getVariant() != null ? oi.getVariant().getVariantId() : null);
                    m.put("productName", oi.getProductName());
                    m.put("variantName", oi.getVariantName() != null ? oi.getVariantName() : "");
                    m.put("quantity", oi.getQuantity());
                    m.put("unitPrice", oi.getUnitPrice());
                    m.put("totalPrice", oi.getTotalPrice());
                    m.put("image", oi.getProduct().getImageUrl() != null ? oi.getProduct().getImageUrl() : "");
                    return m;
                })
                .collect(Collectors.toList());
        data.put("items", items);

        List<Map<String, Object>> history = new ArrayList<>();
        history.add(Map.of("status", "PENDING", "time", o.getCreatedAt() != null ? o.getCreatedAt().toString() : "", "note", ""));
        if (o.getConfirmedAt() != null) {
            history.add(Map.of("status", "CONFIRMED", "time", o.getConfirmedAt().toString(), "note", ""));
        }
        String currentStatus = o.getOrderStatus();
        if ("PREPARING".equals(currentStatus) || o.getReadyAt() != null) {
            String t = o.getConfirmedAt() != null ? o.getConfirmedAt().toString() : "";
            history.add(Map.of("status", "PREPARING", "time", t, "note", "Đang chế biến"));
        }
        if (o.getReadyAt() != null) {
            history.add(Map.of("status", "READY", "time", o.getReadyAt().toString(), "note", ""));
        }
        if (o.getCancelledAt() != null) {
            String reason = o.getFailureReason() != null ? o.getFailureReason() : "";
            history.add(Map.of("status", "CANCELLED", "time", o.getCancelledAt().toString(), "note", reason));
        }
        data.put("statusHistory", history);
        data.put("canReview", "DELIVERED".equals(o.getOrderStatus()));

        return data;
    }
}
