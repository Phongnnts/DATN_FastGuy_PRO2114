package servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dao.OrderItemDAO;
import dao.OrdersDAO;
import dao.PaymentAttemptDAO;
import entity.Orders;
import entity.PaymentAttempt;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.OrderExpiryPolicy;
import service.OrderService;
import service.OrderStatusHistoryService;
import service.ReviewService;
import service.DeliveryFailurePolicy;
import service.OrderTransitionService;
import utils.ApiResponse;
import utils.JsonUtil;
import utils.JwtUtil;
import utils.PrivilegedAuth;

@WebServlet("/api/admin/orders/*")
public class AdminOrderServlet extends HttpServlet {
    private OrdersDAO ordersDAO = new OrdersDAO();
    private OrderItemDAO orderItemDAO = new OrderItemDAO();
    private PaymentAttemptDAO paymentAttemptDAO = new PaymentAttemptDAO();
    private OrderStatusHistoryService historyService = new OrderStatusHistoryService();
    private OrderService orderService = new OrderService();
    private OrderTransitionService transitionService = new OrderTransitionService();
    private ReviewService reviewService = new ReviewService();

    protected boolean checkAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) { ApiResponse.error(resp, "Missing token", 401); return false; }
        String token = authHeader.substring(7);
        if (!"ADMIN".equals(JwtUtil.getRole(token)) || !PrivilegedAuth.isActiveRole(JwtUtil.getUserId(token), "ADMIN")) { ApiResponse.error(resp, "Forbidden", 403); return false; }
        return true;
    }

    public List<Map<String, Object>> getOrdersData() {
        return ordersDAO.findAll().stream().map(this::toListItem).collect(Collectors.toList());
    }

    private Map<String, Object> toListItem(Orders o) {
            Map<String, Object> m = new HashMap<>();
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
            m.put("failureNote", o.getFailureReason());
            m.put("deliveryFailureCode", o.getDeliveryFailureCode());
            m.put("deliveryAttemptCount", o.getDeliveryAttemptCount());
            m.put("deliveryAttemptLimit", o.getDeliveryAttemptLimit());
            m.put("deliveryFailedAt", o.getDeliveryFailedAt() != null ? o.getDeliveryFailedAt().toString() : null);
            m.put("retryScheduledAt", o.getRetryScheduledAt() != null ? o.getRetryScheduledAt().toString() : null);
            m.put("returnedToStoreAt", o.getReturnedToStoreAt() != null ? o.getReturnedToStoreAt().toString() : null);
            m.put("refundStatus", o.getRefundStatus());
            m.put("refundAmount", o.getRefundAmount());
            m.put("refundedAt", o.getRefundedAt());
            m.put("refundNote", o.getRefundNote());
            m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
            return m;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            String fromDate = req.getParameter("fromDate");
            String toDate = req.getParameter("toDate");
            try {
                LocalDate from = fromDate == null || fromDate.isBlank() ? null : LocalDate.parse(fromDate);
                LocalDate to = toDate == null || toDate.isBlank() ? null : LocalDate.parse(toDate);
                if (from != null && to != null && from.isAfter(to)) {
                    ApiResponse.error(resp, "fromDate must not be after toDate", 400);
                    return;
                }
                LocalDateTime start = from == null ? null : from.atStartOfDay();
                LocalDateTime end = to == null ? null : to.plusDays(1).atStartOfDay();
                List<Map<String, Object>> allData = ordersDAO.findAllByCreatedAtRange(start, end).stream().map(this::toListItem).collect(Collectors.toList());
                ApiResponse.ok(resp, allData);
            } catch (DateTimeParseException e) {
                ApiResponse.error(resp, "Invalid date format, expected yyyy-MM-dd", 400);
            }
            return;
        }

        try {
            int orderId = Integer.parseInt(path.substring(1));
            Orders order = ordersDAO.findById(orderId);
            if (order == null) { ApiResponse.error(resp, "Order not found", 404); return; }
            ApiResponse.ok(resp, toDetail(order));
        } catch (NumberFormatException e) {
            resp.sendError(404);
        }
    }

    private int getAdminId(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        return JwtUtil.getUserId(header.substring(7));
    }

    static Integer parseDeliveryOverridePath(String path) {
        if (path == null || !path.matches("/\\d+/delivery-attempt-override")) return null;
        try {
            return Integer.valueOf(path.substring(1, path.indexOf('/', 1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int statusFor(OrderTransitionService.MutationResult result) {
        return switch (result) {
            case SUCCESS -> 200;
            case CONFLICT -> 409;
            case UNPROCESSABLE -> 422;
            case NOT_FOUND -> 404;
            case INVALID -> 400;
        };
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;
        String token = req.getHeader("Authorization").substring(7);

        String path = req.getPathInfo();
        if (path == null) { resp.sendError(404); return; }
        Integer overrideOrderId = parseDeliveryOverridePath(path);
        if (overrideOrderId != null) {
            Map<String, Object> body;
            try {
                body = JsonUtil.fromJson(req.getReader(), Map.class);
            } catch (RuntimeException e) {
                body = null;
            }
            Object rawExpectedStatus = body == null ? null : body.get("expectedStatus");
            Object rawNote = body == null ? null : body.get("note");
            String expectedStatus = rawExpectedStatus instanceof String value ? value.trim() : null;
            String note = rawNote instanceof String value ? DeliveryFailurePolicy.normalizeNote(value) : null;
            if (expectedStatus == null || expectedStatus.isEmpty() || note == null) {
                ApiResponse.error(resp, "Invalid delivery attempt override payload", 400);
                return;
            }
            if (ordersDAO.findById(overrideOrderId) == null) {
                ApiResponse.error(resp, "Order not found", 404);
                return;
            }
            OrderTransitionService.MutationResult result = transitionService.overrideDeliveryAttemptLimit(
                    overrideOrderId, JwtUtil.getUserId(token), expectedStatus, note);
            int status = statusFor(result);
            if (status == 200) ApiResponse.ok(resp, null, "Delivery attempt limit overridden");
            else ApiResponse.error(resp, status == 409 ? "Order changed" : "Cannot override delivery attempt limit", status);
            return;
        }

        String[] parts = path.split("/");
        if (parts.length != 3) { resp.sendError(404); return; }

        try {
            int orderId = Integer.parseInt(parts[1]);
            String action = parts[2];

            if ("notes".equals(action)) {
                Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
                String note = body != null ? (String) body.get("note") : null;
                if (note == null || note.isBlank()) { ApiResponse.error(resp, "Missing note", 400); return; }
                Orders order = ordersDAO.findById(orderId);
                if (order == null) { ApiResponse.error(resp, "Order not found", 404); return; }
                String existing = order.getInternalNote();
                order.setInternalNote(existing != null && !existing.isBlank() ? existing + "\n---\n[Admin] " + note : "[Admin] " + note);
                ordersDAO.save(order);
                ApiResponse.ok(resp, null, "Note saved");
            } else {
                resp.sendError(404);
            }
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid order ID", 400);
        } catch (Exception e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null) { resp.sendError(404); return; }

        String[] parts = path.split("/");
        if (parts.length != 3) { resp.sendError(404); return; }

        try {
            int orderId = Integer.parseInt(parts[1]);
            String action = parts[2];
            int adminId = "featured-review".equals(action) ? 0 : getAdminId(req);

            if ("cancel".equals(action)) {
                Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
                String reason = body != null ? (String) body.get("reason") : "Admin hủy đơn";
                boolean ok = transitionService.transition(orderId, "CANCELLED", "ADMIN", adminId, reason, null, null);
                if (!ok) { ApiResponse.error(resp, "Cannot cancel order", 400); return; }
                Orders order = ordersDAO.findById(orderId);
                if (order != null && order.getUser() != null) {
                }
                ApiResponse.ok(resp, null, "Order cancelled");
            } else if ("status".equals(action)) {
                Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
                String status = body != null ? (String) body.get("status") : null;
                if (status == null) { ApiResponse.error(resp, "Missing status", 400); return; }
                if (!OrderTransitionService.canUseGenericTransition(status)) { ApiResponse.error(resp, "Use cancel action", 400); return; }
                boolean ok = transitionService.transition(orderId, status, "ADMIN", adminId, body != null ? (String) body.get("note") : null);
                if (!ok) { ApiResponse.error(resp, "Invalid status transition", 400); return; }
                ApiResponse.ok(resp, null, "Status updated");
            } else if ("featured-review".equals(action)) {
                Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
                updateFeaturedReview(orderId, body, resp);
            } else {
                resp.sendError(404);
            }
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid order ID", 400);
        } catch (IllegalStateException e) {
            ApiResponse.error(resp, e.getMessage(), 422);
        } catch (Exception e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        }
    }

    protected void updateFeaturedReview(int orderId, Map<String, Object> body, HttpServletResponse resp) throws IOException {
        if (body == null || body.size() != 1 || !(body.get("featured") instanceof Boolean featured)) {
            ApiResponse.error(resp, "featured must be a boolean", 400);
            return;
        }
        try {
            ApiResponse.ok(resp, reviewService.setFeaturedByOrderId(orderId, featured), "Featured review updated");
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, "Review not found", 404);
        }
    }

    private Map<String, Object> toDetail(Orders o) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", o.getOrderId());
        data.put("orderCode", o.getOrderCode());
        data.put("status", o.getOrderStatus());
        data.put("customerName", o.getCustomerName());
        data.put("customerPhone", o.getCustomerPhone());
        data.put("customerAddress", o.getCustomerAddress());
        data.put("totalAmount", o.getTotalAmount());
        data.put("shippingFee", o.getShippingFee());
        data.put("serviceFee", o.getServiceFee());
        data.put("finalAmount", o.getFinalAmount());
        data.put("discountAmount", o.getDiscountAmount() != null ? o.getDiscountAmount() : java.math.BigDecimal.ZERO);
        data.put("paymentMethod", o.getPaymentMethod());
        data.put("paymentStatus", o.getPaymentStatus());
        data.put("deliveryNote", o.getDeliveryNote());
        data.put("cancelledBy", o.getCancelledBy());
        data.put("failureNote", o.getFailureReason());
        data.put("failureReason", o.getFailureReason());
        data.put("deliveryFailureCode", o.getDeliveryFailureCode());
        data.put("deliveryAttemptCount", o.getDeliveryAttemptCount());
        data.put("deliveryAttemptLimit", o.getDeliveryAttemptLimit());
        data.put("deliveryFailedAt", o.getDeliveryFailedAt() != null ? o.getDeliveryFailedAt().toString() : null);
        data.put("retryScheduledAt", o.getRetryScheduledAt() != null ? o.getRetryScheduledAt().toString() : null);
        data.put("returnedToStoreAt", o.getReturnedToStoreAt() != null ? o.getReturnedToStoreAt().toString() : null);
        data.put("refundStatus", o.getRefundStatus());
        data.put("refundAmount", o.getRefundAmount());
        data.put("refundNote", o.getRefundNote());
        data.put("refundedAt", o.getRefundedAt() != null ? o.getRefundedAt().toString() : null);
        data.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        data.put("confirmedAt", o.getConfirmedAt() != null ? o.getConfirmedAt().toString() : null);
        data.put("cancelledAt", o.getCancelledAt() != null ? o.getCancelledAt().toString() : null);
        data.put("deliveredAt", o.getDeliveredAt() != null ? o.getDeliveredAt().toString() : null);
        data.put("staffName", o.getStaff() != null ? o.getStaff().getFullName() : null);
        data.put("shipperName", o.getShipper() != null ? o.getShipper().getFullName() : null);
        data.put("internalNote", o.getInternalNote());
        data.put("review", reviewService.getAdminByOrderId(o.getOrderId()));

        PaymentAttempt attempt = paymentAttemptDAO.findByOrderId(o.getOrderId());
        if (attempt != null) {
            Map<String, Object> payment = new HashMap<>();
            payment.put("provider", attempt.getProvider());
            payment.put("providerReference", attempt.getProviderReference());
            payment.put("attemptStatus", attempt.getStatus());
            payment.put("attemptAmount", attempt.getAmount());
            payment.put("attemptUpdatedAt", attempt.getUpdatedAt() != null ? attempt.getUpdatedAt().toString() : null);
            data.put("payment", payment);
        } else {
            data.put("payment", null);
        }

        List<Map<String, Object>> items = orderItemDAO.findByOrderId(o.getOrderId())
                .stream().map(oi -> {
                    Map<String, Object> im = new HashMap<>();
                    im.put("productName", oi.getProductName());
                    im.put("variantName", oi.getVariantName() != null ? oi.getVariantName() : "");
                    im.put("quantity", oi.getQuantity());
                    im.put("unitPrice", oi.getUnitPrice());
                    im.put("totalPrice", oi.getTotalPrice());
                    im.put("imageUrl", oi.getProduct().getImageUrl() != null ? oi.getProduct().getImageUrl() : "");
                    return im;
                }).collect(Collectors.toList());
        data.put("items", items);

        var savedHistory = historyService.getByOrderId(o.getOrderId());
        data.put("statusHistory", savedHistory);
        OrderExpiryPolicy.Metadata metadata = OrderExpiryPolicy.metadata(o, LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
        data.put("statusEnteredAt", metadata.statusEnteredAt() == null ? null : metadata.statusEnteredAt().toString());
        data.put("expiresAt", metadata.expiresAt() == null ? null : metadata.expiresAt().toString());
        data.put("remainingSeconds", metadata.remainingSeconds());
        data.put("timeoutPolicy", metadata.timeoutPolicy());
        data.put("ownerShiftCode", o.getStaffShift() == null ? null : o.getStaffShift().getShiftCode());

        return data;
    }
}
