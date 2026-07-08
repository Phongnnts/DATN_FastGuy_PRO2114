package dao;

import entity.Orders;
import entity.Review;
import entity.User;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.util.List;

public class ReviewDAO {

    public Review findByUserOrder(int userId, int orderId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Review> list = em.createQuery(
                    "SELECT r FROM Review r WHERE r.user.userId = :uid AND r.order.orderId = :oid",
                    Review.class)
                    .setParameter("uid", userId)
                    .setParameter("oid", orderId)
                    .getResultList();
            return list.isEmpty() ? null : list.get(0);
        } finally {
            em.close();
        }
    }

    public List<Review> findByOrderId(int orderId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT r FROM Review r WHERE r.order.orderId = :oid",
                    Review.class)
                    .setParameter("oid", orderId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public boolean isOrderDelivered(int userId, int orderId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                    "SELECT COUNT(o) FROM Orders o WHERE o.orderId = :oid AND o.user.userId = :uid AND o.orderStatus = 'DELIVERED'",
                    Long.class)
                    .setParameter("oid", orderId)
                    .setParameter("uid", userId)
                    .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }

    public Review save(int userId, int orderId, int rating, String comment) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Review existing = findByUserOrder(userId, orderId);
            if (existing != null) {
                throw new IllegalArgumentException("Bạn đã đánh giá đơn hàng này rồi");
            }
            Review review = new Review();
            review.setUser(em.getReference(User.class, userId));
            review.setOrder(em.getReference(Orders.class, orderId));
            review.setRating(rating);
            review.setComment(comment);
            review.setCreatedAt(java.time.LocalDateTime.now());
            em.persist(review);
            em.getTransaction().commit();
            return review;
        } catch (RuntimeException e) {
            em.getTransaction().rollback();
            throw e;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw new RuntimeException("Failed to save review: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }
}
