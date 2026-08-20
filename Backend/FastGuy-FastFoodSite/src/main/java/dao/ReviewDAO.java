package dao;

import entity.Orders;
import entity.Review;
import entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

import java.time.LocalDateTime;
import java.util.List;

public class ReviewDAO {
    public record FeaturedReview(int reviewId, int rating, String comment, String userName, String avatarUrl,
            LocalDateTime createdAt) {}

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

    public List<FeaturedReview> findFeatured(int limit) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT new dao.ReviewDAO$FeaturedReview(r.reviewId, r.rating, r.comment, u.fullName, u.avatarUrl, r.createdAt) FROM Review r JOIN r.user u WHERE r.featured = true AND r.homepageConsent = true AND r.comment IS NOT NULL AND TRIM(r.comment) <> '' AND u.status = 'ACTIVE' AND u.fullName IS NOT NULL AND TRIM(u.fullName) <> '' AND r.createdAt IS NOT NULL ORDER BY r.createdAt DESC, r.reviewId DESC", FeaturedReview.class)
                    .setMaxResults(limit).getResultList();
        } finally { em.close(); }
    }

    public Review findByOrderId(int orderId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Review> reviews = em.createQuery("SELECT r FROM Review r JOIN FETCH r.user WHERE r.order.orderId = :orderId", Review.class)
                    .setParameter("orderId", orderId).setMaxResults(1).getResultList();
            return reviews.isEmpty() ? null : reviews.get(0);
        } finally { em.close(); }
    }

    public Review setFeaturedByOrderId(int orderId, boolean featured) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            List<Review> reviews = em.createQuery("SELECT r FROM Review r LEFT JOIN FETCH r.user JOIN FETCH r.order WHERE r.order.orderId = :orderId", Review.class)
                    .setParameter("orderId", orderId).setLockMode(LockModeType.PESSIMISTIC_WRITE).setMaxResults(1).getResultList();
            if (reviews.isEmpty()) throw new IllegalArgumentException("Review not found");
            Review review = reviews.get(0);
            if (featured && (!Boolean.TRUE.equals(review.getHomepageConsent()) || review.getComment() == null || review.getComment().isBlank() || review.getUser() == null

                    || !"ACTIVE".equals(review.getUser().getStatus()) || review.getUser().getFullName() == null
                    || review.getUser().getFullName().isBlank() || review.getCreatedAt() == null)) {
                throw new IllegalStateException("Review is not eligible for homepage");
            }
            review.setFeatured(featured);
            em.getTransaction().commit();
            return review;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally { em.close(); }
    }

    public Review save(int userId, int orderId, int rating, String comment, boolean homepageConsent) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null || order.getUser() == null || order.getUser().getUserId() != userId || !"DELIVERED".equals(order.getOrderStatus())) {
                throw new IllegalArgumentException("Chỉ được đánh giá đơn hàng đã giao thành công của bạn");
            }
            Long existing = em.createQuery("SELECT COUNT(r) FROM Review r WHERE r.user.userId = :uid AND r.order.orderId = :oid", Long.class)
                    .setParameter("uid", userId).setParameter("oid", orderId).getSingleResult();
            if (existing > 0) throw new IllegalStateException("Bạn đã đánh giá đơn hàng này rồi");
            Review review = new Review();
            review.setUser(em.getReference(User.class, userId));
            review.setOrder(order);
            review.setRating(rating);
            review.setComment(comment);
            review.setHomepageConsent(homepageConsent);
            review.setCreatedAt(java.time.LocalDateTime.now());
            em.persist(review);
            em.getTransaction().commit();
            return review;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
