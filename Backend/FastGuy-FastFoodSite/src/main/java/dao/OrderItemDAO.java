package dao;

import entity.OrderItem;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderItemDAO {
    public void save(OrderItem item) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(item);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public Map<Integer, Integer> countItemsByOrderIds(List<Integer> orderIds) {
        if (orderIds.isEmpty()) return Map.of();
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Object[]> rows = em.createQuery("SELECT oi.order.orderId, SUM(oi.quantity) FROM OrderItem oi WHERE oi.order.orderId IN :ids GROUP BY oi.order.orderId", Object[].class)
                    .setParameter("ids", orderIds).getResultList();
            Map<Integer, Integer> counts = new HashMap<>();
            for (Object[] row : rows) counts.put(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            return counts;
        } finally { em.close(); }
    }

    public List<OrderItem> findByOrderId(int orderId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT oi FROM OrderItem oi WHERE oi.order.orderId = :oid ORDER BY oi.orderItemId",
                    OrderItem.class)
                    .setParameter("oid", orderId)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
