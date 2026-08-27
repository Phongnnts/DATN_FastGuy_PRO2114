package servlet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import entity.Orders;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import dao.OrderItemDAO;
import dao.UserDAO;
import service.OrderTransitionService;
import service.ShipperShiftAccessService;
import service.ShipperService;
import utils.ApiResponse;
import utils.JwtUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/shipper/*")
public class ShipperServlet extends HttpServlet {
    static final String CONFLICT_MESSAGE = "Đơn hàng đã thay đổi trạng thái. Dữ liệu mới nhất đã được tải lại.";
    private ShipperService shipperService = new ShipperService();
    private ShipperShiftAccessService shipperShiftAccessService = new ShipperShiftAccessService();
    private OrderItemDAO orderItemDAO = new OrderItemDAO();
    private UserDAO userDAO = new UserDAO();
    private ObjectMapper mapper = new ObjectMapper();

    private int getShipperId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return -1;
        }
        String token = authHeader.substring(7);
        int userId = JwtUtil.getUserId(token);
        if (userId < 0) {
            ApiResponse.error(resp, "Invalid token", 401);
            return -1;
        }
        String role = JwtUtil.getRole(token);
        if (!"SHIPPER".equals(role)) {
            ApiResponse.error(resp, "Forbidden", 403);
            return -1;
        }
        entity.User shipper = userDAO.findById(userId);
        if (shipper == null || !isActiveShipper(shipper.getRole(), shipper.getStatus())) {
            ApiResponse.error(resp, "Inactive shipper", 403);
            return -1;
        }
        return userId;
    }

    static boolean isActiveShipper(String role, String status) {
        return "SHIPPER".equals(role) && "ACTIVE".equals(status);
    }

    static Integer parseDetailOrderId(String path) {
        if (path == null || !path.matches("/orders/\\d+")) return null;
        try {
            return Integer.valueOf(path.substring("/orders/".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static MutationPath parseMutationPath(String path) {
        if (path == null || !path.matches("/orders/\\d+/(pickup|deliver|fail)")) return null;
        String[] segments = path.split("/");
        try {
            return new MutationPath(Integer.parseInt(segments[2]), segments[3]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    record MutationPath(int orderId, String action) {}

    static boolean isValidExpectedStatus(String status) {
        return status != null && !status.isBlank() && OrderTransitionService.isCanonicalStatus(status);
    }

    static Map<String, Object> validateFailurePayload(Map<String, Object> body) {
        if (body == null || !(body.get("expectedStatus") instanceof String expectedStatus)
                || !(body.get("reasonCode") instanceof String reasonCode) || !(body.get("note") instanceof String note)) return null;
        expectedStatus = expectedStatus.trim();
        reasonCode = reasonCode.trim();
        note = note.trim();
        if (!isValidExpectedStatus(expectedStatus) || reasonCode.isEmpty() || note.isEmpty()) return null;
        Map<String, Object> result = new HashMap<>();
        result.put("expectedStatus", expectedStatus);
        result.put("reasonCode", reasonCode);
        result.put("note", note);
        return result;
    }

    static int statusFor(OrderTransitionService.MutationResult result) {
        return switch (result) {
            case SUCCESS -> 200;
            case CONFLICT -> 409;
            case UNPROCESSABLE -> 422;
            case NOT_FOUND -> 404;
            case INVALID -> 400;
        };
    }

    static int ownershipStatus(Orders order, int shipperId) {
        if (order == null) return 404;
        return order.getShipper() != null && order.getShipper().getUserId() == shipperId ? 200 : 403;
    }

    private boolean requireCheckedInShift(HttpServletRequest req, HttpServletResponse resp, int shipperId) throws IOException {
        if (shipperShiftAccessService.hasCheckedInShift(shipperId)) return true;
        ApiResponse.error(resp, "Checked-in shift required", 403);
        return false;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int shipperId = getShipperId(req, resp);
        if (shipperId < 0) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            ApiResponse.error(resp, "Invalid endpoint", 400);
            return;
        }

        Integer detailOrderId = parseDetailOrderId(path);
        if (!"/dashboard".equals(path) && !"/orders/history".equals(path) && detailOrderId == null
                && !requireCheckedInShift(req, resp, shipperId)) return;

        switch (path) {
            case "/dashboard":
                ApiResponse.ok(resp, shipperService.getDashboardStats(shipperId));
                break;
            case "/orders/mine":
                List<Orders> mine = shipperService.getMyOrders(shipperId);
                ApiResponse.ok(resp, mine.stream().map(this::toListItem).collect(Collectors.toList()));
                break;
            case "/orders/active":
                List<Orders> active = shipperService.getMyActiveOrders(shipperId);
                ApiResponse.ok(resp, active.stream().map(this::toListItem).collect(Collectors.toList()));
                break;
            case "/orders/history": {
                int page = 1;
                String pageRaw = req.getParameter("page");
                if (pageRaw != null && !pageRaw.isBlank()) {
                    try {
                        page = Integer.parseInt(pageRaw.trim());
                    } catch (NumberFormatException e) {
                        ApiResponse.error(resp, "page must be a positive integer", 400);
                        break;
                    }
                }
                if (page < 1 || page > 10000) {
                    ApiResponse.error(resp, "page must be between 1 and 10000", 400);
                    break;
                }
                int size = 20;
                String sizeRaw = req.getParameter("size");
                if (sizeRaw != null && !sizeRaw.isBlank()) {
                    try {
                        size = Integer.parseInt(sizeRaw.trim());
                    } catch (NumberFormatException e) {
                        ApiResponse.error(resp, "size must be a positive integer", 400);
                        break;
                    }
                }
                if (size < 1) {
                    ApiResponse.error(resp, "size must be a positive integer", 400);
                    break;
                }
                if (size > 100) size = 100;
                LocalDate fromDate = null;
                String fromRaw = req.getParameter("fromDate");
                if (fromRaw != null && !fromRaw.isBlank()) {
                    try {
                        fromDate = LocalDate.parse(fromRaw.trim());
                    } catch (DateTimeParseException e) {
                        ApiResponse.error(resp, "fromDate must be in yyyy-MM-dd format", 400);
                        break;
                    }
                }
                LocalDate toDate = null;
                String toRaw = req.getParameter("toDate");
                if (toRaw != null && !toRaw.isBlank()) {
                    try {
                        toDate = LocalDate.parse(toRaw.trim());
                    } catch (DateTimeParseException e) {
                        ApiResponse.error(resp, "toDate must be in yyyy-MM-dd format", 400);
                        break;
                    }
                }
                if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
                    ApiResponse.error(resp, "fromDate must not be after toDate", 400);
                    break;
                }
                LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
                LocalDateTime to = toDate != null ? toDate.plusDays(1).atStartOfDay() : null;
                Map<String, Object> data = new HashMap<>();
                data.put("items", shipperService.getMyHistory(shipperId, page, size, from, to)
                        .stream().map(this::toListItem).collect(Collectors.toList()));
                data.put("total", shipperService.countMyHistory(shipperId, from, to));
                data.put("page", page);
                data.put("size", size);
                ApiResponse.ok(resp, data);
                break;
            }
            default:
                if (detailOrderId == null) {
                    ApiResponse.error(resp, "Not found", 404);
                    break;
                }
                Orders order = shipperService.getOwnedOrder(detailOrderId, shipperId);
                if (order == null) {
                    ApiResponse.error(resp, "Order not found", 404);
                } else if (!ShipperShiftAccessService.canReadOwnedOrder(order.getOrderStatus(),
                        shipperShiftAccessService.hasCheckedInShift(shipperId))) {
                    ApiResponse.error(resp, "Checked-in shift required", 403);
                } else {
                    ApiResponse.ok(resp, toDetail(order));
                }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int shipperId = getShipperId(req, resp);
        if (shipperId < 0 || !requireCheckedInShift(req, resp, shipperId)) return;
        MutationPath mutation = parseMutationPath(req.getPathInfo());
        if (mutation == null || !"fail".equals(mutation.action())) {
            ApiResponse.error(resp, "Not found", 404);
            return;
        }
        Map<String, Object> payload;
        try {
            payload = validateFailurePayload(mapper.readValue(req.getReader(), new TypeReference<Map<String, Object>>() {}));
        } catch (IOException | RuntimeException e) {
            payload = null;
        }
        if (payload == null) {
            ApiResponse.error(resp, "Invalid delivery failure payload", 400);
            return;
        }
        Orders order = shipperService.getOrder(mutation.orderId());
        int ownershipStatus = ownershipStatus(order, shipperId);
        if (ownershipStatus != 200) {
            ApiResponse.error(resp, ownershipStatus == 404 ? "Order not found" : "Forbidden", ownershipStatus);
            return;
        }
        OrderTransitionService.MutationResult result = shipperService.fail(mutation.orderId(), shipperId,
                (String) payload.get("expectedStatus"), (String) payload.get("reasonCode"), (String) payload.get("note"));
        int status = statusFor(result);
        if (status == 200) ApiResponse.ok(resp, null, "Delivery failure reported");
        else ApiResponse.error(resp, status == 409 ? CONFLICT_MESSAGE : "Cannot report delivery failure", status);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int shipperId = getShipperId(req, resp);
        if (shipperId < 0) return;
        if (!requireCheckedInShift(req, resp, shipperId)) return;

        MutationPath mutation = parseMutationPath(req.getPathInfo());
        if (mutation == null) {
            ApiResponse.error(resp, "Not found", 404);
            return;
        }

        Map<String, Object> body = mapper.readValue(req.getReader(), new TypeReference<Map<String, Object>>() {});
        Object rawExpectedStatus = body == null ? null : body.get("expectedStatus");
        String expectedStatus = rawExpectedStatus instanceof String ? (String) rawExpectedStatus : null;
        if (!isValidExpectedStatus(expectedStatus)) {
            ApiResponse.error(resp, "Invalid expectedStatus", 400);
            return;
        }

        int orderId = mutation.orderId();
        OrderTransitionService.MutationResult result;
        switch (mutation.action()) {
            case "pickup":
                result = shipperService.pickUpOrder(orderId, shipperId, expectedStatus);
                break;
            case "deliver": {
                BigDecimal collectedAmount = null;
                if (body.get("collectedAmount") != null) {
                    try {
                        collectedAmount = new BigDecimal(body.get("collectedAmount").toString());
                    } catch (NumberFormatException e) {
                        ApiResponse.error(resp, "Collected amount must be a valid number", 400);
                        return;
                    }
                }
                result = shipperService.deliverOrder(orderId, shipperId, collectedAmount, expectedStatus);
                break;
            }
            default:
                ApiResponse.error(resp, "Not found", 404);
                return;
        }
        if (result == OrderTransitionService.MutationResult.CONFLICT) {
            ApiResponse.error(resp, CONFLICT_MESSAGE, 409);
        } else if (result == OrderTransitionService.MutationResult.SUCCESS) {
            ApiResponse.ok(resp, null, "pickup".equals(mutation.action()) ? "Picked up successfully" : "Delivered successfully");
        } else {
            ApiResponse.error(resp, "Cannot update this order", 400);
        }
    }

    private Map<String, Object> toListItem(Orders o) {
        Map<String, Object> m = new HashMap<>();
        m.put("orderId", o.getOrderId());
        m.put("orderCode", o.getOrderCode());
        m.put("status", o.getOrderStatus());
        m.put("customerName", o.getCustomerName());
        m.put("customerPhone", o.getCustomerPhone());
        m.put("customerAddress", o.getCustomerAddress());
        m.put("finalAmount", o.getFinalAmount());
        m.put("shippingFee", o.getShippingFee());
        m.put("paymentMethod", o.getPaymentMethod());
        m.put("paymentStatus", o.getPaymentStatus());
        m.put("itemCount", orderItemDAO.findByOrderId(o.getOrderId()).stream().mapToInt(item -> item.getQuantity()).sum());
        m.put("assignedAt", o.getAssignedAt() != null ? o.getAssignedAt().toString() : null);
        m.put("pickedUpAt", o.getPickedUpAt() != null ? o.getPickedUpAt().toString() : null);
        m.put("deliveredAt", o.getDeliveredAt() != null ? o.getDeliveredAt().toString() : null);
        m.put("codCollectedAmount", o.getCodCollectedAmount());
        m.put("codCollectedAt", o.getCodCollectedAt() != null ? o.getCodCollectedAt().toString() : null);
        m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        return m;
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
        data.put("discountAmount", o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO);
        data.put("finalAmount", o.getFinalAmount());
        data.put("codCollectedAmount", o.getCodCollectedAmount());
        data.put("codCollectedAt", o.getCodCollectedAt() != null ? o.getCodCollectedAt().toString() : null);
        data.put("paymentMethod", o.getPaymentMethod());
        data.put("paymentStatus", o.getPaymentStatus());
        data.put("deliveryNote", o.getDeliveryNote());
        data.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        data.put("assignedAt", o.getAssignedAt() != null ? o.getAssignedAt().toString() : null);
        data.put("pickedUpAt", o.getPickedUpAt() != null ? o.getPickedUpAt().toString() : null);
        data.put("deliveredAt", o.getDeliveredAt() != null ? o.getDeliveredAt().toString() : null);
        List<Map<String, Object>> items = orderItemDAO.findByOrderId(o.getOrderId())
                .stream()
                .map(oi -> {
                    Map<String, Object> im = new HashMap<>();
                    im.put("productId", oi.getProduct().getProductId());
                    im.put("variantId", oi.getVariant() != null ? oi.getVariant().getVariantId() : null);
                    im.put("productName", oi.getProductName());
                    im.put("variantName", oi.getVariantName() != null ? oi.getVariantName() : "");
                    im.put("quantity", oi.getQuantity());
                    im.put("unitPrice", oi.getUnitPrice());
                    im.put("totalPrice", oi.getTotalPrice());
                    im.put("modifiers", oi.getModifiers());
                    return im;
                })
                .collect(Collectors.toList());
        data.put("items", items);
        var savedHistory = new service.OrderStatusHistoryService().getByOrderId(o.getOrderId());
        data.put("statusHistory", savedHistory);
        data.put("allowedActions", ShipperService.getAllowedActions(o.getOrderStatus(), o.getPaymentMethod(), o.getPaymentStatus()));
        return data;
    }
}
