package dao;

import entity.Orders;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import utils.DatabaseUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

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

    public long countToday() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            LocalDateTime start = LocalDate.now().atStartOfDay();
            LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
            return em.createQuery(
                    "SELECT COUNT(o) FROM Orders o WHERE o.createdAt BETWEEN :start AND :end",
                    Long.class)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public double sumRevenueToday() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            LocalDateTime start = LocalDate.now().atStartOfDay();
            LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
            Double result = em.createQuery(
                    "SELECT SUM(o.finalAmount) FROM Orders o WHERE o.orderStatus = 'DELIVERED' AND o.createdAt BETWEEN :start AND :end",
                    Double.class)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getSingleResult();
            return result != null ? result : 0;
        } finally {
            em.close();
        }
    }

    public List<Map<String, Object>> sumRevenueByMonth() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT MONTH(o.created_at) AS m, YEAR(o.created_at) AS y, SUM(o.final_amount) AS rev " +
                    "FROM Orders o WHERE o.order_status = 'DELIVERED' " +
                    "GROUP BY YEAR(o.created_at), MONTH(o.created_at) " +
                    "ORDER BY y DESC, m DESC")
                    .getResultList();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("month", ((Number) row[0]).intValue());
                item.put("year", ((Number) row[1]).intValue());
                item.put("revenue", ((Number) row[2]).doubleValue());
                result.add(item);
            }
            return result;
        } finally {
            em.close();
        }
    }

    public List<Map<String, Object>> findTopProducts(int limit) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            Query query = em.createNativeQuery(
                    "SELECT TOP " + limit + " p.name, SUM(oi.quantity) AS sold " +
                    "FROM OrderItem oi JOIN Product p ON oi.product_id = p.product_id " +
                    "GROUP BY p.name ORDER BY sold DESC");
            List<Object[]> rows = query.getResultList();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", row[0]);
                item.put("sold", ((Number) row[1]).intValue());
                result.add(item);
            }
            return result;
        } finally {
            em.close();
        }
    }

    public List<Orders> findByStatusAndNoShipper(String status) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT o FROM Orders o WHERE o.orderStatus = :status AND o.shipper IS NULL ORDER BY o.createdAt DESC",
                    Orders.class)
                    .setParameter("status", status)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Orders> findByShipperId(int shipperId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT o FROM Orders o WHERE o.shipper.userId = :sid ORDER BY o.createdAt DESC",
                    Orders.class)
                    .setParameter("sid", shipperId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Orders> findByShipperIdAndStatus(int shipperId, String status) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT o FROM Orders o WHERE o.shipper.userId = :sid AND o.orderStatus = :status ORDER BY o.createdAt DESC",
                    Orders.class)
                    .setParameter("sid", shipperId)
                    .setParameter("status", status)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void save(Orders order) throws RuntimeException {
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
            throw new RuntimeException("Failed to save order: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }
}
