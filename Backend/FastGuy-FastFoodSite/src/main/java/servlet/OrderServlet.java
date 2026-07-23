package servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.SerializationFeature;

import dao.OrderItemDAO;
import dao.OrdersDAO;
import entity.Orders;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.OrderService;
import service.OrderTransitionService;
import service.PayOSPaymentService;
import utils.ApiResponse;
import utils.JwtUtil;

@WebServlet("/api/orders/*")
public class OrderServlet extends HttpServlet {
    private OrderService orderService = new OrderService();
    private PayOSPaymentService payOSPaymentService = new PayOSPaymentService();
    private OrdersDAO ordersDAO = new OrdersDAO();
    private OrderItemDAO orderItemDAO = new OrderItemDAO();
    private service.OrderStatusHistoryService orderStatusHistoryService = new service.OrderStatusHistoryService();

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
        String path = req.getPathInfo();

        // KHONG can auth — public tracking
        if ("/track".equals(path)) {
            String code = req.getParameter("code");
            String phoneSuffix = req.getParameter("phoneSuffix");
            if (code == null || code.isBlank()) {
                ApiResponse.error(resp, "Thiếu mã đơn hàng", 400);
                return;
            }
            if (phoneSuffix == null || !phoneSuffix.matches("\\d{4}")) {
                ApiResponse.error(resp, "Số điện thoại phải là đúng 4 chữ số cuối", 400);
                return;
            }
            Orders order = ordersDAO.findByOrderCode(code.trim());
            if (order == null) {
                ApiResponse.error(resp, "Không tìm thấy đơn hàng", 404);
                return;
            }
            String phone = order.getCustomerPhone();
            if (phone == null || !phone.endsWith(phoneSuffix)) {
                ApiResponse.error(resp, "Số điện thoại không khớp", 404);
                return;
            }
            ApiResponse.ok(resp, toPublicTrack(order));
            return;
        }

        // Verify PayOS payment — no auth needed (called from payment-return page)
        if (path != null && path.startsWith("/verify-payment/")) {
            try {
                int orderId = Integer.parseInt(path.substring("/verify-payment/".length()));
                boolean paid = payOSPaymentService.verifyPayment(orderId);
                Map<String, Object> result = new HashMap<>();
                result.put("paid", paid);
                ApiResponse.ok(resp, result);
            } catch (NumberFormatException e) {
                ApiResponse.error(resp, "Invalid order ID", 400);
            }
            return;
        }

        // Cac endpoint con lai can auth
        int userId = getUserId(req, resp);
        if (userId < 0) return;

        if (path != null && path.endsWith("/payment-status")) {
            String idStr = path.substring(1, path.length() - "/payment-status".length());
            try {
                int orderId = Integer.parseInt(idStr);
                Orders order = ordersDAO.findById(orderId);
                if (!OrderService.canUserAccess(order, userId)) {
                    ApiResponse.error(resp, "Not found", 404);
                    return;
                }
                java.util.Map<String, Object> pd = new HashMap<>();
                pd.put("paymentStatus", order.getPaymentStatus());
                pd.put("paidAt", order.getPaidAt());
                ApiResponse.ok(resp, pd);
            } catch (NumberFormatException e) {
                ApiResponse.error(resp, "Invalid order ID", 400);
            }
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
            if (!OrderService.canUserAccess(order, userId)) {
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

        String path = req.getPathInfo();
        if (path != null && path.endsWith("/guest-checkout")) {
            handleGuestCheckout(req, resp);
            return;
        }

        int userId = getUserId(req, resp);
        if (userId < 0) return;

        Map<String, Object> body = utils.JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) {
            ApiResponse.error(resp, "Invalid data", 400);
            return;
        }
        String idempotencyKey = req.getHeader("Idempotency-Key");
        String requestHash = requestHash(body);
        String cartSignature = utils.JsonUtil.toJson(body.get("cartSignature"));

        String customerName = (String) body.get("customerName");
        String address = (String) body.get("address");
        String phone = (String) body.get("phone");
        String deliveryNote = (String) body.get("deliveryNote");
        String paymentMethod = (String) body.get("paymentMethod");
        Integer ghnProvinceId = body.get("ghnProvinceId") instanceof Number ? ((Number) body.get("ghnProvinceId")).intValue() : null;
        Integer ghnDistrictId = body.get("ghnDistrictId") instanceof Number ? ((Number) body.get("ghnDistrictId")).intValue() : null;
        String ghnWardCode = (String) body.get("ghnWardCode");
        String toProvinceName = (String) body.get("toProvinceName");
        String toDistrictName = (String) body.get("toDistrictName");
        String toWardName = (String) body.get("toWardName");
        String couponCode = (String) body.get("couponCode");
        if (address == null) {
            ApiResponse.error(resp, "Missing address", 400);
            return;
        }

        Orders order = null;
        try {
            order = orderService.checkout(userId, customerName, address, phone, deliveryNote, paymentMethod,
                    ghnProvinceId, ghnDistrictId, ghnWardCode, toProvinceName, toDistrictName, toWardName, couponCode,
                    idempotencyKey, requestHash, cartSignature);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
            return;
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse.error(resp, "Checkout failed: " + e.getMessage(), 500);
            return;
        }
        if (order == null) {
            ApiResponse.error(resp, "Cart is empty", 400);
            return;
        }
        if ("BANK_TRANSFER".equals(order.getPaymentMethod())) {
            if (payOSPaymentService.createPaymentLink(order.getOrderId()) == null) {
                ApiResponse.error(resp, "Không thể tạo link PayOS, vui lòng thử lại", 502);
                return;
            }
            order = ordersDAO.findById(order.getOrderId());
            orderService.clearCart(userId);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getOrderId());
        data.put("orderCode", order.getOrderCode());
        data.put("status", order.getOrderStatus());
        data.put("paymentStatus", order.getPaymentStatus());
        data.put("finalAmount", order.getFinalAmount());
        data.put("shippingFee", order.getShippingFee());
        data.put("serviceFee", order.getServiceFee());
        data.put("discountAmount", order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO);
        addTransferData(data, order);

        resp.setStatus(201);
        ApiResponse.ok(resp, data, "Order created");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId < 0) return;

        String path = req.getPathInfo();
        if (path == null) {
            resp.sendError(404);
            return;
        }

        if (path.endsWith("/cancel")) {
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

            Map<String, Object> body = utils.JsonUtil.fromJson(req.getReader(), Map.class);
            String reason = body != null ? (String) body.get("reason") : null;
            boolean ok = orderService.cancelOrder(orderId, userId, null, reason, true);
            if (!ok) {
                ApiResponse.error(resp, "Cannot cancel order in current status", 400);
                return;
            }
            ApiResponse.ok(resp, null, "Order cancelled");
            return;
        }

        if (path.endsWith("/confirm-payment")) {
            ApiResponse.error(resp, "Payment must be confirmed by payment gateway", 403);
            return;
        }

        resp.sendError(404);
    }

    private Map<String, Object> toListItem(Orders o) {
        Map<String, Object> m = new HashMap<>();
        m.put("orderId", o.getOrderId());
        m.put("orderCode", o.getOrderCode());
        m.put("status", o.getOrderStatus());
        m.put("totalAmount", o.getTotalAmount());
        m.put("shippingFee", o.getShippingFee());
        m.put("serviceFee", o.getServiceFee());
        m.put("discountAmount", o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO);
        m.put("finalAmount", o.getFinalAmount());
        m.put("paymentMethod", o.getPaymentMethod());
        m.put("paymentStatus", o.getPaymentStatus());
        m.put("refundAmount", o.getRefundAmount());
        m.put("refundedAt", o.getRefundedAt());
        m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        List<Map<String, Object>> itemList = orderItemDAO.findByOrderId(o.getOrderId())
                .stream().map(oi -> {
                    Map<String, Object> im = new HashMap<>();
                    im.put("productName", oi.getProductName());
                    im.put("quantity", oi.getQuantity());
                    im.put("image", oi.getProduct().getImageUrl() != null ? oi.getProduct().getImageUrl() : "");
                    return im;
                }).collect(Collectors.toList());
        m.put("items", itemList);
        return m;
    }

    private Map<String, Object> toDetail(Orders o) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", o.getOrderId());
        data.put("orderCode", o.getOrderCode());
        data.put("userId", o.getUser() != null ? o.getUser().getUserId() : null);
        data.put("status", o.getOrderStatus());
        data.put("totalAmount", o.getTotalAmount());
        data.put("shippingFee", o.getShippingFee());
        data.put("serviceFee", o.getServiceFee());
        data.put("finalAmount", o.getFinalAmount());
        data.put("codCollectedAmount", o.getCodCollectedAmount());
        data.put("codCollectedAt", o.getCodCollectedAt() != null ? o.getCodCollectedAt().toString() : null);
        data.put("paymentMethod", o.getPaymentMethod());
        data.put("paymentStatus", o.getPaymentStatus());
        data.put("couponCode", o.getCouponCode());
        data.put("discountAmount", o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO);
        data.put("cancelledBy", o.getCancelledBy());
        data.put("refundStatus", o.getRefundStatus());
        data.put("refundAmount", o.getRefundAmount());
        data.put("refundedAt", o.getRefundedAt());
        data.put("refundNote", o.getRefundNote());
        data.put("failureReason", o.getFailureReason());
        data.put("customerAddress", o.getCustomerAddress());
        data.put("deliveryNote", o.getDeliveryNote());
        data.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        data.put("updatedAt", o.getConfirmedAt() != null ? o.getConfirmedAt().toString() : o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        addTransferData(data, o);

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
        var savedHistory = orderStatusHistoryService.getByOrderId(o.getOrderId());
        data.put("statusHistory", savedHistory.isEmpty() ? history : savedHistory);

        OrderTransitionService transitionService = new OrderTransitionService();
        data.put("allowedActions", transitionService.getAllowedActions(o.getOrderStatus(), "USER", o.getPaymentStatus()));

        return data;
    }

    private Map<String, Object> toPublicTrack(Orders o) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderCode", o.getOrderCode());
        data.put("orderStatus", o.getOrderStatus());
        data.put("paymentMethod", o.getPaymentMethod());
        data.put("paymentStatus", o.getPaymentStatus());
        data.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        data.put("estimatedDeliveryAt", o.getExpectedDeliveryTime() != null ? o.getExpectedDeliveryTime().toString() : null);

        List<Map<String, Object>> items = orderItemDAO.findByOrderId(o.getOrderId())
                .stream()
                .map(oi -> {
                    Map<String, Object> im = new HashMap<>();
                    im.put("name", oi.getProductName());
                    im.put("quantity", oi.getQuantity());
                    return im;
                })
                .collect(Collectors.toList());
        data.put("items", items);

        List<Map<String, Object>> history = new ArrayList<>();
        if (o.getCreatedAt() != null) {
            history.add(Map.of("status", "PENDING", "timestamp", o.getCreatedAt().toString()));
        }
        if (o.getConfirmedAt() != null) {
            history.add(Map.of("status", "CONFIRMED", "timestamp", o.getConfirmedAt().toString()));
        }
        if ("PREPARING".equals(o.getOrderStatus()) || o.getReadyAt() != null) {
            String t = o.getConfirmedAt() != null ? o.getConfirmedAt().toString() : "";
            history.add(Map.of("status", "PREPARING", "timestamp", t));
        }
        if (o.getReadyAt() != null) {
            history.add(Map.of("status", "READY", "timestamp", o.getReadyAt().toString()));
        }
        if (o.getCancelledAt() != null) {
            history.add(Map.of("status", "CANCELLED", "timestamp", o.getCancelledAt().toString()));
        }
        var savedHistory = orderStatusHistoryService.getByOrderId(o.getOrderId());
        if (!savedHistory.isEmpty()) {
            history = savedHistory.stream()
                    .map(entry -> Map.<String, Object>of("status", String.valueOf(entry.get("status")),
                            "timestamp", String.valueOf(entry.get("time"))))
                    .collect(Collectors.toList());
        }
        data.put("statusHistory", history);

        return data;
    }

    private void handleGuestCheckout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> body = utils.JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) {
            ApiResponse.error(resp, "Invalid data", 400);
            return;
        }
        String idempotencyKey = req.getHeader("Idempotency-Key");
        String requestHash = requestHash(body);

        String customerName = (String) body.get("customerName");
        String phone = (String) body.get("phone");
        String address = (String) body.get("address");
        String deliveryNote = (String) body.get("deliveryNote");
        String paymentMethod = (String) body.get("paymentMethod");

        Object rawItems = body.get("items");
        if (rawItems == null || !(rawItems instanceof List)) {
            ApiResponse.error(resp, "Missing items", 400);
            return;
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) rawItems;

        Integer ghnProvinceId = body.get("ghnProvinceId") instanceof Number ? ((Number) body.get("ghnProvinceId")).intValue() : null;
        Integer ghnDistrictId = body.get("ghnDistrictId") instanceof Number ? ((Number) body.get("ghnDistrictId")).intValue() : null;
        String ghnWardCode = (String) body.get("ghnWardCode");
        String toProvinceName = (String) body.get("toProvinceName");
        String toDistrictName = (String) body.get("toDistrictName");
        String toWardName = (String) body.get("toWardName");
        String couponCode = (String) body.get("couponCode");

        Orders order;
        try {
            order = orderService.guestCheckout(customerName, phone, address, deliveryNote, paymentMethod,
                    itemsData, ghnProvinceId, ghnDistrictId, ghnWardCode,
                    toProvinceName, toDistrictName, toWardName, couponCode, idempotencyKey, requestHash);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
            return;
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse.error(resp, "Checkout failed: " + e.getMessage(), 500);
            return;
        }
        if (order == null) {
            ApiResponse.error(resp, "Invalid data", 400);
            return;
        }
        if ("BANK_TRANSFER".equals(order.getPaymentMethod())) {
            if (payOSPaymentService.createPaymentLink(order.getOrderId()) == null) {
                ApiResponse.error(resp, "Không thể tạo link PayOS, vui lòng thử lại", 502);
                return;
            }
            order = ordersDAO.findById(order.getOrderId());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getOrderId());
        data.put("orderCode", order.getOrderCode());
        data.put("status", order.getOrderStatus());
        data.put("paymentStatus", order.getPaymentStatus());
        data.put("finalAmount", order.getFinalAmount());
        data.put("shippingFee", order.getShippingFee());
        data.put("serviceFee", order.getServiceFee());
        data.put("discountAmount", order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO);
        addTransferData(data, order);

        resp.setStatus(201);
        ApiResponse.ok(resp, data, "Order created");
    }

    private void addTransferData(Map<String, Object> data, Orders order) {
        if (!"BANK_TRANSFER".equals(order.getPaymentMethod())) return;
        if (!"PENDING".equals(order.getOrderStatus())) return;
        String checkoutUrl = order.getPayosCheckoutUrl();
        if (checkoutUrl != null && !checkoutUrl.isBlank()) data.put("checkoutUrl", checkoutUrl);
    }

    private String requestHash(Map<String, Object> body) {
        try {
            String canonical = utils.JsonUtil.getMapper().copy()
                    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsString(body);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
