package servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import service.AdminOrderAttentionPolicy;
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
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
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

    private static final java.util.Set<String> ORDER_STATUSES = java.util.Set.of("PENDING", "CONFIRMED", "PREPARING", "READY", "ASSIGNED", "PICKED_UP", "DELIVERY_FAILED", "RETURNED_TO_STORE", "DELIVERED", "CANCELLED");
    private static final java.util.Set<String> PAYMENT_STATUSES = java.util.Set.of("UNPAID", "PAID", "FAILED", "REFUNDED");
    private static final java.util.Set<String> REFUND_STATUSES = java.util.Set.of("PENDING", "REFUNDED", "REJECTED");

    public List<Map<String, Object>> getOrdersData() {
        List<Orders> orders = ordersDAO.findAll();
        Map<Integer,Integer> counts = orderItemDAO.countItemsByOrderIds(orders.stream().map(Orders::getOrderId).toList());
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        return orders.stream().map(order -> toListItem(order, counts.getOrDefault(order.getOrderId(), 0), now)).collect(Collectors.toList());
    }

    private Map<String, Object> toListItem(Orders o, int itemCount, LocalDateTime now) {
            Map<String, Object> m = new HashMap<>();
            m.put("orderId", o.getOrderId());
            m.put("orderCode", o.getOrderCode());
            m.put("status", o.getOrderStatus());
            m.put("customerName", o.getCustomerName());
            m.put("paymentMethod", o.getPaymentMethod());
            m.put("paymentStatus", o.getPaymentStatus());
            m.put("itemCount", itemCount);
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
            m.put("attentionReasons", AdminOrderAttentionPolicy.reasons(o, now));
            LocalDateTime entered = o.getStatusEnteredAt() != null ? o.getStatusEnteredAt() : o.getCreatedAt();
            m.put("waitingMinutes", entered == null ? 0L : Math.max(0L, java.time.Duration.between(entered, now).toMinutes()));
            m.put("allowedActions", transitionService.getAllowedActions(o.getOrderStatus(), "ADMIN", o.getPaymentStatus()));
            return m;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            try {
                OrdersDAO.AdminOrderQuery query = adminOrderQuery(req);
                OrdersDAO.OrdersPageResult result = ordersDAO.findAdminQueue(query);
                Map<Integer,Integer> counts = orderItemDAO.countItemsByOrderIds(result.items().stream().map(Orders::getOrderId).toList());
                LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
                List<Map<String,Object>> items = result.items().stream()
                        .map(order -> toListItem(order, counts.getOrDefault(order.getOrderId(), 0), now)).toList();
                long totalPages = result.totalItems() == 0 ? 0 : (result.totalItems() + result.pageSize() - 1) / result.pageSize();
                ApiResponse.ok(resp, Map.of("items", items, "pagination", Map.of(
                        "page", result.page(), "pageSize", result.pageSize(),
                        "totalItems", result.totalItems(), "totalPages", totalPages)));
            } catch (DateTimeParseException e) {
                ApiResponse.error(resp, "Invalid date format, expected yyyy-MM-dd", 400);
            } catch (IllegalArgumentException e) {
                ApiResponse.error(resp, e.getMessage(), 400);
            } catch (RuntimeException e) {
                ApiResponse.error(resp, "Internal server error", 500);
            }
            return;
        }

        try {
            if (!path.matches("/[1-9]\\d*")) { ApiResponse.error(resp, "Not found", 404); return; }
            int orderId = Integer.parseInt(path.substring(1));
            Orders order = ordersDAO.findById(orderId);
            if (order == null) { ApiResponse.error(resp, "Order not found", 404); return; }
            ApiResponse.ok(resp, toDetail(order));
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Not found", 404);
        } catch (RuntimeException e) {
            ApiResponse.error(resp, "Internal server error", 500);
        }
    }

    static boolean parseAttentionOnly(String value) {
        if (value == null || value.isBlank() || "false".equals(value)) return false;
        if ("true".equals(value)) return true;
        throw new IllegalArgumentException("attentionOnly must be true or false");
    }

    static OrdersDAO.AdminOrderQuery adminOrderQuery(HttpServletRequest req) {
        String search = optional(req.getParameter("search"));
        String status = enumValue(req.getParameter("status"), ORDER_STATUSES, "status");
        String paymentStatus = enumValue(req.getParameter("paymentStatus"), PAYMENT_STATUSES, "paymentStatus");
        String refundStatus = enumValue(req.getParameter("refundStatus"), REFUND_STATUSES, "refundStatus");
        boolean attentionOnly = parseAttentionOnly(req.getParameter("attentionOnly"));
        LocalDate from = date(req.getParameter("fromDate"));
        LocalDate to = date(req.getParameter("toDate"));
        if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("fromDate must not be after toDate");
        String sort = optional(req.getParameter("sort"));
        if (sort == null) sort = "WAITING_DESC";
        if (!java.util.Set.of("WAITING_DESC", "CREATED_DESC").contains(sort)) throw new IllegalArgumentException("Invalid sort");
        int page = positiveInt(req.getParameter("page"), 1, Integer.MAX_VALUE, 1, "page");
        int pageSize = positiveInt(req.getParameter("pageSize"), 1, 100, 20, "pageSize");
        return new OrdersDAO.AdminOrderQuery(search, status, paymentStatus, refundStatus,
                from == null ? null : from.atStartOfDay(), to == null ? null : to.plusDays(1).atStartOfDay(),
                attentionOnly, LocalDateTime.now(BUSINESS_ZONE), sort, page, pageSize);
    }

    private static String optional(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static String enumValue(String value, java.util.Set<String> allowed, String name) {
        String normalized = optional(value);
        if (normalized != null && !allowed.contains(normalized)) throw new IllegalArgumentException("Invalid " + name);
        return normalized;
    }
    private static LocalDate date(String value) { String normalized = optional(value); return normalized == null ? null : LocalDate.parse(normalized); }
    private static int positiveInt(String value, int minimum, int maximum, int fallback, String name) {
        if (value == null || value.isBlank()) return fallback;
        try { int parsed = Integer.parseInt(value); if (parsed < minimum || parsed > maximum) throw new IllegalArgumentException("Invalid " + name); return parsed; }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid " + name); }
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
                if (body == null || !body.keySet().equals(java.util.Set.of("expectedStatus", "note"))
                        || !(body.get("expectedStatus") instanceof String expectedStatus)
                        || !(body.get("note") instanceof String note) || note.isBlank()) {
                    ApiResponse.error(resp, "Invalid note payload", 400); return;
                }
                OrderTransitionService.MutationResult result = transitionService.appendAdminNote(orderId, JwtUtil.getUserId(token), expectedStatus, note);
                int status = statusFor(result);
                if (status == 200) ApiResponse.ok(resp, null, "Note saved");
                else ApiResponse.error(resp, status == 409 ? "Order changed" : status == 404 ? "Order not found" : "Cannot save note", status);
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
                if (body == null || !body.keySet().equals(java.util.Set.of("expectedStatus", "reason"))
                        || !(body.get("expectedStatus") instanceof String expectedStatus)
                        || !(body.get("reason") instanceof String reason) || reason.isBlank()) {
                    ApiResponse.error(resp, "Invalid cancel payload", 400); return;
                }
                OrderTransitionService.MutationResult result = transitionService.transition(orderId, "CANCELLED", "ADMIN", adminId, reason, null, null, expectedStatus);
                int resultStatus = statusFor(result);
                if (resultStatus == 200) ApiResponse.ok(resp, null, "Order cancelled");
                else ApiResponse.error(resp, resultStatus == 409 ? "Order changed" : resultStatus == 404 ? "Order not found" : "Cannot cancel order", resultStatus);
            } else if ("status".equals(action)) {
                Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
                if (body == null || !java.util.Set.of("expectedStatus", "status", "note").containsAll(body.keySet())
                        || body.size() < 2 || !(body.get("expectedStatus") instanceof String expectedStatus)
                        || !(body.get("status") instanceof String status)
                        || body.containsKey("note") && body.get("note") != null && !(body.get("note") instanceof String)) {
                    ApiResponse.error(resp, "Invalid status payload", 400); return;
                }
                if (!OrderTransitionService.canUseGenericTransition(status)) { ApiResponse.error(resp, "Use dedicated action", 400); return; }
                String note = body.get("note") instanceof String value ? value : null;
                OrderTransitionService.MutationResult result = transitionService.transition(orderId, status, "ADMIN", adminId, note, null, null, expectedStatus);
                int resultStatus = statusFor(result);
                if (resultStatus == 200) ApiResponse.ok(resp, null, "Status updated");
                else ApiResponse.error(resp, resultStatus == 409 ? "Order changed" : resultStatus == 404 ? "Order not found" : "Invalid status transition", resultStatus);
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
        data.put("allowedActions", transitionService.getAllowedActions(o.getOrderStatus(), "ADMIN", o.getPaymentStatus()));

        return data;
    }
}
