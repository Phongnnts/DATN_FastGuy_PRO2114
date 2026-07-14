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

public class ShipperService {
    private OrdersDAO ordersDAO = new OrdersDAO();
    private UserDAO userDAO = new UserDAO();
    private OrderService orderService = new OrderService();
    private NotificationService notificationService = new NotificationService();
    private OrderStatusHistoryService orderStatusHistoryService = new OrderStatusHistoryService();
    private LoyaltyService loyaltyService = new LoyaltyService();

    public List<Orders> getAvailableOrders() {
        return ordersDAO.findByStatusAndNoShipper("READY");
    }

    public List<Orders> getMyOrders(int shipperId) {
        return ordersDAO.findByShipperId(shipperId);
    }

    public List<Orders> getMyActiveOrders(int shipperId) {
        return ordersDAO.findByShipperIdAndStatus(shipperId, "PICKED_UP");
    }

    public List<Orders> getMyHistory(int shipperId) {
        return ordersDAO.findByShipperIdAndStatus(shipperId, "DELIVERED");
    }

    public Map<String, Object> getDashboardStats(int shipperId) {
        long todayDelivered = ordersDAO.countByShipperAndStatus(shipperId, "DELIVERED", LocalDate.now());
        long todayPickedUp = ordersDAO.countByShipperAndStatus(shipperId, "PICKED_UP", LocalDate.now());
        long totalDelivered = ordersDAO.countByShipperAndStatus(shipperId, "DELIVERED", null);
        long activeCount = ordersDAO.findByShipperIdAndStatus(shipperId, "PICKED_UP").size();
        long availableCount = ordersDAO.findByStatusAndNoShipper("READY").size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayDelivered", todayDelivered);
        stats.put("todayPickedUp", todayPickedUp);
        stats.put("totalDelivered", totalDelivered);
        stats.put("activeCount", activeCount);
        stats.put("availableCount", availableCount);
        return stats;
    }

    public boolean assignShipper(int orderId, int shipperId) {
        Orders order = ordersDAO.findById(orderId);
        if (order == null || !"READY".equals(order.getOrderStatus())) return false;
        if (order.getShipper() != null) return false;
        User shipper = userDAO.findById(shipperId);
        if (shipper == null) return false;
        order.setShipper(shipper);
        order.setAssignedAt(LocalDateTime.now());
        ordersDAO.save(order);
        return true;
    }

    public boolean pickUpOrder(int orderId, int shipperId) {
        Orders order = ordersDAO.findById(orderId);
        if (order == null || !"READY".equals(order.getOrderStatus())) return false;
        if (order.getShipper() != null && order.getShipper().getUserId() != shipperId) return false;
        User shipper = userDAO.findById(shipperId);
        if (shipper == null) return false;
        order.setOrderStatus("PICKED_UP");
        if (order.getShipper() == null) order.setShipper(shipper);
        order.setPickedUpAt(LocalDateTime.now());
        ordersDAO.save(order);
        orderStatusHistoryService.record(orderId, shipperId, "SHIPPER", "READY", "PICKED_UP", "Đã lấy hàng");
        if (order.getUser() != null) {
            notificationService.notifyUser(order.getUser().getUserId(), "Đơn hàng đang giao", "Đơn " + order.getOrderCode() + " đã được shipper lấy hàng", "ORDER_STATUS", "/account/orders/" + orderId);
        }
        return true;
    }

    public String deliverOrder(int orderId, int shipperId, BigDecimal collectedAmount) {
        Orders order = ordersDAO.findById(orderId);
        if (order == null || !"PICKED_UP".equals(order.getOrderStatus())) return "Order must be picked up before delivery";
        if (order.getShipper() == null || order.getShipper().getUserId() != shipperId) return "Order is not assigned to this shipper";
        if ("COD".equals(order.getPaymentMethod()) && (collectedAmount == null || order.getFinalAmount() == null || collectedAmount.compareTo(order.getFinalAmount()) != 0)) {
            return "COD collected amount must exactly match final amount";
        }
        order.setOrderStatus("DELIVERED");
        order.setDeliveredAt(LocalDateTime.now());
        if ("COD".equals(order.getPaymentMethod())) {
            order.setCodCollectedAmount(collectedAmount);
            order.setCodCollectedAt(LocalDateTime.now());
            order.setPaymentStatus("PAID");
            order.setPaidAt(LocalDateTime.now());
        }
        ordersDAO.save(order);
        orderStatusHistoryService.record(orderId, shipperId, "SHIPPER", "PICKED_UP", "DELIVERED", "Đã giao hàng");
        if (order.getUser() != null) {
            notificationService.notifyUser(order.getUser().getUserId(), "Đơn hàng đã giao", "Đơn " + order.getOrderCode() + " đã được giao thành công", "ORDER_STATUS", "/account/orders/" + orderId);
        }
        loyaltyService.awardForDelivery(orderId);
        return null;
    }

    public boolean cancelOrder(int orderId, int shipperId, String reason) {
        Orders order = ordersDAO.findById(orderId);
        if (order == null) return false;
        if (order.getShipper() == null || order.getShipper().getUserId() != shipperId) return false;
        if (!"PICKED_UP".equals(order.getOrderStatus())) return false;

        boolean ok = orderService.cancelOrder(orderId, null, null, reason, false);
        if (!ok) return false;

        order = ordersDAO.findById(orderId);
        if (order.getUser() != null) {
            notificationService.notifyUser(order.getUser().getUserId(), "Đơn giao hàng thất bại", "Đơn " + order.getOrderCode() + " đã bị hủy bởi shipper", "ORDER_CANCELLED", "/account/orders/" + orderId);
        }
        return true;
    }
}
