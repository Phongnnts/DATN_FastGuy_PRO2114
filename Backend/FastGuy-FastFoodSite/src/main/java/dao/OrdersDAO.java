package dao;

import entity.Orders;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import utils.DatabaseUtil;

import java.math.BigDecimal;
import java.sql.Timestamp;
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

    public Orders findByOrderCode(String orderCode) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Orders> list = em.createQuery(
                    "SELECT o FROM Orders o WHERE o.orderCode = :code", Orders.class)
                    .setParameter("code", orderCode)
                    .getResultList();
            return list.isEmpty() ? null : list.get(0);
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

    public List<Orders> findPendingRefunds() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT o FROM Orders o WHERE o.refundStatus = 'PENDING' ORDER BY o.cancelledAt DESC",
                    Orders.class)
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
            BigDecimal result = em.createQuery(
                    "SELECT SUM(o.finalAmount) FROM Orders o WHERE o.orderStatus = 'DELIVERED' AND o.paymentStatus = 'PAID'",
                    BigDecimal.class)
                    .getSingleResult();
            return result != null ? result.doubleValue() : 0.0;
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
            BigDecimal result = em.createQuery(
                    "SELECT SUM(o.finalAmount) FROM Orders o WHERE o.orderStatus = 'DELIVERED' AND o.paymentStatus = 'PAID' AND o.deliveredAt >= :start AND o.deliveredAt < :end",
                    BigDecimal.class)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getSingleResult();
            return result != null ? result.doubleValue() : 0.0;
        } finally {
            em.close();
        }
    }

    public List<Map<String, Object>> sumRevenueByMonth() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT MONTH(o.delivered_at) AS m, YEAR(o.delivered_at) AS y, SUM(o.final_amount) AS rev " +
                    "FROM Orders o WHERE o.order_status = 'DELIVERED' AND o.payment_status = 'PAID' " +
                    "GROUP BY YEAR(o.delivered_at), MONTH(o.delivered_at) " +
                    "ORDER BY y ASC, m ASC")
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

    public List<Map<String, Object>> sumRevenueByCustomRange(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT MONTH(o.delivered_at) AS m, YEAR(o.delivered_at) AS y, SUM(o.final_amount) AS rev " +
                    "FROM Orders o WHERE o.order_status = 'DELIVERED' AND o.payment_status = 'PAID' " +
                    "AND o.delivered_at >= ?1 AND o.delivered_at < ?2 " +
                    "GROUP BY YEAR(o.delivered_at), MONTH(o.delivered_at) " +
                    "ORDER BY y ASC, m ASC")
                    .setParameter(1, start)
                    .setParameter(2, end)
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
                    "JOIN Orders o ON oi.order_id = o.order_id " +
                    "WHERE o.order_status = 'DELIVERED' AND o.payment_status = 'PAID' " +
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

    public long countByShipperAndStatus(int shipperId, String status, LocalDate date) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder(
                    "SELECT COUNT(o) FROM Orders o WHERE o.shipper.userId = :sid AND o.orderStatus = :status");
            if (date != null) {
                jpql.append(" AND o.createdAt BETWEEN :start AND :end");
            }
            var q = em.createQuery(jpql.toString(), Long.class)
                    .setParameter("sid", shipperId)
                    .setParameter("status", status);
            if (date != null) {
                q.setParameter("start", date.atStartOfDay());
                q.setParameter("end", date.plusDays(1).atStartOfDay());
            }
            return q.getSingleResult();
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

    public double sumRevenueByDateRange(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            BigDecimal result = em.createQuery(
                    "SELECT SUM(o.finalAmount) FROM Orders o WHERE o.orderStatus = 'DELIVERED' AND o.paymentStatus = 'PAID' AND o.deliveredAt >= :start AND o.deliveredAt < :end",
                    BigDecimal.class)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getSingleResult();
            return result != null ? result.doubleValue() : 0.0;
        } finally {
            em.close();
        }
    }

    public long countByStatusAndDateRange(String status, LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(o) FROM Orders o WHERE o.orderStatus = :status AND o.deliveredAt >= :start AND o.deliveredAt < :end",
                    Long.class)
                    .setParameter("status", status)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long countAllByDateRange(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(o) FROM Orders o WHERE o.createdAt >= :start AND o.createdAt < :end", Long.class)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public List<Map<String, Object>> findTopProductsByDateRange(LocalDateTime start, LocalDateTime end, int limit) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT TOP " + limit + " p.name, SUM(oi.quantity) AS sold, SUM(oi.quantity * oi.unit_price) AS rev " +
                    "FROM OrderItem oi JOIN Product p ON oi.product_id = p.product_id " +
                    "JOIN Orders o ON oi.order_id = o.order_id " +
                    "WHERE o.delivered_at >= :start AND o.delivered_at < :end AND o.order_status = 'DELIVERED' AND o.payment_status = 'PAID' " +
                    "GROUP BY p.name ORDER BY sold DESC")
                    .setParameter("start", java.sql.Timestamp.valueOf(start))
                    .setParameter("end", java.sql.Timestamp.valueOf(end))
                    .getResultList();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", row[0]);
                item.put("sold", ((Number) row[1]).intValue());
                item.put("revenue", row[2] != null ? ((Number) row[2]).doubleValue() : 0);
                result.add(item);
            }
            return result;
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

    public List<Map<String, Object>> revenueByDay(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT CAST(o.delivered_at AS DATE) AS d, SUM(o.final_amount) AS rev, COUNT(*) AS cnt " +
                    "FROM Orders o WHERE o.order_status = 'DELIVERED' AND o.payment_status = 'PAID' " +
                    "AND o.delivered_at >= ?1 AND o.delivered_at < ?2 " +
                    "GROUP BY CAST(o.delivered_at AS DATE) ORDER BY d")
                    .setParameter(1, Timestamp.valueOf(start)).setParameter(2, Timestamp.valueOf(end)).getResultList();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("date", String.valueOf(row[0]));
                item.put("revenue", ((Number) row[1]).doubleValue());
                item.put("orders", ((Number) row[2]).intValue());
                result.add(item);
            }
            return result;
        } finally {
            em.close();
        }
    }

    public List<Map<String, Object>> ordersByStatusInPeriod(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT o.order_status AS s, COUNT(*) AS cnt " +
                    "FROM Orders o WHERE o.created_at >= ?1 AND o.created_at < ?2 " +
                    "GROUP BY o.order_status ORDER BY cnt DESC")
                    .setParameter(1, Timestamp.valueOf(start)).setParameter(2, Timestamp.valueOf(end)).getResultList();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("status", String.valueOf(row[0]));
                item.put("count", ((Number) row[1]).intValue());
                result.add(item);
            }
            return result;
        } finally {
            em.close();
        }
    }

    public List<Map<String, Object>> revenueByCategory(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT c.name AS cat, SUM(oi.total_price) AS rev " +
                    "FROM OrderItem oi " +
                    "JOIN Orders o ON oi.order_id = o.order_id " +
                    "JOIN Product p ON oi.product_id = p.product_id " +
                    "JOIN Category c ON p.category_id = c.category_id " +
                    "WHERE o.order_status = 'DELIVERED' AND o.payment_status = 'PAID' " +
                    "AND o.delivered_at >= ?1 AND o.delivered_at < ?2 " +
                    "GROUP BY c.name ORDER BY rev DESC")
                    .setParameter(1, Timestamp.valueOf(start)).setParameter(2, Timestamp.valueOf(end)).getResultList();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("category", String.valueOf(row[0]));
                item.put("revenue", ((Number) row[1]).doubleValue());
                result.add(item);
            }
            return result;
        } finally {
            em.close();
        }
    }

    public List<Map<String, Object>> paymentMethodStats(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT o.payment_method AS pm, COUNT(*) AS cnt, SUM(o.final_amount) AS rev " +
                    "FROM Orders o WHERE o.order_status = 'DELIVERED' AND o.payment_status = 'PAID' " +
                    "AND o.delivered_at >= ?1 AND o.delivered_at < ?2 " +
                    "GROUP BY o.payment_method")
                    .setParameter(1, Timestamp.valueOf(start)).setParameter(2, Timestamp.valueOf(end)).getResultList();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("method", String.valueOf(row[0]));
                item.put("count", ((Number) row[1]).intValue());
                item.put("revenue", ((Number) row[2]).doubleValue());
                result.add(item);
            }
            return result;
        } finally {
            em.close();
        }
    }

    public double avgOrderValue(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            Double result = em.createQuery(
                    "SELECT AVG(o.finalAmount) FROM Orders o WHERE o.orderStatus = 'DELIVERED' AND o.paymentStatus = 'PAID' AND o.deliveredAt >= :start AND o.deliveredAt < :end",
                    Double.class)
                    .setParameter("start", start).setParameter("end", end)
                    .getSingleResult();
            return result != null ? result : 0.0;
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
