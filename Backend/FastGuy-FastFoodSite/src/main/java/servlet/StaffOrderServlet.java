package servlet;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.OrderItemDAO;
import dao.OrdersDAO;
import entity.Orders;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.DeliveryFailurePolicy;
import service.OrderTransitionService;
import service.StaffOrderService;
import service.StaffShiftAccessService;
import utils.ApiResponse;
import utils.JwtUtil;

@WebServlet("/api/staff/orders/*")
public class StaffOrderServlet extends HttpServlet {
    public static final String CONFLICT_MESSAGE = "Đơn hàng đã được cập nhật. Vui lòng thử lại.";
    private StaffOrderService staffOrderService = new StaffOrderService();
    private OrderItemDAO orderItemDAO = new OrderItemDAO();
    private OrdersDAO ordersDAO = new OrdersDAO();
    private service.OrderStatusHistoryService orderStatusHistoryService = new service.OrderStatusHistoryService();
    private StaffShiftAccessService staffShiftAccessService = new StaffShiftAccessService();
    private ObjectMapper mapper = new ObjectMapper();

    record DeliveryMutationPath(int orderId, String action) {}

    static DeliveryMutationPath parseDeliveryMutationPath(String path) {
        if (path == null || !path.matches("/\\d+/(retry-delivery|start-scheduled-retry|return-to-store)")) return null;
        String[] parts = path.split("/");
        try {
            return new DeliveryMutationPath(Integer.parseInt(parts[1]), parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static Map<String, Object> validateRetryPayload(Map<String, Object> body) {
        if (body == null || !(body.get("expectedStatus") instanceof String expectedStatus)
                || !(body.get("shipperId") instanceof Number shipperId) || !(body.get("retryMode") instanceof String retryMode)) return null;
        int parsedShipperId;
        try {
            parsedShipperId = new java.math.BigDecimal(shipperId.toString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            return null;
        }
        expectedStatus = expectedStatus.trim();
        retryMode = retryMode.trim();
        if (parsedShipperId < 1 || !"DELIVERY_FAILED".equals(expectedStatus) || !("IMMEDIATE".equals(retryMode) || "SCHEDULED".equals(retryMode))) return null;
        LocalDateTime scheduledAt = null;
        if (body.get("scheduledAt") != null) {
            if (!(body.get("scheduledAt") instanceof String rawScheduledAt)) return null;
            try {
                scheduledAt = LocalDateTime.parse(rawScheduledAt.trim());
            } catch (DateTimeParseException e) {
                return null;
            }
        }
        if ("SCHEDULED".equals(retryMode) && scheduledAt == null) return null;
        String note = body.get("note") instanceof String rawNote ? DeliveryFailurePolicy.normalizeNote(rawNote) : null;
        if (note == null) return null;
        Map<String, Object> result = new HashMap<>();
        result.put("expectedStatus", expectedStatus);
        result.put("shipperId", parsedShipperId);
        result.put("retryMode", retryMode);
        result.put("scheduledAt", scheduledAt);
        result.put("note", note);
        return result;
    }

    private static int statusFor(OrderTransitionService.MutationResult result) {
        return switch (result) {
            case SUCCESS -> 200;
            case CONFLICT -> 409;
            case UNPROCESSABLE -> 422;
            case INVALID -> 400;
        };
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int staffId = getStaffId(req, resp);
        if (staffId < 0 || !requireCheckedInShift(req, resp, staffId)) return;
        String path = req.getPathInfo();
        DeliveryMutationPath mutation = parseDeliveryMutationPath(path);
        if (mutation != null) {
            Map<String, Object> body;
            try {
                body = mapper.readValue(req.getReader(), new TypeReference<Map<String, Object>>() {});
            } catch (IOException | RuntimeException e) {
                body = null;
            }
            if (staffOrderService.getOrderDetail(mutation.orderId()) == null) {
                ApiResponse.error(resp, "Order not found", 404);
                return;
            }
            OrderTransitionService.MutationResult result;
            if ("retry-delivery".equals(mutation.action())) {
                Map<String, Object> payload = validateRetryPayload(body);
                if (payload == null) { ApiResponse.error(resp, "Invalid retry payload", 400); return; }
                result = staffOrderService.retryDelivery(mutation.orderId(), staffId, (String) payload.get("expectedStatus"),
                        (Integer) payload.get("shipperId"), (String) payload.get("retryMode"),
                        (LocalDateTime) payload.get("scheduledAt"), (String) payload.get("note"));
            } else {
                Object rawExpectedStatus = body == null ? null : body.get("expectedStatus");
                String expectedStatus = rawExpectedStatus instanceof String value ? value.trim() : null;
                if (!"DELIVERY_FAILED".equals(expectedStatus)) { ApiResponse.error(resp, "Invalid expectedStatus", 400); return; }
                if ("start-scheduled-retry".equals(mutation.action())) {
                    result = staffOrderService.startScheduledRetry(mutation.orderId(), staffId, expectedStatus);
                } else {
                    Object rawNote = body.get("note");
                    String note = rawNote instanceof String value ? DeliveryFailurePolicy.normalizeNote(value) : null;
                    if (note == null) { ApiResponse.error(resp, "Invalid note", 400); return; }
                    result = staffOrderService.returnToStore(mutation.orderId(), staffId, expectedStatus, note);
                }
            }
            int status = statusFor(result);
            if (status == 200) ApiResponse.ok(resp, null, "Delivery failure updated");
            else ApiResponse.error(resp, status == 409 ? CONFLICT_MESSAGE : "Cannot update delivery failure", status);
        } else if (path != null && path.contains("/notes")) {
            String orderIdStr = path.substring(1, path.indexOf("/notes"));
            try {
                int orderId = Integer.parseInt(orderIdStr);
                java.util.Map<String, Object> body = utils.JsonUtil.fromJson(req.getReader(), java.util.Map.class);
                Object rawNote = body != null ? body.get("note") : null;
                String note = rawNote instanceof String ? (String) rawNote : null;
                Orders order = ordersDAO.findById(orderId);
                String validationError = validateNote(note, order != null ? order.getInternalNote() : null, order != null);
                if (validationError != null) {
                    ApiResponse.error(resp, validationError, "Order not found".equals(validationError) ? 404 : 400);
                    return;
                }
                order.setInternalNote(appendNote(order.getInternalNote(), note));
                ordersDAO.save(order);
                ApiResponse.ok(resp, null, "Note saved");
            } catch (NumberFormatException e) {
                ApiResponse.error(resp, "Invalid order ID", 400);
            }
        } else {
            resp.sendError(404);
        }
    }

    static String validateNote(String note, String existing, boolean orderExists) {
        if (note == null || note.isBlank()) return "Note is required";
        String trimmed = note.trim();
        if (trimmed.length() > 1000) return "Note is too long";
        if (!orderExists) return "Order not found";
        return appendNote(existing, trimmed).length() <= 1000 ? null : "Note is too long";
    }

    static String appendNote(String existing, String note) {
        String trimmed = note.trim();
        return existing == null || existing.isBlank() ? trimmed : existing + "\n---\n" + trimmed;
    }

    public static boolean requiresCheckedInShift(String method, String pathInfo) {
        return !("GET".equals(method) && ("/history".equals(pathInfo) || "/export".equals(pathInfo)));
    }

    public static boolean hasRouteAccess(String method, String pathInfo, boolean validIdentity, boolean checkedIn) {
        return validIdentity && (!requiresCheckedInShift(method, pathInfo) || checkedIn);
    }

    private boolean requireCheckedInShift(HttpServletRequest req, HttpServletResponse resp, int staffId) throws IOException {
        boolean validIdentity = staffShiftAccessService.hasValidStaffIdentity(staffId);
        if (!validIdentity) {
            ApiResponse.error(resp, "Forbidden", 403);
            return false;
        }
        if (!requiresCheckedInShift(req.getMethod(), req.getPathInfo()) || staffShiftAccessService.hasCheckedInShift(staffId)) return true;
        ApiResponse.error(resp, "Checked-in shift required", 403);
        return false;
    }

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
        if (staffId < 0 || !requireCheckedInShift(req, resp, staffId)) return;

        try {
            String path = req.getPathInfo();

            if ("/shippers".equals(path)) {
                List<entity.WorkShift> shifts = staffOrderService.getAvailableShipperShifts();
                List<Map<String, Object>> result = shifts.stream().map(shift -> {
                    entity.User user = shift.getUser();
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", user.getUserId());
                    m.put("fullName", user.getFullName());
                    m.put("phone", user.getPhone());
                    m.put("activeOrderCount", staffOrderService.countActiveOrders(user.getUserId(), shift.getCheckInAt()));
                    return m;
                }).collect(Collectors.toList());
                ApiResponse.ok(resp, result);
                return;
            }

            if (path == null || path.equals("/")) {
                List<Orders> orders = staffOrderService.getPendingOrders();
                List<Map<String, Object>> result = orders.stream().map(o -> toListItem(o)).collect(Collectors.toList());
                ApiResponse.ok(resp, result);
            } else if (path.equals("/confirmed")) {
                List<Orders> orders = staffOrderService.getConfirmedOrders();
                List<Map<String, Object>> result = orders.stream().map(o -> toListItem(o)).collect(Collectors.toList());
                ApiResponse.ok(resp, result);
            } else if (path.equals("/preparing")) {
                List<Orders> orders = staffOrderService.getPreparingOrders();
                List<Map<String, Object>> result = orders.stream().map(o -> toListItem(o)).collect(Collectors.toList());
                ApiResponse.ok(resp, result);
            } else if (path.equals("/ready")) {
                List<Orders> orders = staffOrderService.getReadyOrders();
                List<Map<String, Object>> result = orders.stream().map(o -> toListItem(o)).collect(Collectors.toList());
                ApiResponse.ok(resp, result);
            } else if (path.equals("/delivery-failures")) {
                ApiResponse.ok(resp, staffOrderService.getDeliveryFailureQueue().stream().map(o -> toFailureQueueItem(o, toListItem(o))).collect(Collectors.toList()));
            } else if (path.equals("/history")) {
                HistoryFilter filter = getHistoryFilter(req);
                List<Orders> orders = ordersDAO.findStaffHistory(filter.page(), filter.size(), filter.status(), filter.from(), filter.to(), filter.search());
                long total = ordersDAO.countStaffHistory(filter.status(), filter.from(), filter.to(), filter.search());
                long totalPages = total == 0 ? 0 : (total + filter.size() - 1) / filter.size();
                Map<String, Object> result = new HashMap<>();
                result.put("items", orders.stream().map(this::toListItem).collect(Collectors.toList()));
                result.put("total", total);
                result.put("page", filter.page());
                result.put("size", filter.size());
                result.put("totalPages", totalPages);
                ApiResponse.ok(resp, result);
            } else if (path.equals("/export")) {
                HistoryFilter filter = getHistoryFilter(req);
                resp.setContentType("text/csv;charset=UTF-8");
                resp.setHeader("Content-Disposition", "attachment; filename=staff-order-history.csv");
                List<Orders> orders = ordersDAO.findStaffHistoryForExport(filter.status(), filter.from(), filter.to(), filter.search());
                var writer = resp.getWriter();
                writer.write("\uFEFF");
                writer.write("Mã đơn;Trạng thái;Khách hàng;Số điện thoại;Tổng tiền;Thời điểm kết thúc\r\n");
                for (Orders order : orders) {
                    LocalDateTime endedAt = order.getDeliveredAt() != null ? order.getDeliveredAt() : order.getCancelledAt();
                    writer.write(String.join(";",
                            csvCell(order.getOrderCode()), csvCell(order.getOrderStatus()), csvCell(order.getCustomerName()),
                            csvCell(order.getCustomerPhone()), csvCell(order.getFinalAmount()), csvCell(endedAt)) + "\r\n");
                }
                writer.flush();
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
        } catch (IllegalArgumentException | DateTimeParseException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse.error(resp, "Internal error: " + e.getMessage(), 500);
        }
    }

    private HistoryFilter getHistoryFilter(HttpServletRequest req) {
        int page = parsePositiveInt(req.getParameter("page"), 1, "page must be positive");
        int size = parsePositiveInt(req.getParameter("size"), 20, "pageSize must be between 1 and 100");
        if (size > 100) throw new IllegalArgumentException("pageSize must be between 1 and 100");
        String status = normalize(req.getParameter("status"));
        if (status != null && !"DELIVERED".equals(status) && !"CANCELLED".equals(status)) throw new IllegalArgumentException("Invalid status");
        String search = normalize(req.getParameter("search"));
        if (search != null && search.length() > 100) throw new IllegalArgumentException("Search is too long");
        LocalDate fromDate = parseDate(req.getParameter("from"));
        LocalDate toDate = parseDate(req.getParameter("to"));
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) throw new IllegalArgumentException("from must not be after to");
        return new HistoryFilter(page, size, status, fromDate != null ? fromDate.atStartOfDay() : null,
                toDate != null ? toDate.plusDays(1).atStartOfDay() : null, search);
    }

    private int parsePositiveInt(String raw, int fallback, String message) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            int value = Integer.parseInt(raw);
            if (value < 1) throw new IllegalArgumentException(message);
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(message);
        }
    }

    private LocalDate parseDate(String raw) {
        return raw == null || raw.isBlank() ? null : LocalDate.parse(raw);
    }

    private String normalize(String raw) {
        if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw)) return null;
        return raw.trim();
    }

    static String csvCell(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        String trimmed = text.stripLeading();
        if (trimmed.startsWith("=") || trimmed.startsWith("+") || trimmed.startsWith("-") || trimmed.startsWith("@")) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private record HistoryFilter(int page, int size, String status, LocalDateTime from, LocalDateTime to, String search) {}

    public static int statusForAssignment(OrderTransitionService.MutationResult result) {
        return switch (result) {
            case SUCCESS -> 200;
            case CONFLICT -> 409;
            case UNPROCESSABLE -> 422;
            case INVALID -> 400;
        };
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int staffId = getStaffId(req, resp);
        if (staffId < 0 || !requireCheckedInShift(req, resp, staffId)) return;

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

        int orderId;
        try {
            orderId = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid order ID", 400);
            return;
        }
        String action = parts[2];

        if ("status".equals(action)) {
            Map<String, Object> body = utils.JsonUtil.fromJson(req.getReader(), Map.class);
            if (body == null) {
                ApiResponse.error(resp, "Invalid data", 400);
                return;
            }
            Object rawStatus = body.get("status");
            String status = rawStatus instanceof String ? (String) rawStatus : null;
            if (status == null || (!"CONFIRMED".equals(status) && !"PREPARING".equals(status) && !"READY".equals(status) && !"CANCELLED".equals(status))) {
                ApiResponse.error(resp, "Invalid status", 400);
                return;
            }
            Object rawExpectedStatus = body.get("expectedStatus");
            String expectedStatus = rawExpectedStatus instanceof String ? (String) rawExpectedStatus : null;
            if (expectedStatus == null) {
                ApiResponse.error(resp, "Missing expectedStatus", 400);
                return;
            }
            Object rawReason = body.get("failureReason");
            String failureReason = rawReason instanceof String ? (String) rawReason : null;
            if ("CONFIRMED".equals(status)) {
                Orders order = staffOrderService.getOrderDetail(orderId);
                if (order != null && "BANK_TRANSFER".equals(order.getPaymentMethod()) && !"PAID".equals(order.getPaymentStatus())) {
                    ApiResponse.error(resp, "Đơn chuyển khoản chưa thanh toán", 400);
                    return;
                }
            }
            OrderTransitionService.MutationResult result = staffOrderService.updateStatus(orderId, status, staffId, failureReason, expectedStatus);
            if (result == OrderTransitionService.MutationResult.CONFLICT) {
                ApiResponse.error(resp, CONFLICT_MESSAGE, 409);
                return;
            }
            if (result != OrderTransitionService.MutationResult.SUCCESS) {
                ApiResponse.error(resp, "Cannot update status: invalid transition", 400);
                return;
            }
            ApiResponse.ok(resp, null, "Status updated");
        } else if ("assign-shipper".equals(action)) {
            Map<String, Object> body = utils.JsonUtil.fromJson(req.getReader(), Map.class);
            if (body == null || !body.containsKey("shipperId")) {
                ApiResponse.error(resp, "Missing shipperId", 400);
                return;
            }
            if (!(body.get("shipperId") instanceof Number)) {
                ApiResponse.error(resp, "Invalid shipperId", 400);
                return;
            }
            int shipperId = ((Number) body.get("shipperId")).intValue();
            Object rawExpectedStatus = body.get("expectedStatus");
            String expectedStatus = rawExpectedStatus instanceof String ? (String) rawExpectedStatus : null;
            if (expectedStatus == null) {
                ApiResponse.error(resp, "Missing expectedStatus", 400);
                return;
            }
            OrderTransitionService.MutationResult result = staffOrderService.assignShipper(orderId, shipperId, staffId, expectedStatus);
            int status = statusForAssignment(result);
            if (status == 409) {
                ApiResponse.error(resp, CONFLICT_MESSAGE, status);
                return;
            }
            if (status == 422) {
                ApiResponse.error(resp, "Shipper is no longer in an active checked-in shift", status);
                return;
            }
            if (status != 200) {
                ApiResponse.error(resp, "Cannot assign shipper", status);
                return;
            }
            ApiResponse.ok(resp, null, "Shipper assigned");
        } else {
            resp.sendError(404);
        }
    }

    static Map<String, Object> toFailureQueueItem(Orders o) {
        return toFailureQueueItem(o, new HashMap<>());
    }

    static Map<String, Object> toFailureQueueItem(Orders o, Map<String, Object> commonFields) {
        Map<String, Object> m = new HashMap<>(commonFields);
        m.put("deliveryAttemptCount", o.getDeliveryAttemptCount());
        m.put("deliveryAttemptLimit", o.getDeliveryAttemptLimit());
        m.put("deliveryFailureCode", o.getDeliveryFailureCode());
        m.put("failureNote", o.getFailureReason());
        m.put("deliveryFailedAt", o.getDeliveryFailedAt() != null ? o.getDeliveryFailedAt().toString() : null);
        m.put("retryScheduledAt", o.getRetryScheduledAt() != null ? o.getRetryScheduledAt().toString() : null);
        m.put("returnedToStoreAt", o.getReturnedToStoreAt() != null ? o.getReturnedToStoreAt().toString() : null);
        return m;
    }

    private Map<String, Object> toListItem(Orders o) {
        Map<String, Object> m = new HashMap<>();
        m.put("orderId", o.getOrderId());
        m.put("orderCode", o.getOrderCode());
        m.put("userId", o.getUser() != null ? o.getUser().getUserId() : null);
        m.put("customerName", o.getCustomerName());
        m.put("customerPhone", o.getCustomerPhone());
        m.put("status", o.getOrderStatus());
        m.put("orderStatus", o.getOrderStatus());
        var orderItems = orderItemDAO.findByOrderId(o.getOrderId());
        m.put("itemCount", orderItems.stream().mapToInt(oi -> oi.getQuantity()).sum());
        m.put("items", orderItems.stream().map(oi -> {
            Map<String, Object> im = new HashMap<>();
            im.put("productId", oi.getProduct() != null ? oi.getProduct().getProductId() : null);
            im.put("variantId", oi.getVariant() != null ? oi.getVariant().getVariantId() : null);
            im.put("productName", oi.getProductName());
            im.put("variantName", oi.getVariantName() != null ? oi.getVariantName() : "");
            im.put("quantity", oi.getQuantity());
            im.put("unitPrice", oi.getUnitPrice());
            im.put("totalPrice", oi.getTotalPrice());
            im.put("imageUrl", oi.getProduct() != null && oi.getProduct().getImageUrl() != null ? oi.getProduct().getImageUrl() : "");
            im.put("modifiers", oi.getModifiers());
            return im;
        }).collect(Collectors.toList()));
        m.put("totalAmount", o.getTotalAmount());
        m.put("shippingFee", o.getShippingFee());
        m.put("serviceFee", o.getServiceFee());
        m.put("discountAmount", o.getDiscountAmount());
        m.put("paymentMethod", o.getPaymentMethod());
        m.put("paymentStatus", o.getPaymentStatus());
        m.put("finalAmount", o.getFinalAmount());
        m.put("refundAmount", o.getRefundAmount());
        m.put("refundedAt", o.getRefundedAt());
        m.put("shipperId", o.getShipper() != null ? o.getShipper().getUserId() : null);
        m.put("shipperName", o.getShipper() != null ? o.getShipper().getFullName() : null);
        m.put("assignedAt", o.getAssignedAt() != null ? o.getAssignedAt().toString() : null);
        m.put("updatedAt", o.getUpdatedAt() != null ? o.getUpdatedAt().toString() : null);
        LocalDateTime endedAt = o.getDeliveredAt() != null ? o.getDeliveredAt() : o.getCancelledAt();
        m.put("endedAt", endedAt != null ? endedAt.toString() : null);
        m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> toDetail(Orders o) {
        Map<String, Object> m = new HashMap<>();
        m.put("orderId", o.getOrderId());
        m.put("orderCode", o.getOrderCode());
        m.put("userId", o.getUser() != null ? o.getUser().getUserId() : null);
        m.put("customerName", o.getCustomerName());
        m.put("customerPhone", o.getCustomerPhone());
        m.put("status", o.getOrderStatus());
        m.put("orderStatus", o.getOrderStatus());
        m.put("totalAmount", o.getTotalAmount());
        m.put("shippingFee", o.getShippingFee());
        m.put("serviceFee", o.getServiceFee());
        m.put("discountAmount", o.getDiscountAmount());
        m.put("finalAmount", o.getFinalAmount());
        m.put("codCollectedAmount", o.getCodCollectedAmount());
        m.put("codCollectedAt", o.getCodCollectedAt() != null ? o.getCodCollectedAt().toString() : null);
        m.put("paymentMethod", o.getPaymentMethod());
        m.put("paymentStatus", o.getPaymentStatus());
        m.put("cancelledBy", o.getCancelledBy());
        m.put("refundStatus", o.getRefundStatus());
        m.put("refundAmount", o.getRefundAmount());
        m.put("refundedAt", o.getRefundedAt() != null ? o.getRefundedAt().toString() : null);
        m.put("refundNote", o.getRefundNote());
        m.put("failureNote", o.getFailureReason());
        m.put("deliveryFailureCode", o.getDeliveryFailureCode());
        m.put("deliveryAttemptCount", o.getDeliveryAttemptCount());
        m.put("deliveryAttemptLimit", o.getDeliveryAttemptLimit());
        m.put("deliveryFailedAt", o.getDeliveryFailedAt() != null ? o.getDeliveryFailedAt().toString() : null);
        m.put("retryScheduledAt", o.getRetryScheduledAt() != null ? o.getRetryScheduledAt().toString() : null);
        m.put("returnedToStoreAt", o.getReturnedToStoreAt() != null ? o.getReturnedToStoreAt().toString() : null);
        m.put("shipperId", o.getShipper() != null ? o.getShipper().getUserId() : null);
        m.put("shipperName", o.getShipper() != null ? o.getShipper().getFullName() : null);
        m.put("assignedAt", o.getAssignedAt() != null ? o.getAssignedAt().toString() : null);
        m.put("customerAddress", o.getCustomerAddress());
        m.put("deliveryNote", o.getDeliveryNote());
        m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        m.put("updatedAt", o.getUpdatedAt() != null ? o.getUpdatedAt().toString() : null);

        List<Map<String, Object>> items = orderItemDAO.findByOrderId(o.getOrderId())
                .stream()
                .map(oi -> {
                    Map<String, Object> im = new HashMap<>();
                    im.put("productId", oi.getProduct() != null ? oi.getProduct().getProductId() : null);
                    im.put("variantId", oi.getVariant() != null ? oi.getVariant().getVariantId() : null);
                    im.put("productName", oi.getProductName());
                    im.put("variantName", oi.getVariantName() != null ? oi.getVariantName() : "");
                    im.put("quantity", oi.getQuantity());
                    im.put("unitPrice", oi.getUnitPrice());
                    im.put("totalPrice", oi.getTotalPrice());
                    im.put("imageUrl", oi.getProduct() != null && oi.getProduct().getImageUrl() != null ? oi.getProduct().getImageUrl() : "");
            im.put("modifiers", oi.getModifiers());
                    return im;
                })
                .collect(Collectors.toList());
        m.put("items", items);
        m.put("itemCount", items.stream().mapToInt(item -> ((Number) item.get("quantity")).intValue()).sum());

        List<Map<String, Object>> history = new java.util.ArrayList<>();
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
        m.put("statusHistory", savedHistory.isEmpty() ? history : savedHistory);
        String noteRaw = o.getInternalNote();
        List<Map<String, String>> notes = new java.util.ArrayList<>();
        if (noteRaw != null && !noteRaw.isBlank()) {
            for (String block : noteRaw.split("\n---\n")) {
                String trimmed = block.trim();
                if (!trimmed.isEmpty()) {
                    Map<String, String> nm = new HashMap<>();
                    nm.put("content", trimmed);
                    notes.add(nm);
                }
            }
        }
        m.put("internalNotes", notes);

        OrderTransitionService transitionService = new OrderTransitionService();
        m.put("allowedActions", transitionService.getAllowedActions(o.getOrderStatus(), "STAFF", o.getPaymentStatus()));

        return m;
    }
}
