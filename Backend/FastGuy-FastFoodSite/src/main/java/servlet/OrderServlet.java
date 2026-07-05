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
import service.SePayService;
import utils.ApiResponse;
import utils.JwtUtil;

@WebServlet("/api/orders/*")
public class OrderServlet extends HttpServlet {
    private OrderService orderService = new OrderService();
    private SePayService sePayService = new SePayService();
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
        String path = req.getPathInfo();

        // KHONG can auth
        if ("/track".equals(path)) {
            String code = req.getParameter("code");
            if (code == null) {
                ApiResponse.error(resp, "Missing order code", 400);
                return;
            }
            List<Orders> allOrders = ordersDAO.findAll();
            Orders order = allOrders.stream()
                .filter(o -> code.equals(o.getOrderCode()))
                .findFirst().orElse(null);
            if (order == null) {
                ApiResponse.error(resp, "Order not found", 404);
                return;
            }
            ApiResponse.ok(resp, toDetail(order));
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
                if (order == null || order.getUser().getUserId() != userId) {
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

        int zoneId = body.containsKey("zoneId") ? ((Number) body.get("zoneId")).intValue() : 0;
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
        BigDecimal shippingFee = BigDecimal.ZERO;
        try {
            if (body.containsKey("shippingFee")) {
                Object feeObj = body.get("shippingFee");
                if (feeObj instanceof Number) {
                    shippingFee = BigDecimal.valueOf(((Number) feeObj).doubleValue());
                } else if (feeObj instanceof String) {
                    shippingFee = new BigDecimal((String) feeObj);
                }
            }
        } catch (Exception e) {
            shippingFee = BigDecimal.ZERO;
        }

        if (address == null) {
            ApiResponse.error(resp, "Missing address", 400);
            return;
        }

        Orders order = null;
        try {
            order = orderService.checkout(userId, zoneId, address, phone, deliveryNote, paymentMethod,
                    ghnProvinceId, ghnDistrictId, ghnWardCode, toProvinceName, toDistrictName, toWardName, shippingFee, couponCode);
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse.error(resp, "Checkout failed: " + e.getMessage(), 500);
            return;
        }
        if (order == null) {
            ApiResponse.error(resp, "Cart is empty", 400);
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getOrderId());
        data.put("orderCode", order.getOrderCode());
        data.put("status", order.getOrderStatus());
        data.put("paymentStatus", order.getPaymentStatus());
        data.put("finalAmount", order.getFinalAmount());
        data.put("discountAmount", order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO);

        if ("BANK_TRANSFER".equals(paymentMethod)) {
            String qrUrl = sePayService.generateQrUrl(
                order.getOrderId(),
                order.getFinalAmount().longValue(),
                order.getOrderCode()
            );
            data.put("sepayQrUrl", qrUrl);
            data.put("transferContent", "TT " + order.getOrderCode());
        }

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

            order.setOrderStatus("CANCELLED");
            order.setCancelledAt(LocalDateTime.now());
            ordersDAO.save(order);
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
        m.put("finalAmount", o.getFinalAmount());
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
        data.put("finalAmount", o.getFinalAmount());
        data.put("paymentMethod", o.getPaymentMethod());
        data.put("paymentStatus", o.getPaymentStatus());
        data.put("couponCode", o.getCouponCode());
        data.put("discountAmount", o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO);
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

        return data;
    }

    private void handleGuestCheckout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> body = utils.JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) {
            ApiResponse.error(resp, "Invalid data", 400);
            return;
        }

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

        BigDecimal shippingFee = BigDecimal.ZERO;
        try {
            if (body.containsKey("shippingFee")) {
                Object feeObj = body.get("shippingFee");
                if (feeObj instanceof Number) {
                    shippingFee = BigDecimal.valueOf(((Number) feeObj).doubleValue());
                } else if (feeObj instanceof String) {
                    shippingFee = new BigDecimal((String) feeObj);
                }
            }
        } catch (Exception e) {
            shippingFee = BigDecimal.ZERO;
        }

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
                    itemsData, shippingFee, ghnProvinceId, ghnDistrictId, ghnWardCode,
                    toProvinceName, toDistrictName, toWardName, couponCode);
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse.error(resp, "Checkout failed: " + e.getMessage(), 500);
            return;
        }
        if (order == null) {
            ApiResponse.error(resp, "Invalid data", 400);
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getOrderId());
        data.put("orderCode", order.getOrderCode());
        data.put("status", order.getOrderStatus());
        data.put("paymentStatus", order.getPaymentStatus());
        data.put("finalAmount", order.getFinalAmount());
        data.put("discountAmount", order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO);

        resp.setStatus(201);
        ApiResponse.ok(resp, data, "Order created");
    }
}
