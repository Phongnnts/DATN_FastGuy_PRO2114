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
import entity.Orders;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.NotificationService;
import service.OrderService;
import service.OrderStatusHistoryService;
import service.OrderTransitionService;
import utils.ApiResponse;
import utils.JsonUtil;
import utils.JwtUtil;

@WebServlet("/api/admin/orders/*")
public class AdminOrderServlet extends HttpServlet {
    private OrdersDAO ordersDAO = new OrdersDAO();
    private OrderItemDAO orderItemDAO = new OrderItemDAO();
    private OrderStatusHistoryService historyService = new OrderStatusHistoryService();
    private OrderService orderService = new OrderService();
    private OrderTransitionService transitionService = new OrderTransitionService();
    private NotificationService notificationService = new NotificationService();

    public List<Map<String, Object>> getOrdersData() {
        return ordersDAO.findAll().stream().map(o -> {
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
        String token = authHeader.substring(7);
        if (!"ADMIN".equals(JwtUtil.getRole(token))) { ApiResponse.error(resp, "Forbidden", 403); return; }

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
                List<Map<String, Object>> allData = getOrdersData().stream().filter(m -> {
                    String created = (String) m.get("createdAt");
                    if (created == null) return false;
                    LocalDate date = LocalDateTime.parse(created.replace("Z", "")).toLocalDate();
                    return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
                }).sorted(Comparator.comparing(m -> (String) m.get("createdAt"), Comparator.nullsLast(Comparator.reverseOrder()))).collect(Collectors.toList());
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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) { ApiResponse.error(resp, "Missing token", 401); return; }
        if (!"ADMIN".equals(JwtUtil.getRole(authHeader.substring(7)))) { ApiResponse.error(resp, "Forbidden", 403); return; }

        String path = req.getPathInfo();
        if (path == null) { resp.sendError(404); return; }

        String[] parts = path.split("/");
        if (parts.length < 3) { resp.sendError(404); return; }

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
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) { ApiResponse.error(resp, "Missing token", 401); return; }
        if (!"ADMIN".equals(JwtUtil.getRole(authHeader.substring(7)))) { ApiResponse.error(resp, "Forbidden", 403); return; }

        String path = req.getPathInfo();
        if (path == null) { resp.sendError(404); return; }

        String[] parts = path.split("/");
        if (parts.length < 3) { resp.sendError(404); return; }

        try {
            int orderId = Integer.parseInt(parts[1]);
            String action = parts[2];
            int adminId = getAdminId(req);

            if ("cancel".equals(action)) {
                Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
                String reason = body != null ? (String) body.get("reason") : "Admin hủy đơn";
                boolean ok = orderService.cancelOrder(orderId, null, adminId, reason, false);
                if (!ok) { ApiResponse.error(resp, "Cannot cancel order", 400); return; }
                Orders order = ordersDAO.findById(orderId);
                if (order != null && order.getUser() != null) {
                    notificationService.notifyUser(order.getUser().getUserId(), "Đơn hàng đã bị hủy", "Đơn " + order.getOrderCode() + " đã bị hủy bởi quản trị viên", "ORDER_CANCELLED", "/account/orders/" + orderId);
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
            } else {
                resp.sendError(404);
            }
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid order ID", 400);
        } catch (Exception e) {
            ApiResponse.error(resp, e.getMessage(), 400);
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
        data.put("failureReason", o.getFailureReason());
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

        return data;
    }
}
