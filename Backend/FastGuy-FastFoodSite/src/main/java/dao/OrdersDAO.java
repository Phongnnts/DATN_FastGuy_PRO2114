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

    public Orders findByIdempotencyKey(String key) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Orders> list = em.createQuery(
                    "SELECT o FROM Orders o WHERE o.idempotencyKey = :key", Orders.class)
                    .setParameter("key", key)
                    .setMaxResults(1)
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
                    "SELECT o FROM Orders o WHERE o.orderStatus = :status ORDER BY o.createdAt ASC, o.orderId ASC",
                    Orders.class)
                    .setParameter("status", status)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Orders> findByStatusAndStaffShift(String status, int shiftId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT o FROM Orders o WHERE o.orderStatus = :status AND o.staffShift.shiftId = :shiftId ORDER BY o.createdAt ASC, o.orderId ASC", Orders.class)
                    .setParameter("status", status).setParameter("shiftId", shiftId).getResultList();
        } finally { em.close(); }
    }

    public List<Orders> findHandoverEligible(int shiftId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT o FROM Orders o WHERE o.orderStatus IN ('CONFIRMED','PREPARING','READY','DELIVERY_FAILED') AND (o.staffShift IS NULL OR o.staffShift.shiftId <> :shiftId) ORDER BY COALESCE(o.deliveryFailedAt, o.readyAt, o.confirmedAt, o.createdAt) ASC, o.orderId ASC", Orders.class)
                    .setParameter("shiftId", shiftId).getResultList();
        } finally { em.close(); }
    }

    public long countActiveOwnership(EntityManager em, int shiftId) {
        return em.createQuery("SELECT COUNT(o) FROM Orders o WHERE o.staffShift.shiftId = :shiftId AND o.orderStatus IN ('CONFIRMED','PREPARING','READY','DELIVERY_FAILED')", Long.class)
                .setParameter("shiftId", shiftId).getSingleResult();
    }

    public long countActiveOwnership(int shiftId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try { return countActiveOwnership(em, shiftId); } finally { em.close(); }
    }

    public List<Orders> lockActiveOwnership(EntityManager em, int shiftId) {
        return em.createQuery("SELECT o FROM Orders o WHERE o.staffShift.shiftId = :shiftId AND o.orderStatus IN ('CONFIRMED','PREPARING','READY','DELIVERY_FAILED') ORDER BY o.orderId", Orders.class)
                .setParameter("shiftId", shiftId).setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE).getResultList();
    }

    public List<Orders> findDeliveryFailureQueue() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT o FROM Orders o WHERE o.orderStatus = 'DELIVERY_FAILED' ORDER BY COALESCE(o.retryScheduledAt, o.deliveryFailedAt) ASC, o.orderId ASC",
                    Orders.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Orders> findDispatchCandidates() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT o FROM Orders o WHERE (o.orderStatus = 'READY' AND o.shipper IS NULL) "
                            + "OR o.orderStatus = 'DELIVERY_FAILED' "
                            + "ORDER BY CASE WHEN o.orderStatus = 'READY' THEN 0 ELSE 1 END, "
                            + "COALESCE(o.readyAt, o.deliveryFailedAt, o.createdAt) ASC, o.orderId ASC",
                    Orders.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Orders> findReadyWithoutShipperForClosing() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT o FROM Orders o WHERE o.orderStatus = 'READY' AND o.shipper IS NULL AND o.createdAt IS NOT NULL ORDER BY o.createdAt ASC, o.orderId ASC",
                    Orders.class).getResultList();
        } finally {
            em.close();
        }
    }

    public List<Orders> findExpiryCandidates(LocalDateTime oldestStatusEnteredAt) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT o FROM Orders o WHERE o.orderStatus IN ('PENDING','CONFIRMED','PREPARING','READY') AND o.statusEnteredAt <= :oldest ORDER BY o.statusEnteredAt, o.orderId", Orders.class)
                    .setParameter("oldest", oldestStatusEnteredAt).getResultList();
        } finally {
            em.close();
        }
    }

    public List<Orders> findCutoffCandidates() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT o FROM Orders o WHERE o.orderStatus IN ('PENDING','CONFIRMED','PREPARING','READY') ORDER BY o.statusEnteredAt, o.orderId", Orders.class).getResultList();
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

    public List<Orders> findRefunds(String status, LocalDate from, LocalDate to, String search) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT o FROM Orders o WHERE o.refundStatus IS NOT NULL");
            if (status != null && !status.isBlank()) jpql.append(" AND o.refundStatus = :status");
            if (from != null) jpql.append(" AND o.createdAt >= :from");
            if (to != null) jpql.append(" AND o.createdAt < :to");
            if (search != null && !search.isBlank()) {
                jpql.append(" AND (LOWER(o.orderCode) LIKE :search OR LOWER(o.customerName) LIKE :search OR LOWER(o.customerPhone) LIKE :search)");
            }
            jpql.append(" ORDER BY o.cancelledAt DESC");
            var q = em.createQuery(jpql.toString(), Orders.class);
            if (status != null && !status.isBlank()) q.setParameter("status", status);
            if (from != null) q.setParameter("from", from.atStartOfDay());
            if (to != null) q.setParameter("to", to.plusDays(1).atStartOfDay());
            if (search != null && !search.isBlank()) q.setParameter("search", "%" + search.toLowerCase() + "%");
            return q.getResultList();
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

    public Object[] summarizeShift(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT "
                    + "SUM(CASE WHEN o.orderStatus = 'DELIVERED' AND o.deliveredAt >= :start AND o.deliveredAt < :end THEN 1 ELSE 0 END), "
                    + "SUM(CASE WHEN o.orderStatus = 'CANCELLED' AND o.cancelledAt >= :start AND o.cancelledAt < :end THEN 1 ELSE 0 END), "
                    + "COALESCE(SUM(CASE WHEN o.orderStatus = 'DELIVERED' AND o.deliveredAt >= :start AND o.deliveredAt < :end THEN COALESCE(o.finalAmount, 0) - COALESCE(o.refundAmount, 0) ELSE 0 END), 0) "
                    + "FROM Orders o", Object[].class)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long countPendingRefunds() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(o) FROM Orders o WHERE o.refundStatus = 'PENDING'", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }

    public long countActiveByDateRange(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(o) FROM Orders o WHERE o.createdAt >= :start AND o.createdAt < :end AND o.orderStatus NOT IN ('DELIVERED','CANCELLED','RETURNED_TO_STORE')", Long.class)
                    .setParameter("start", start).setParameter("end", end).getSingleResult();
        } finally {
            em.close();
        }
    }

    public long countOverdueActive(LocalDateTime threshold) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(o) FROM Orders o WHERE o.orderStatus IN ('PENDING', 'CONFIRMED', 'PREPARING') AND o.createdAt < :threshold", Long.class)
                    .setParameter("threshold", threshold)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public List<Orders> findPriorityOrders() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT o FROM Orders o WHERE o.orderStatus IN ('PENDING', 'CONFIRMED', 'PREPARING', 'READY') ORDER BY o.createdAt ASC, o.orderId ASC", Orders.class)
                    .setMaxResults(6)
                    .getResultList();
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
                    "SELECT COUNT(o) FROM Orders o WHERE o.createdAt >= :start AND o.createdAt < :end",
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
                    "SELECT o FROM Orders o WHERE o.shipper.userId = :sid ORDER BY o.createdAt DESC, o.orderId DESC",
                    Orders.class)
                    .setParameter("sid", shipperId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public long countActiveByShipper(int shipperId, LocalDateTime shiftStart) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(o) FROM Orders o WHERE o.shipper.userId = :shipperId " +
                            "AND o.orderStatus IN ('ASSIGNED','PICKED_UP') AND o.assignedAt >= :shiftStart",
                    Long.class)
                    .setParameter("shipperId", shipperId)
                    .setParameter("shiftStart", shiftStart)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public List<Orders> findStaffHistory(int page, int size, String status, LocalDateTime from, LocalDateTime to, String search) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            var query = em.createQuery(staffHistoryJpql("SELECT o", status, from, to, search)
                    + " ORDER BY COALESCE(o.deliveredAt, o.cancelledAt, o.createdAt) DESC, o.orderId DESC", Orders.class);
            bindStaffHistory(query, status, from, to, search);
            return query.setFirstResult((page - 1) * size).setMaxResults(size).getResultList();
        } finally {
            em.close();
        }
    }

    public long countStaffHistory(String status, LocalDateTime from, LocalDateTime to, String search) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            var query = em.createQuery(staffHistoryJpql("SELECT COUNT(o)", status, from, to, search), Long.class);
            bindStaffHistory(query, status, from, to, search);
            return query.getSingleResult();
        } finally {
            em.close();
        }
    }

    public List<Orders> findStaffHistoryForExport(String status, LocalDateTime from, LocalDateTime to, String search) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            var query = em.createQuery(staffHistoryJpql("SELECT o", status, from, to, search)
                    + " ORDER BY COALESCE(o.deliveredAt, o.cancelledAt, o.createdAt) DESC, o.orderId DESC", Orders.class);
            bindStaffHistory(query, status, from, to, search);
            return query.setMaxResults(10000).getResultList();
        } finally {
            em.close();
        }
    }

    private String staffHistoryJpql(String select, String status, LocalDateTime from, LocalDateTime to, String search) {
        StringBuilder jpql = new StringBuilder(select + " FROM Orders o WHERE o.orderStatus IN ('DELIVERED','CANCELLED')");
        if (status != null) jpql.append(" AND o.orderStatus = :status");
        if (from != null) jpql.append(" AND COALESCE(o.deliveredAt, o.cancelledAt, o.createdAt) >= :from");
        if (to != null) jpql.append(" AND COALESCE(o.deliveredAt, o.cancelledAt, o.createdAt) < :to");
        if (search != null) jpql.append(" AND (LOWER(o.orderCode) LIKE :search OR LOWER(o.customerName) LIKE :search OR LOWER(o.customerPhone) LIKE :search)");
        return jpql.toString();
    }

    private void bindStaffHistory(Query query, String status, LocalDateTime from, LocalDateTime to, String search) {
        if (status != null) query.setParameter("status", status);
        if (from != null) query.setParameter("from", from);
        if (to != null) query.setParameter("to", to);
        if (search != null) query.setParameter("search", "%" + search.toLowerCase(Locale.ROOT) + "%");
    }

    public List<Orders> findHistoryByShipperId(int shipperId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT o FROM Orders o WHERE o.shipper.userId = :shipperId AND o.orderStatus IN ('DELIVERED','CANCELLED') ORDER BY COALESCE(o.deliveredAt, o.cancelledAt, o.createdAt) DESC, o.orderId DESC",
                    Orders.class)
                    .setParameter("shipperId", shipperId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Orders> findHistoryByShipperId(int shipperId, int page, int size, LocalDateTime from, LocalDateTime to) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder(
                    "SELECT o FROM Orders o WHERE o.shipper.userId = :shipperId AND o.orderStatus IN ('DELIVERED','CANCELLED')");
            if (from != null) jpql.append(" AND o.createdAt >= :from");
            if (to != null) jpql.append(" AND o.createdAt < :to");
            jpql.append(" ORDER BY COALESCE(o.deliveredAt, o.cancelledAt, o.createdAt) DESC, o.orderId DESC");
            var q = em.createQuery(jpql.toString(), Orders.class)
                    .setParameter("shipperId", shipperId);
            if (from != null) q.setParameter("from", from);
            if (to != null) q.setParameter("to", to);
            return q.setFirstResult((page - 1) * size).setMaxResults(size).getResultList();
        } finally {
            em.close();
        }
    }

    public long countHistoryByShipperId(int shipperId, LocalDateTime from, LocalDateTime to) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder(
                    "SELECT COUNT(o) FROM Orders o WHERE o.shipper.userId = :shipperId AND o.orderStatus IN ('DELIVERED','CANCELLED')");
            if (from != null) jpql.append(" AND o.createdAt >= :from");
            if (to != null) jpql.append(" AND o.createdAt < :to");
            var q = em.createQuery(jpql.toString(), Long.class)
                    .setParameter("shipperId", shipperId);
            if (from != null) q.setParameter("from", from);
            if (to != null) q.setParameter("to", to);
            return q.getSingleResult();
        } finally {
            em.close();
        }
    }

    public double sumCodCollectedByShipperAndDateRange(int shipperId, LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            BigDecimal result = em.createQuery(
                    "SELECT SUM(o.codCollectedAmount) FROM Orders o WHERE o.shipper.userId = :shipperId AND o.paymentMethod = 'COD' AND o.deliveredAt >= :start AND o.deliveredAt < :end AND o.codCollectedAmount IS NOT NULL",
                    BigDecimal.class)
                    .setParameter("shipperId", shipperId)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getSingleResult();
            return result != null ? result.doubleValue() : 0.0;
        } finally {
            em.close();
        }
    }

    public long countDeliveredByShipperAndDateRange(int shipperId, LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(o) FROM Orders o WHERE o.shipper.userId = :shipperId AND o.orderStatus = 'DELIVERED' AND o.deliveredAt >= :start AND o.deliveredAt < :end",
                    Long.class)
                    .setParameter("shipperId", shipperId)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public double sumRevenueByDateRange(LocalDateTime start, LocalDateTime end) {
        return sumRevenueDecimalByDateRange(start, end).doubleValue();
    }

    public BigDecimal sumRevenueDecimalByDateRange(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            BigDecimal result = em.createQuery(
                    "SELECT SUM(o.finalAmount) FROM Orders o WHERE o.orderStatus = 'DELIVERED' AND o.paymentStatus = 'PAID' AND o.deliveredAt >= :start AND o.deliveredAt < :end",
                    BigDecimal.class)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getSingleResult();
            return result != null ? result : BigDecimal.ZERO;
        } finally {
            em.close();
        }
    }

    public long[] operationalCohortSummary(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            Object[] row = em.createQuery(
                    "SELECT COUNT(o), SUM(CASE WHEN o.orderStatus = 'DELIVERED' THEN 1 ELSE 0 END) FROM Orders o WHERE o.createdAt >= :start AND o.createdAt < :end",
                    Object[].class)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getSingleResult();
            return new long[]{((Number) row[0]).longValue(), row[1] instanceof Number delivered ? delivered.longValue() : 0L};
        } finally {
            em.close();
        }
    }

    public Map<String, Double> financialBreakdown(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            Object[] row = em.createQuery(
                    "SELECT COALESCE(SUM(o.totalAmount), 0), COALESCE(SUM(o.shippingFee), 0), COALESCE(SUM(o.serviceFee), 0), COALESCE(SUM(o.discountAmount), 0), COALESCE(SUM(o.finalAmount), 0) FROM Orders o WHERE o.orderStatus = 'DELIVERED' AND o.paymentStatus = 'PAID' AND o.deliveredAt >= :start AND o.deliveredAt < :end",
                    Object[].class)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getSingleResult();
            Map<String, Double> result = new HashMap<>();
            result.put("itemRevenue", ((Number) row[0]).doubleValue());
            result.put("shippingRevenue", ((Number) row[1]).doubleValue());
            result.put("serviceFeeRevenue", ((Number) row[2]).doubleValue());
            result.put("discountTotal", ((Number) row[3]).doubleValue());
            result.put("grossRevenue", ((Number) row[4]).doubleValue());
            return result;
        } finally {
            em.close();
        }
    }

    public List<Orders> findAllByCreatedAtRange(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            String jpql = "SELECT o FROM Orders o";
            if (start != null) jpql += " WHERE o.createdAt >= :start";
            if (end != null) jpql += start == null ? " WHERE o.createdAt < :end" : " AND o.createdAt < :end";
            jpql += " ORDER BY o.createdAt DESC";
            var query = em.createQuery(jpql, Orders.class);
            if (start != null) query.setParameter("start", start);
            if (end != null) query.setParameter("end", end);
            return query.getResultList();
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

    public double sumRefundsInRange(LocalDateTime start, LocalDateTime end) {
        return sumRefundsDecimalInRange(start, end).doubleValue();
    }

    public BigDecimal sumRefundsDecimalInRange(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            BigDecimal result = em.createQuery(
                    "SELECT SUM(o.refundAmount) FROM Orders o WHERE o.refundStatus = 'REFUNDED' AND o.refundedAt >= :start AND o.refundedAt < :end",
                    BigDecimal.class)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getSingleResult();
            return result != null ? result : BigDecimal.ZERO;
        } finally {
            em.close();
        }
    }

    public long countRefundsInRange(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(o) FROM Orders o WHERE o.refundStatus = 'REFUNDED' AND o.refundedAt >= :start AND o.refundedAt < :end",
                    Long.class)
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
                    "SELECT TOP " + limit + " p.product_id, p.name, SUM(oi.quantity) AS sold, SUM(oi.total_price) AS rev " +
                    "FROM OrderItem oi JOIN Product p ON oi.product_id = p.product_id " +
                    "JOIN Orders o ON oi.order_id = o.order_id " +
                    "WHERE o.delivered_at >= :start AND o.delivered_at < :end AND o.order_status = 'DELIVERED' AND o.payment_status = 'PAID' " +
                    "GROUP BY p.product_id, p.name ORDER BY sold DESC")
                    .setParameter("start", java.sql.Timestamp.valueOf(start))
                    .setParameter("end", java.sql.Timestamp.valueOf(end))
                    .getResultList();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("productId", ((Number) row[0]).intValue());
                item.put("name", row[1]);
                item.put("sold", ((Number) row[2]).intValue());
                item.put("revenue", row[3] != null ? ((Number) row[3]).doubleValue() : 0);
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

    public List<Orders> findDeliveredByStaffId(int staffId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT o FROM Orders o WHERE o.staff.userId = :staffId AND o.orderStatus = 'DELIVERED' ORDER BY o.deliveredAt DESC, o.createdAt DESC",
                    Orders.class)
                    .setParameter("staffId", staffId)
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

    public List<Map<String, Object>> monthlyFinancialTrend(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery(
                    "WITH nums AS (SELECT TOP (DATEDIFF(MONTH, ?1, DATEADD(DAY,-1,?2)) + 1) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) - 1 n FROM sys.all_objects a CROSS JOIN sys.all_objects b), " +
                    "months AS (SELECT DATEADD(MONTH, n, DATEFROMPARTS(YEAR(?1), MONTH(?1), 1)) month_start FROM nums), " +
                    "gross AS (SELECT DATEFROMPARTS(YEAR(delivered_at), MONTH(delivered_at), 1) month_start, SUM(final_amount) amount FROM Orders " +
                    "WHERE order_status='DELIVERED' AND payment_status='PAID' AND delivered_at>=?1 AND delivered_at<?2 GROUP BY DATEFROMPARTS(YEAR(delivered_at), MONTH(delivered_at), 1)), " +
                    "refunds AS (SELECT DATEFROMPARTS(YEAR(refunded_at), MONTH(refunded_at), 1) month_start, SUM(refund_amount) amount FROM Orders " +
                    "WHERE refund_status='REFUNDED' AND refunded_at>=?1 AND refunded_at<?2 GROUP BY DATEFROMPARTS(YEAR(refunded_at), MONTH(refunded_at), 1)) " +
                    "SELECT YEAR(m.month_start), MONTH(m.month_start), COALESCE(g.amount,0), COALESCE(r.amount,0), COALESCE(g.amount,0)-COALESCE(r.amount,0) " +
                    "FROM months m LEFT JOIN gross g ON g.month_start=m.month_start LEFT JOIN refunds r ON r.month_start=m.month_start ORDER BY m.month_start")
                    .setParameter(1, Timestamp.valueOf(start)).setParameter(2, Timestamp.valueOf(end)).getResultList();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("year", ((Number) row[0]).intValue());
                item.put("month", ((Number) row[1]).intValue());
                item.put("grossRevenue", ((Number) row[2]).doubleValue());
                item.put("refundTotal", ((Number) row[3]).doubleValue());
                item.put("netCashRevenue", ((Number) row[4]).doubleValue());
                result.add(item);
            }
            return result;
        } finally { em.close(); }
    }

    public List<Map<String, Object>> revenueByHour(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT DATEPART(HOUR, o.delivered_at) h, COUNT(*) cnt, SUM(o.final_amount) rev FROM Orders o " +
                    "WHERE o.order_status = 'DELIVERED' AND o.payment_status = 'PAID' AND o.delivered_at >= ?1 AND o.delivered_at < ?2 " +
                    "GROUP BY DATEPART(HOUR, o.delivered_at) ORDER BY h")
                    .setParameter(1, Timestamp.valueOf(start)).setParameter(2, Timestamp.valueOf(end)).getResultList();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("hour", ((Number) row[0]).intValue());
                item.put("orders", ((Number) row[1]).intValue());
                item.put("revenue", ((Number) row[2]).doubleValue());
                result.add(item);
            }
            return result;
        } finally { em.close(); }
    }

    public List<Map<String, Object>> performanceByWeekday(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT ((DATEDIFF(DAY, '19000101', CAST(o.created_at AS date)) % 7) + 1) weekday, COUNT(*) total, " +
                    "SUM(CASE WHEN o.order_status = 'DELIVERED' THEN 1 ELSE 0 END) completed FROM Orders o " +
                    "WHERE o.created_at >= ?1 AND o.created_at < ?2 GROUP BY ((DATEDIFF(DAY, '19000101', CAST(o.created_at AS date)) % 7) + 1) ORDER BY weekday")
                    .setParameter(1, Timestamp.valueOf(start)).setParameter(2, Timestamp.valueOf(end)).getResultList();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> item = new HashMap<>();
                long total = ((Number) row[1]).longValue();
                long completed = row[2] instanceof Number number ? number.longValue() : 0L;
                item.put("weekday", ((Number) row[0]).intValue());
                item.put("orders", total);
                item.put("completed", completed);
                item.put("completionRate", total == 0 ? 0.0 : completed * 100.0 / total);
                result.add(item);
            }
            return result;
        } finally { em.close(); }
    }

    public List<Map<String, Object>> refundTrend(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT CAST(o.refunded_at AS date) d, COUNT(*) cnt, SUM(o.refund_amount) amount FROM Orders o " +
                    "WHERE o.refund_status = 'REFUNDED' AND o.refunded_at >= ?1 AND o.refunded_at < ?2 " +
                    "GROUP BY CAST(o.refunded_at AS date) ORDER BY d")
                    .setParameter(1, Timestamp.valueOf(start)).setParameter(2, Timestamp.valueOf(end)).getResultList();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("date", String.valueOf(row[0]));
                item.put("count", ((Number) row[1]).intValue());
                item.put("amount", ((Number) row[2]).doubleValue());
                result.add(item);
            }
            return result;
        } finally { em.close(); }
    }

    public List<Map<String, Object>> exceptionReasons(LocalDateTime start, LocalDateTime end) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT x.reason, COUNT(*) cnt FROM (SELECT COALESCE(NULLIF(o.delivery_failure_code,''), NULLIF(o.failure_reason,''), 'UNKNOWN') reason " +
                    "FROM Orders o WHERE o.created_at >= ?1 AND o.created_at < ?2 AND o.order_status IN ('CANCELLED','DELIVERY_FAILED','RETURNED_TO_STORE')) x " +
                    "GROUP BY x.reason ORDER BY cnt DESC")
                    .setParameter(1, Timestamp.valueOf(start)).setParameter(2, Timestamp.valueOf(end)).getResultList();
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("reason", String.valueOf(row[0]));
                item.put("count", ((Number) row[1]).intValue());
                result.add(item);
            }
            return result;
        } finally { em.close(); }
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
