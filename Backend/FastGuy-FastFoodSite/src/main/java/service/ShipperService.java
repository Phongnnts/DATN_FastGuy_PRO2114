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
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null || !"READY".equals(order.getOrderStatus()) || order.getShipper() != null) {
                em.getTransaction().rollback();
                return false;
            }
            User shipper = em.find(User.class, shipperId);
            if (shipper == null) { em.getTransaction().rollback(); return false; }
            order.setShipper(shipper);
            order.setAssignedAt(LocalDateTime.now());
            em.getTransaction().commit();
            return true;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean pickUpOrder(int orderId, int shipperId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null || !"READY".equals(order.getOrderStatus())) {
                em.getTransaction().rollback();
                return false;
            }
            if (order.getShipper() != null && order.getShipper().getUserId() != shipperId) {
                em.getTransaction().rollback();
                return false;
            }
            User shipper = em.find(User.class, shipperId);
            if (shipper == null) { em.getTransaction().rollback(); return false; }
            order.setOrderStatus("PICKED_UP");
            if (order.getShipper() == null) order.setShipper(shipper);
            order.setPickedUpAt(LocalDateTime.now());
            em.getTransaction().commit();
            orderStatusHistoryService.record(orderId, shipperId, "SHIPPER", "READY", "PICKED_UP", "Đã lấy hàng");
            if (order.getUser() != null) {
                notificationService.notifyUser(order.getUser().getUserId(), "Đơn hàng đang giao", "Đơn " + order.getOrderCode() + " đã được shipper lấy hàng", "ORDER_STATUS", "/account/orders/" + orderId);
            }
            return true;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
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
            order.setOrderStatus("DELIVERED");
            order.setDeliveredAt(LocalDateTime.now());
            if ("COD".equals(order.getPaymentMethod())) {
                order.setCodCollectedAmount(collectedAmount);
                order.setCodCollectedAt(LocalDateTime.now());
                order.setPaymentStatus("PAID");
                order.setPaidAt(LocalDateTime.now());
            }
            int earnedPoints = loyaltyService.awardForDelivery(em, order);
            em.getTransaction().commit();
            orderStatusHistoryService.record(orderId, shipperId, "SHIPPER", "PICKED_UP", "DELIVERED", "Đã giao hàng");
            if (order.getUser() != null) {
                notificationService.notifyUser(order.getUser().getUserId(), "Đơn hàng đã giao", "Đơn " + order.getOrderCode() + " đã được giao thành công", "ORDER_STATUS", "/account/orders/" + orderId);
                if (earnedPoints > 0) notificationService.notifyUser(order.getUser().getUserId(), "Điểm thưởng", "Bạn nhận được " + earnedPoints + " điểm từ đơn " + order.getOrderCode(), "LOYALTY_EARN", "/account/profile");
            }
            return null;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

}
