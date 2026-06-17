package dao;

import entity.OrderItem;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.util.List;

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
