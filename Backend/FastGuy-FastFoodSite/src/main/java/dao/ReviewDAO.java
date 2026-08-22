package dao;

import entity.Orders;
import entity.Product;
import entity.Review;
import entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ReviewDAO {
    public static class AlreadyReviewedException extends IllegalStateException {
        public AlreadyReviewedException() { super("ALREADY_REVIEWED"); }
        public AlreadyReviewedException(Throwable cause) { super("ALREADY_REVIEWED", cause); }
    }

    public record FeaturedReview(int reviewId, int rating, String comment, String userName, String avatarUrl,
            LocalDateTime createdAt) {}
    public record ProductReviewSummary(int productId, Double averageRating, long reviewCount) {}
    public record PublicReview(int reviewId, int productId, int rating, String comment, String userName,
            LocalDateTime createdAt) {}
    public record ProductReviewAggregate(Double averageRating, long reviewCount, long rating1, long rating2,
            long rating3, long rating4, long rating5) {}

    private final Supplier<EntityManager> entityManagers;

    public ReviewDAO() { this(DatabaseUtil::getEntityManager); }
    ReviewDAO(Supplier<EntityManager> entityManagers) { this.entityManagers = entityManagers; }

    public Review findByUserOrder(int userId, int orderId) {
        List<Review> reviews = findAllByUserOrder(userId, orderId);
        return reviews.isEmpty() ? null : reviews.get(0);
    }

    public List<Review> findAllByUserOrder(int userId, int orderId) {
        EntityManager em = entityManagers.get();
        try {
            return em.createQuery(
                    "SELECT r FROM Review r JOIN FETCH r.product WHERE r.user.userId = :uid AND r.order.orderId = :oid ORDER BY r.product.productId",
                    Review.class)
                    .setParameter("uid", userId)
                    .setParameter("oid", orderId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public Map<Integer, ProductReviewSummary> summariesByProductIds(List<Integer> productIds) {
        if (productIds.isEmpty()) return Map.of();
        EntityManager em = entityManagers.get();
        try {
            List<ProductReviewSummary> summaries = em.createQuery(
                    "SELECT new dao.ReviewDAO$ProductReviewSummary(r.product.productId, AVG(r.rating), COUNT(r)) FROM Review r WHERE r.product.productId IN :productIds GROUP BY r.product.productId",
                    ProductReviewSummary.class)
                    .setParameter("productIds", productIds)
                    .getResultList();
            Map<Integer, ProductReviewSummary> result = new LinkedHashMap<>();
            summaries.forEach(summary -> result.put(summary.productId(), summary));
            return result;
        } finally { em.close(); }
    }

    public List<PublicReview> findPublicByProductId(int productId, int page, int size) {
        EntityManager em = entityManagers.get();
        try {
            return em.createQuery(
                    "SELECT new dao.ReviewDAO$PublicReview(r.reviewId, r.product.productId, r.rating, r.comment, u.fullName, r.createdAt) FROM Review r JOIN r.user u WHERE r.product.productId = :productId ORDER BY r.createdAt DESC, r.reviewId DESC",
                    PublicReview.class)
                    .setParameter("productId", productId)
                    .setFirstResult((page - 1) * size)
                    .setMaxResults(size)
                    .getResultList();
        } finally { em.close(); }
    }

    public ProductReviewAggregate aggregateByProductId(int productId) {
        EntityManager em = entityManagers.get();
        try {
            return em.createQuery(
                    "SELECT new dao.ReviewDAO$ProductReviewAggregate(AVG(r.rating), COUNT(r), COALESCE(SUM(CASE WHEN r.rating = 1 THEN 1 ELSE 0 END), 0), COALESCE(SUM(CASE WHEN r.rating = 2 THEN 1 ELSE 0 END), 0), COALESCE(SUM(CASE WHEN r.rating = 3 THEN 1 ELSE 0 END), 0), COALESCE(SUM(CASE WHEN r.rating = 4 THEN 1 ELSE 0 END), 0), COALESCE(SUM(CASE WHEN r.rating = 5 THEN 1 ELSE 0 END), 0)) FROM Review r WHERE r.product.productId = :productId",
                    ProductReviewAggregate.class)
                    .setParameter("productId", productId)
                    .getSingleResult();
        } finally { em.close(); }
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
        throw new IllegalStateException("PRODUCT_ID_REQUIRED");
    }

    public Review save(int userId, int orderId, int productId, int rating, String comment, boolean homepageConsent) {
        EntityManager em = entityManagers.get();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            Long membership = em.createQuery("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.order.orderId = :oid AND oi.product.productId = :pid", Long.class)
                    .setParameter("oid", orderId).setParameter("pid", productId).getSingleResult();
            requireEligible(order, userId, membership);
            Long existing = em.createQuery("SELECT COUNT(r) FROM Review r WHERE r.user.userId = :uid AND r.order.orderId = :oid AND r.product.productId = :pid", Long.class)
                    .setParameter("uid", userId).setParameter("oid", orderId).setParameter("pid", productId).getSingleResult();
            Review review = newReview(order, userId, productId, rating, comment, homepageConsent, existing);
            review.setUser(em.getReference(User.class, userId));
            review.setProduct(em.getReference(Product.class, productId));
            em.persist(review);
            em.flush();
            em.getTransaction().commit();
            return review;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            if (isDuplicateTriple(e)) throw new AlreadyReviewedException(e);
            throw e;
        } finally {
            em.close();
        }
    }

    static void requireEligible(Orders order, int userId, long membership) {
        if (order == null || order.getUser() == null || order.getUser().getUserId() != userId
                || !"ACTIVE".equals(order.getUser().getStatus()) || !"USER".equals(order.getUser().getRole())
                || !"DELIVERED".equals(order.getOrderStatus()) || membership < 1) {
            throw new IllegalArgumentException("Chỉ người dùng đang hoạt động được đánh giá sản phẩm đã mua trong đơn hàng đã giao của mình");
        }
    }

    static Review newReview(Orders order, int userId, int productId, int rating, String comment,
            boolean homepageConsent, long existing) {
        if (existing > 0) throw new AlreadyReviewedException();
        Review review = new Review();
        User user = new User();
        user.setUserId(userId);
        Product product = new Product();
        product.setProductId(productId);
        review.setUser(user);
        review.setOrder(order);
        review.setProduct(product);
        review.setRating(rating);
        review.setComment(comment);
        review.setHomepageConsent(homepageConsent);
        review.setCreatedAt(LocalDateTime.now());
        return review;
    }

    private static boolean isDuplicateTriple(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.contains("UQ_Review_UserOrderProduct")) return true;
        }
        return false;
    }
}
