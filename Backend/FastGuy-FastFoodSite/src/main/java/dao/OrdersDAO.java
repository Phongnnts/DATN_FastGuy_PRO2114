package dao;

import entity.Orders;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.util.List;

public class OrdersDAO {
    public Orders findById(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.find(Orders.class, id);
        } finally {
            em.close();
        }
    }

    public List<Orders> findByUserId(int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT o FROM Orders o WHERE o.user.userId = :uid ORDER BY o.createdAt DESC",
                    Orders.class)
                    .setParameter("uid", userId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Orders> findAll() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT o FROM Orders o ORDER BY o.createdAt DESC", Orders.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Orders> findByStatus(String status) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT o FROM Orders o WHERE o.orderStatus = :status ORDER BY o.createdAt DESC",
                    Orders.class)
                    .setParameter("status", status)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public long count() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(o) FROM Orders o", Long.class)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long countByStatus(String status) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(o) FROM Orders o WHERE o.orderStatus = :status", Long.class)
                    .setParameter("status", status)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public double sumRevenue() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            Double result = em.createQuery(
                    "SELECT SUM(o.finalAmount) FROM Orders o WHERE o.orderStatus = 'DELIVERED'",
                    Double.class)
                    .getSingleResult();
            return result != null ? result : 0;
        } finally {
            em.close();
        }
    }

    public void save(Orders order) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (order.getOrderId() == 0) {
                em.persist(order);
            } else {
                em.merge(order);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}
