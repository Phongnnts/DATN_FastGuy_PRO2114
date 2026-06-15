package repository;

import config.HibernateConfig;
import entity.Review;
import jakarta.persistence.EntityManager;

import java.util.List;

public class ReviewRepository {

    public List<Review> findByProductId(Long productId) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery(
                    "SELECT r FROM Review r WHERE r.product.productId = :pid ORDER BY r.createdAt DESC",
                    Review.class)
                    .setParameter("pid", productId)
                    .getResultList();
        }
    }

    public boolean existsByOrderAndUser(Long orderId, Long userId) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            Long count = em.createQuery(
                    "SELECT COUNT(r) FROM Review r WHERE r.order.orderId = :oid AND r.user.userId = :uid",
                    Long.class)
                    .setParameter("oid", orderId)
                    .setParameter("uid", userId)
                    .getSingleResult();
            return count > 0;
        }
    }

    public Review save(Review review) {
        EntityManager em = HibernateConfig.getEntityManager();
        try {
            em.getTransaction().begin();
            if (review.getReviewId() == null) {
                em.persist(review);
            } else {
                review = em.merge(review);
            }
            em.getTransaction().commit();
            return review;
        } finally {
            em.close();
        }
    }
}
