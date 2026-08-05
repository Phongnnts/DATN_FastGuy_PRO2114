package service;

import dao.OrdersDAO;
import dao.UserDAO;
import entity.Orders;
import entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShipperService {
    private OrdersDAO ordersDAO = new OrdersDAO();
    private UserDAO userDAO = new UserDAO();
    private NotificationService notificationService = new NotificationService();
    private OrderTransitionService transitionService = new OrderTransitionService();

    static boolean canPickUp(String status, Integer assignedShipperId, int shipperId) {
        return "ASSIGNED".equals(status) && assignedShipperId != null && assignedShipperId == shipperId;
    }

    public static Set<String> getAllowedActions(String status, String paymentMethod, String paymentStatus) {
        if ("ASSIGNED".equals(status)) return Set.of("PICKED_UP");
        if (!"PICKED_UP".equals(status)) return Set.of();
        return "COD".equals(paymentMethod) || "PAID".equals(paymentStatus) ? Set.of("DELIVERED") : Set.of();
    }

    public List<Orders> getMyOrders(int shipperId) {
        return ordersDAO.findByShipperId(shipperId);
    }

    public Orders getOwnedOrder(int orderId, int shipperId) {
        Orders order = ordersDAO.findById(orderId);
        return order != null && order.getShipper() != null && order.getShipper().getUserId() == shipperId ? order : null;
    }

    public List<Orders> getMyActiveOrders(int shipperId) {
        List<Orders> orders = new java.util.ArrayList<>(ordersDAO.findByShipperIdAndStatus(shipperId, "ASSIGNED"));
        orders.addAll(ordersDAO.findByShipperIdAndStatus(shipperId, "PICKED_UP"));
        return orders;
    }

    public List<Orders> getMyHistory(int shipperId, int page, int size, LocalDateTime from, LocalDateTime to) {
        return ordersDAO.findHistoryByShipperId(shipperId, page, size, from, to);
    }

    public long countMyHistory(int shipperId, LocalDateTime from, LocalDateTime to) {
        return ordersDAO.countHistoryByShipperId(shipperId, from, to);
    }

    public Map<String, Object> getDashboardStats(int shipperId) {
        LocalDate today = LocalDate.now();
        ShipperDashboardBoundary.DateRange todayRange = ShipperDashboardBoundary.forDate(today);
        long todayDelivered = ordersDAO.countDeliveredByShipperAndDateRange(shipperId, todayRange.start(), todayRange.end());
        long todayPickedUp = ordersDAO.countByShipperAndStatus(shipperId, "PICKED_UP", LocalDate.now());
        long totalDelivered = ordersDAO.countByShipperAndStatus(shipperId, "DELIVERED", null);
        long activeCount = getMyActiveOrders(shipperId).size();
        double todayCodCollected = ordersDAO.sumCodCollectedByShipperAndDateRange(shipperId, todayRange.start(), todayRange.end());

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayDelivered", todayDelivered);
        stats.put("todayPickedUp", todayPickedUp);
        stats.put("totalDelivered", totalDelivered);
        stats.put("activeCount", activeCount);
        stats.put("todayCodCollected", todayCodCollected);
        stats.put("pendingCodCollected", todayCodCollected);
        return stats;
    }

    public boolean pickUpOrder(int orderId, int shipperId) {
        boolean ok = transitionService.transition(orderId, "PICKED_UP", "SHIPPER", shipperId, "Đã lấy hàng", null, null);
        Orders order = ok ? ordersDAO.findById(orderId) : null;
        if (order != null && order.getUser() != null) notificationService.notifyUser(order.getUser().getUserId(), "Đơn hàng đang giao", "Đơn " + order.getOrderCode() + " đã được shipper lấy hàng", "ORDER_STATUS", "/account/orders/" + orderId);
        return ok;
    }

    public String deliverOrder(int orderId, int shipperId, BigDecimal collectedAmount) {
        boolean ok = transitionService.transition(orderId, "DELIVERED", "SHIPPER", shipperId, "Đã giao hàng", null, collectedAmount);
        if (!ok) return "Order cannot be delivered";
        Orders order = ordersDAO.findById(orderId);
        if (order != null && order.getUser() != null) notificationService.notifyUser(order.getUser().getUserId(), "Đơn hàng đã giao", "Đơn " + order.getOrderCode() + " đã được giao thành công", "ORDER_STATUS", "/account/orders/" + orderId);
        return null;
    }

}
