package service;

import dao.OrdersDAO;
import dao.UserDAO;
import entity.Orders;
import entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShipperService {
    private OrdersDAO ordersDAO = new OrdersDAO();
    private UserDAO userDAO = new UserDAO();
    private NotificationService notificationService = new NotificationService();
    private OrderTransitionService transitionService = new OrderTransitionService();

    static boolean canPickUp(String status, Integer assignedShipperId, int shipperId) {
        return "ASSIGNED".equals(status) && assignedShipperId != null && assignedShipperId == shipperId;
    }

    public List<Orders> getMyOrders(int shipperId) {
        return ordersDAO.findByShipperId(shipperId);
    }

    public List<Orders> getMyActiveOrders(int shipperId) {
        List<Orders> orders = new java.util.ArrayList<>(ordersDAO.findByShipperIdAndStatus(shipperId, "ASSIGNED"));
        orders.addAll(ordersDAO.findByShipperIdAndStatus(shipperId, "PICKED_UP"));
        return orders;
    }

    public List<Orders> getMyHistory(int shipperId) {
        return ordersDAO.findByShipperIdAndStatus(shipperId, "DELIVERED");
    }

    public Map<String, Object> getDashboardStats(int shipperId) {
        long todayDelivered = ordersDAO.countByShipperAndStatus(shipperId, "DELIVERED", LocalDate.now());
        long todayPickedUp = ordersDAO.countByShipperAndStatus(shipperId, "PICKED_UP", LocalDate.now());
        long totalDelivered = ordersDAO.countByShipperAndStatus(shipperId, "DELIVERED", null);
        long activeCount = getMyActiveOrders(shipperId).size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayDelivered", todayDelivered);
        stats.put("todayPickedUp", todayPickedUp);
        stats.put("totalDelivered", totalDelivered);
        stats.put("activeCount", activeCount);
        return stats;
    }

    public boolean pickUpOrder(int orderId, int shipperId) {
        Orders order = ordersDAO.findById(orderId);
        boolean ok = transitionService.transition(orderId, "PICKED_UP", "SHIPPER", shipperId, "Đã lấy hàng", null, null);
        if (ok && order != null && order.getUser() != null) notificationService.notifyUser(order.getUser().getUserId(), "Đơn hàng đang giao", "Đơn " + order.getOrderCode() + " đã được shipper lấy hàng", "ORDER_STATUS", "/account/orders/" + orderId);
        return ok;
    }

    public String deliverOrder(int orderId, int shipperId, BigDecimal collectedAmount) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null || !"PICKED_UP".equals(order.getOrderStatus())) {
                em.getTransaction().rollback();
                return "Order must be picked up before delivery";
            }
            if (order.getShipper() == null || order.getShipper().getUserId() != shipperId) {
                em.getTransaction().rollback();
                return "Order is not assigned to this shipper";
            }
            if ("COD".equals(order.getPaymentMethod()) && (collectedAmount == null || order.getFinalAmount() == null || collectedAmount.compareTo(order.getFinalAmount()) != 0)) {
                em.getTransaction().rollback();
                return "COD collected amount must exactly match final amount";
            }
            if (!"COD".equals(order.getPaymentMethod()) && !"PAID".equals(order.getPaymentStatus())) {
                em.getTransaction().rollback();
                return "Order must be paid before delivery";
            }
            em.getTransaction().rollback();
            boolean ok = transitionService.transition(orderId, "DELIVERED", "SHIPPER", shipperId, "Đã giao hàng", null, collectedAmount);
            if (!ok) return "Order cannot be delivered";
            if (order.getUser() != null) notificationService.notifyUser(order.getUser().getUserId(), "Đơn hàng đã giao", "Đơn " + order.getOrderCode() + " đã được giao thành công", "ORDER_STATUS", "/account/orders/" + orderId);
            return null;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

}
