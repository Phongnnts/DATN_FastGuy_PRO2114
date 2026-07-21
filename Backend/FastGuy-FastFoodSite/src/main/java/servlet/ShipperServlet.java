package servlet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import entity.Orders;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import dao.OrderItemDAO;
import service.ShipperShiftAccessService;
import service.ShipperService;
import utils.ApiResponse;
import utils.JwtUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/shipper/*")
public class ShipperServlet extends HttpServlet {
    private ShipperService shipperService = new ShipperService();
    private ShipperShiftAccessService shipperShiftAccessService = new ShipperShiftAccessService();
    private OrderItemDAO orderItemDAO = new OrderItemDAO();
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
        return userId;
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

        if (!"/dashboard".equals(path) && !"/orders/history".equals(path) && !requireCheckedInShift(req, resp, shipperId)) return;

        switch (path) {
            case "/dashboard":
                ApiResponse.ok(resp, shipperService.getDashboardStats(shipperId));
                break;
            case "/orders":
                List<Orders> available = shipperService.getAvailableOrders();
                ApiResponse.ok(resp, available.stream().map(this::toListItem).collect(Collectors.toList()));
                break;
            case "/orders/mine":
                List<Orders> mine = shipperService.getMyOrders(shipperId);
                ApiResponse.ok(resp, mine.stream().map(this::toListItem).collect(Collectors.toList()));
                break;
            case "/orders/active":
                List<Orders> active = shipperService.getMyActiveOrders(shipperId);
                ApiResponse.ok(resp, active.stream().map(this::toListItem).collect(Collectors.toList()));
                break;
            case "/orders/history":
                List<Orders> history = shipperService.getMyHistory(shipperId);
                ApiResponse.ok(resp, history.stream().map(this::toListItem).collect(Collectors.toList()));
                break;
            default:
                if (path.startsWith("/orders/")) {
                    try {
                        String[] segs = path.split("/");
                        if (segs.length >= 3) {
                            int orderId = Integer.parseInt(segs[2]);
                            List<Orders> all = new ArrayList<>(shipperService.getMyOrders(shipperId));
                            all.addAll(shipperService.getAvailableOrders());
                            Orders order = all.stream().filter(o -> o.getOrderId() == orderId).findFirst().orElse(null);
                            if (order != null) {
                                ApiResponse.ok(resp, toDetail(order));
                            } else {
                                ApiResponse.error(resp, "Order not found", 404);
                            }
                        }
                    } catch (NumberFormatException e) {
                        ApiResponse.error(resp, "Invalid order ID", 400);
                    }
                } else {
                    ApiResponse.error(resp, "Not found", 404);
                }
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int shipperId = getShipperId(req, resp);
        if (shipperId < 0) return;
        if (!requireCheckedInShift(req, resp, shipperId)) return;

        String path = req.getPathInfo();
        if (path == null) {
            ApiResponse.error(resp, "Invalid endpoint", 400);
            return;
        }

        String[] segs = path.split("/");
        if (segs.length < 3) {
            ApiResponse.error(resp, "Invalid endpoint", 400);
            return;
        }

        try {
            int orderId = Integer.parseInt(segs[2]);
            String action = segs.length >= 4 ? segs[3] : "";

            switch (action) {
                case "pickup": {
                    boolean ok = shipperService.pickUpOrder(orderId, shipperId);
                    if (ok) {
                        ApiResponse.ok(resp, null, "Picked up successfully");
                    } else {
                        ApiResponse.error(resp, "Cannot pick up this order", 400);
                    }
                    break;
                }
                case "deliver": {
                    Map<String, Object> body = mapper.readValue(req.getReader(), new TypeReference<Map<String, Object>>() {});
                    BigDecimal collectedAmount = null;
                    if (body != null && body.get("collectedAmount") != null) {
                        try {
                            collectedAmount = new BigDecimal(body.get("collectedAmount").toString());
                        } catch (NumberFormatException e) {
                            ApiResponse.error(resp, "Collected amount must be a valid number", 400);
                            break;
                        }
                    }
                    String error = shipperService.deliverOrder(orderId, shipperId, collectedAmount);
                    if (error == null) {
                        ApiResponse.ok(resp, null, "Delivered successfully");
                    } else {
                        ApiResponse.error(resp, error, 400);
                    }
                    break;
                }
                default:
                    ApiResponse.error(resp, "Not found", 404);
            }
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid order ID", 400);
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
        m.put("serviceFee", o.getServiceFee());
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
        data.put("finalAmount", o.getFinalAmount());
        data.put("codCollectedAmount", o.getCodCollectedAmount());
        data.put("codCollectedAt", o.getCodCollectedAt() != null ? o.getCodCollectedAt().toString() : null);
        data.put("paymentMethod", o.getPaymentMethod());
        data.put("paymentStatus", o.getPaymentStatus());
        data.put("deliveryNote", o.getDeliveryNote());
        data.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
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
                    return im;
                })
                .collect(Collectors.toList());
        data.put("items", items);
        var savedHistory = new service.OrderStatusHistoryService().getByOrderId(o.getOrderId());
        data.put("statusHistory", savedHistory);
        return data;
    }
}
