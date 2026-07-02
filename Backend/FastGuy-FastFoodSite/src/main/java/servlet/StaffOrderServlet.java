package servlet;

import java.io.IOException;
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
import service.StaffOrderService;
import utils.ApiResponse;
import utils.JwtUtil;

@WebServlet("/api/staff/orders/*")
public class StaffOrderServlet extends HttpServlet {
    private StaffOrderService staffOrderService = new StaffOrderService();
    private OrderItemDAO orderItemDAO = new OrderItemDAO();
    private OrdersDAO ordersDAO = new OrdersDAO();
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int staffId = getStaffId(req, resp);
        if (staffId < 0) return;
        String path = req.getPathInfo();
        if (path != null && path.contains("/notes")) {
            String orderIdStr = path.substring(1, path.indexOf("/notes") - 1);
            try {
                int orderId = Integer.parseInt(orderIdStr);
                java.util.Map<String, Object> body = utils.JsonUtil.fromJson(req.getReader(), java.util.Map.class);
                String note = body != null ? (String) body.get("note") : null;
                if (note != null) {
                    Orders order = ordersDAO.findById(orderId);
                    if (order != null) {
                        String existing = order.getInternalNote();
                        order.setInternalNote(existing != null ? existing + "\n---\n" + note : note);
                        ordersDAO.save(order);
                    }
                }
                ApiResponse.ok(resp, null, "Note saved");
            } catch (NumberFormatException e) {
                ApiResponse.error(resp, "Invalid order ID", 400);
            }
        } else {
            resp.sendError(404);
        }
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
        if (staffId < 0) return;

        try {
            String path = req.getPathInfo();

            if ("/shippers".equals(path)) {
                List<entity.User> shippers = staffOrderService.getAvailableShippers();
                List<Map<String, Object>> result = shippers.stream().map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getUserId());
                    m.put("fullName", u.getFullName());
                    m.put("phone", u.getPhone());
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
            } else if (path.equals("/history")) {
                List<Orders> all = new java.util.ArrayList<>();
                for (String s : new String[]{"READY", "DELIVERED", "CANCELLED", "CONFIRMED", "PREPARING"}) {
                    all.addAll(ordersDAO.findByStatus(s));
                }
                all.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                ApiResponse.ok(resp, all.stream().map(o -> toListItem(o)).collect(Collectors.toList()));
            } else if (path.equals("/export")) {
                resp.setContentType("text/csv;charset=UTF-8");
                resp.setHeader("Content-Disposition", "attachment; filename=orders.csv");
                List<Orders> all = new java.util.ArrayList<>();
                for (String s : new String[]{"PENDING","CONFIRMED","PREPARING","READY","DELIVERED","CANCELLED"}) {
                    all.addAll(ordersDAO.findByStatus(s));
                }
                var writer = resp.getWriter();
                writer.write("orderCode,status,customerName,finalAmount,createdAt\n");
                for (Orders o : all) {
                    writer.write(String.format("%s,%s,%s,%s,%s\n",
                        o.getOrderCode(), o.getOrderStatus(), o.getCustomerName(),
                        o.getFinalAmount(), o.getCreatedAt()));
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
        } catch (Exception e) {
            e.printStackTrace();
            ApiResponse.error(resp, "Internal error: " + e.getMessage(), 500);
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
            if (status == null || (!"CONFIRMED".equals(status) && !"PREPARING".equals(status) && !"READY".equals(status) && !"CANCELLED".equals(status))) {
                ApiResponse.error(resp, "Invalid status", 400);
                return;
            }
            String failureReason = (String) body.get("failureReason");
            if ("CONFIRMED".equals(status)) {
                Orders order = staffOrderService.getOrderDetail(orderId);
                if (order != null && "BANK_TRANSFER".equals(order.getPaymentMethod()) && !"PAID".equals(order.getPaymentStatus())) {
                    ApiResponse.error(resp, "Đơn chuyển khoản chưa thanh toán", 400);
                    return;
                }
            }
            boolean ok = staffOrderService.updateStatus(orderId, status, staffId, failureReason);
            if (!ok) {
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
            int shipperId = ((Number) body.get("shipperId")).intValue();
            boolean ok = staffOrderService.assignShipper(orderId, shipperId);
            if (!ok) {
                ApiResponse.error(resp, "Cannot assign shipper", 400);
                return;
            }
            ApiResponse.ok(resp, null, "Shipper assigned");
        } else if ("assign".equals(action)) {
            ApiResponse.ok(resp, null, "Assigned");
        } else if ("export".equals(action)) {
            resp.setContentType("text/csv;charset=UTF-8");
            resp.setHeader("Content-Disposition", "attachment; filename=orders.csv");
            resp.getWriter().write("orderCode,status,total\n");
        } else {
            resp.sendError(404);
        }
    }

    private Map<String, Object> toListItem(Orders o) {
        Map<String, Object> m = new HashMap<>();
        m.put("orderId", o.getOrderId());
        m.put("orderCode", o.getOrderCode());
        m.put("userId", o.getUser() != null ? o.getUser().getUserId() : null);
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
        m.put("userId", o.getUser() != null ? o.getUser().getUserId() : null);
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
                    im.put("variantId", oi.getVariant() != null ? oi.getVariant().getVariantId() : null);
                    im.put("productName", oi.getProductName());
                    im.put("variantName", oi.getVariantName() != null ? oi.getVariantName() : "");
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
        m.put("statusHistory", history);
        m.put("internalNotes", new java.util.ArrayList<>());

        return m;
    }
}
