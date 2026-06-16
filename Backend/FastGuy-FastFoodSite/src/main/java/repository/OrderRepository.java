package repository;

import config.HibernateConfig;
import entity.Orders;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class OrderRepository {

    public List<Orders> findByShipperId(Long shipperId) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery(
                    "SELECT o FROM Orders o WHERE o.shipper.userId = :sid ORDER BY o.createdAt DESC",
                    Orders.class)
                    .setParameter("sid", shipperId)
                    .getResultList();
        }
    }

    public List<Orders> findByUserId(Long userId) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery(
                    "SELECT o FROM Orders o WHERE o.user.userId = :uid ORDER BY o.createdAt DESC",
                    Orders.class)
                    .setParameter("uid", userId)
                    .getResultList();
        }
    }

    public Optional<Orders> findById(Long orderId) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return Optional.ofNullable(em.find(Orders.class, orderId));
        }
    }

    public Optional<Orders> findByOrderCode(String orderCode) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery("SELECT o FROM Orders o WHERE o.orderCode = :code", Orders.class)
                    .setParameter("code", orderCode)
                    .getResultStream().findFirst();
        }
    }

    public Optional<Orders> findByOrderCodeAndCustomerPhone(String orderCode, String phone) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery(
                    "SELECT o FROM Orders o WHERE o.orderCode = :code AND o.customerPhone = :phone",
                    Orders.class)
                    .setParameter("code", orderCode)
                    .setParameter("phone", phone)
                    .getResultStream().findFirst();
        }
    }

    public List<Orders> findByStaffId(Long staffId) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery(
                    "SELECT o FROM Orders o WHERE o.staff.userId = :sid ORDER BY o.createdAt DESC",
                    Orders.class)
                    .setParameter("sid", staffId)
                    .getResultList();
        }
    }

    public List<Orders> findAll() {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery("SELECT o FROM Orders o ORDER BY o.createdAt DESC", Orders.class)
                    .getResultList();
        }
    }

    public Orders save(Orders order) {
        EntityManager em = HibernateConfig.getEntityManager();
        try {
            em.getTransaction().begin();
            if (order.getOrderId() == null) {
                em.persist(order);
            } else {
                order = em.merge(order);
            }
            em.getTransaction().commit();
            return order;
        } finally {
            em.close();
        }
    }

    public long count() {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery("SELECT COUNT(o) FROM Orders o", Long.class)
                    .getSingleResult();
        }
    }

    public long countByStatus(String status) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery("SELECT COUNT(o) FROM Orders o WHERE o.orderStatus = :st", Long.class)
                    .setParameter("st", status)
                    .getSingleResult();
        }
    }

    public java.math.BigDecimal sumRevenue() {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            Double result = em.createQuery(
                    "SELECT SUM(o.finalAmount) FROM Orders o WHERE o.orderStatus = 'DELIVERED'", Double.class)
                    .getSingleResult();
            return result != null ? java.math.BigDecimal.valueOf(result) : java.math.BigDecimal.ZERO;
        }
    }

    public List<Orders> findRecent(int limit) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery("SELECT o FROM Orders o ORDER BY o.createdAt DESC", Orders.class)
                    .setMaxResults(limit)
                    .getResultList();
        }
    }

    public String generateOrderCode() {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            Long count = em.createQuery("SELECT COUNT(o) FROM Orders o", Long.class)
                    .getSingleResult();
            return String.format("FG-%tY%<tm%<td-%05d", new java.util.Date(), count + 1);
        }
    }
}
