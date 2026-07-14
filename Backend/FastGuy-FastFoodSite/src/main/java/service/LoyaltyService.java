package service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entity.LoyaltyTransaction;
import entity.Orders;
import entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

public class LoyaltyService {
    public void awardForDelivery(int orderId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null || order.getUser() == null || !"DELIVERED".equals(order.getOrderStatus()) || !"PAID".equals(order.getPaymentStatus()) || hasType(em, orderId, "EARN")) {
                em.getTransaction().rollback();
                return;
            }
            int points = order.getFinalAmount() == null ? 0 : order.getFinalAmount().divideToIntegralValue(BigDecimal.valueOf(1000)).intValue();
            LoyaltyTransaction transaction = new LoyaltyTransaction();
            transaction.setUser(order.getUser());
            transaction.setOrder(order);
            transaction.setTransactionType("EARN");
            transaction.setPoints(points);
            transaction.setCreatedAt(LocalDateTime.now());
            em.persist(transaction);
            order.getUser().setLoyaltyPoints(order.getUser().getLoyaltyPoints() + points);
            em.getTransaction().commit();
            if (points > 0) new NotificationService().notifyUser(order.getUser().getUserId(), "Điểm thưởng", "Bạn nhận được " + points + " điểm từ đơn " + order.getOrderCode(), "LOYALTY_EARN", "/account/loyalty");
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public Map<String, Object> getForUser(int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            User user = em.find(User.class, userId);
            if (user == null) return null;
            List<LoyaltyTransaction> transactions = em.createQuery("SELECT lt FROM LoyaltyTransaction lt WHERE lt.user.userId = :userId ORDER BY lt.createdAt DESC", LoyaltyTransaction.class)
                    .setParameter("userId", userId).getResultList();
            List<Map<String, Object>> history = new ArrayList<>();
            for (LoyaltyTransaction transaction : transactions) {
                Map<String, Object> item = new HashMap<>();
                item.put("transactionId", transaction.getLoyaltyTransactionId());
                item.put("orderId", transaction.getOrder().getOrderId());
                item.put("orderCode", transaction.getOrder().getOrderCode());
                item.put("type", transaction.getTransactionType());
                item.put("points", transaction.getPoints());
                item.put("createdAt", transaction.getCreatedAt());
                history.add(item);
            }
            int points = user.getLoyaltyPoints();
            Map<String, Object> result = new HashMap<>();
            result.put("points", points);
            result.put("tier", points >= 2000 ? "Gold" : points >= 500 ? "Silver" : "Bronze");
            result.put("history", history);
            return result;
        } finally {
            em.close();
        }
    }

    public void reverseForRefund(EntityManager em, Orders order) {
        if (order.getUser() == null || hasType(em, order.getOrderId(), "REVERSE")) return;
        List<LoyaltyTransaction> earns = em.createQuery("SELECT lt FROM LoyaltyTransaction lt WHERE lt.order.orderId = :orderId AND lt.transactionType = 'EARN'", LoyaltyTransaction.class)
                .setParameter("orderId", order.getOrderId()).getResultList();
        if (earns.isEmpty()) return;
        int points = earns.get(0).getPoints();
        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setUser(order.getUser());
        transaction.setOrder(order);
        transaction.setTransactionType("REVERSE");
        transaction.setPoints(-points);
        transaction.setCreatedAt(LocalDateTime.now());
        em.persist(transaction);
        order.getUser().setLoyaltyPoints(Math.max(0, order.getUser().getLoyaltyPoints() - points));
    }

    private boolean hasType(EntityManager em, int orderId, String type) {
        return !em.createQuery("SELECT lt FROM LoyaltyTransaction lt WHERE lt.order.orderId = :orderId AND lt.transactionType = :type", LoyaltyTransaction.class)
                .setParameter("orderId", orderId).setParameter("type", type).setMaxResults(1).getResultList().isEmpty();
    }
}
