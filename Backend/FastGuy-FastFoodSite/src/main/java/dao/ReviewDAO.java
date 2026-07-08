package dao;

import entity.Orders;
import entity.Product;
import entity.Review;
import entity.User;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.util.List;

public class ReviewDAO {
    public Review findByUserOrderProduct(int userId, int orderId, int productId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Review> list = em.createQuery(
                    "SELECT r FROM Review r WHERE r.user.userId = :uid AND r.order.orderId = :oid AND r.product.productId = :pid",
                    Review.class)
                    .setParameter("uid", userId)
                    .setParameter("oid", orderId)
                    .setParameter("pid", productId)
                    .getResultList();
            return list.isEmpty() ? null : list.get(0);
        } finally {
            em.close();
        }
    }

    public List<Review> findByProductId(int productId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT r FROM Review r WHERE r.product.productId = :pid ORDER BY r.createdAt DESC",
                    Review.class)
                    .setParameter("pid", productId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public boolean hasUserBoughtProduct(int userId, int orderId, int productId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                    "SELECT COUNT(oi) FROM OrderItem oi WHERE oi.order.orderId = :oid AND oi.order.user.userId = :uid AND oi.product.productId = :pid AND oi.order.orderStatus = 'DELIVERED'",
                    Long.class)
                    .setParameter("oid", orderId)
                    .setParameter("uid", userId)
                    .setParameter("pid", productId)
                    .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }

    public long countByProductId(int productId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(r) FROM Review r WHERE r.product.productId = :pid", Long.class)
                    .setParameter("pid", productId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public double averageByProductId(int productId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            Double avg = em.createQuery("SELECT AVG(r.rating) FROM Review r WHERE r.product.productId = :pid", Double.class)
                    .setParameter("pid", productId)
                    .getSingleResult();
            return avg != null ? avg : 0;
        } finally {
            em.close();
        }
    }

    public List<Review> findByOrderId(int orderId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT r FROM Review r WHERE r.order.orderId = :oid ORDER BY r.product.productId",
                    Review.class)
                    .setParameter("oid", orderId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public Review save(int userId, int orderId, int productId, int rating, String comment) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Review existing = findManaged(em, userId, orderId, productId);
            if (existing != null) {
                throw new IllegalArgumentException("Bạn đã đánh giá món này rồi");
            }
            Review review = new Review();
            review.setUser(em.getReference(User.class, userId));
            review.setOrder(em.getReference(Orders.class, orderId));
            review.setProduct(em.getReference(Product.class, productId));
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

    private Review findManaged(EntityManager em, int userId, int orderId, int productId) {
        List<Review> list = em.createQuery(
                "SELECT r FROM Review r WHERE r.user.userId = :uid AND r.order.orderId = :oid AND r.product.productId = :pid",
                Review.class)
                .setParameter("uid", userId)
                .setParameter("oid", orderId)
                .setParameter("pid", productId)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }
}
