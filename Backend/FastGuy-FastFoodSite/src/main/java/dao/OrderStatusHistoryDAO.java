package dao;

import java.util.List;

import entity.OrderStatusHistory;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

public class OrderStatusHistoryDAO {
    public void save(OrderStatusHistory history) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(history);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException(e);
        } finally {
            em.close();
        }
    }

    public List<OrderStatusHistory> findByOrderId(int orderId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT h FROM OrderStatusHistory h WHERE h.orderId = :orderId ORDER BY h.createdAt",
                    OrderStatusHistory.class)
                    .setParameter("orderId", orderId)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
