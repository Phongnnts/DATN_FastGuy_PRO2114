package service;

import dao.ReviewDAO;
import entity.Review;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReviewService {

    private final ReviewDAO reviewDAO;

    public ReviewService() {
        this(new ReviewDAO());
    }

    ReviewService(ReviewDAO reviewDAO) {
        this.reviewDAO = reviewDAO;
    }

    public Map<String, Object> getByOrderId(int userId, int orderId) {
        List<Map<String, Object>> reviews = reviewDAO
            .findAllByUserOrder(userId, orderId)
            .stream()
            .sorted(
                Comparator.comparingInt(review ->
                    review.getProduct().getProductId()
                )
            )
            .map(this::toCustomerMap)
            .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", orderId);
        result.put("reviews", reviews);
        return result;
    }

    public Map<String, Object> getByProductId(
        int productId,
        int page,
        int size
    ) {
        if (
            page < 1 || size < 1 || size > 50
        ) throw new IllegalArgumentException("Trang đánh giá không hợp lệ");
        ReviewDAO.ProductReviewAggregate aggregate =
            reviewDAO.aggregateByProductId(productId);
        long count = aggregate == null ? 0 : aggregate.reviewCount();
        List<Map<String, Object>> items = reviewDAO
            .findPublicByProductId(productId, page, size)
            .stream()
            .sorted(
                Comparator.comparing(ReviewDAO.PublicReview::createdAt)
                    .reversed()
                    .thenComparing(
                        ReviewDAO.PublicReview::reviewId,
                        Comparator.reverseOrder()
                    )
            )
            .map(this::toPublicMap)
            .toList();
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("1", aggregate == null ? 0L : aggregate.rating1());
        distribution.put("2", aggregate == null ? 0L : aggregate.rating2());
        distribution.put("3", aggregate == null ? 0L : aggregate.rating3());
        distribution.put("4", aggregate == null ? 0L : aggregate.rating4());
        distribution.put("5", aggregate == null ? 0L : aggregate.rating5());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", count);
        result.put("page", page);
        result.put("size", size);
        result.put(
            "averageRating",
            aggregate == null || aggregate.averageRating() == null
                ? new BigDecimal("0.0")
                : BigDecimal.valueOf(aggregate.averageRating()).setScale(
                      1,
                      RoundingMode.HALF_UP
                  )
        );
        result.put("reviewCount", count);
        result.put("ratingDistribution", distribution);
        return result;
    }

    public Map<String, Object> create(
        int userId,
        int orderId,
        int productId,
        int rating,
        String comment
    ) {
        return create(userId, orderId, productId, rating, comment, false);
    }

    public Map<String, Object> create(
        int userId,
        int orderId,
        int productId,
        int rating,
        String comment,
        boolean homepageConsent
    ) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException(
            "Số sao phải là số nguyên từ 1 đến 5"
        );
        if (comment != null) {
            comment = comment.trim();
            if (comment.length() > 1000) throw new IllegalArgumentException(
                "Bình luận không được vượt quá 1000 ký tự"
            );
            if (comment.isEmpty()) comment = null;
        }
        return toCustomerMap(
            reviewDAO.save(
                userId,
                orderId,
                productId,
                rating,
                comment,
                homepageConsent
            )
        );
    }

    public Map<String, Object> create(
        int userId,
        int orderId,
        int rating,
        String comment,
        boolean homepageConsent
    ) {
        throw new IllegalStateException("PRODUCT_ID_REQUIRED");
    }

    public Map<String, Object> setFeaturedByOrderId(
        int orderId,
        boolean featured
    ) {
        return toAdminMap(reviewDAO.setFeaturedByOrderId(orderId, featured));
    }

    static void requireFeaturedEligibility(Review review) {
        if (review == null) throw new IllegalArgumentException(
            "Review not found"
        );
        if (
            !Boolean.TRUE.equals(review.getHomepageConsent()) ||
            review.getComment() == null ||
            review.getComment().isBlank() ||
            review.getUser() == null ||
            !"ACTIVE".equals(review.getUser().getStatus()) ||
            review.getUser().getFullName() == null ||
            review.getUser().getFullName().isBlank() ||
            review.getCreatedAt() == null
        ) {
            throw new IllegalStateException(
                "Review is not eligible for homepage"
            );
        }
    }

    public Map<String, Object> getAdminByOrderId(int orderId) {
        Review review = reviewDAO.findByOrderId(orderId);
        return review == null ? null : toAdminMap(review);
    }

    private Map<String, Object> toCustomerMap(Review review) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reviewId", review.getReviewId());
        data.put("productId", review.getProduct().getProductId());
        data.put("rating", review.getRating());
        data.put("comment", review.getComment());
        data.put("createdAt", review.getCreatedAt());
        return data;
    }

    private Map<String, Object> toPublicMap(ReviewDAO.PublicReview review) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reviewId", review.reviewId());
        data.put("productId", review.productId());
        data.put("rating", review.rating());
        data.put("comment", review.comment());
        data.put("userName", review.userName());
        data.put("createdAt", review.createdAt());
        return data;
    }

    private Map<String, Object> toAdminMap(Review review) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reviewId", review.getReviewId());
        data.put(
            "productId",
            review.getProduct() == null
                ? null
                : review.getProduct().getProductId()
        );
        data.put("rating", review.getRating());
        data.put("comment", review.getComment());
        data.put("createdAt", review.getCreatedAt());
        data.put("updatedAt", review.getUpdatedAt());
        data.put(
            "userName",
            review.getUser() != null
                ? review.getUser().getFullName()
                : "Khách hàng"
        );
        data.put(
            "avatarUrl",
            review.getUser() != null ? review.getUser().getAvatarUrl() : ""
        );
        data.put(
            "orderId",
            review.getOrder() != null ? review.getOrder().getOrderId() : null
        );
        data.put("featured", Boolean.TRUE.equals(review.getFeatured()));
        data.put(
            "homepageConsent",
            Boolean.TRUE.equals(review.getHomepageConsent())
        );
        String reason = featureIneligibilityReason(review);
        data.put("featureEligible", reason == null);
        data.put("featureIneligibilityReason", reason);
        return data;
    }

    private String featureIneligibilityReason(Review review) {
        if (
            !Boolean.TRUE.equals(review.getHomepageConsent())
        ) return "MISSING_HOMEPAGE_CONSENT";
        if (
            review.getComment() == null || review.getComment().isBlank()
        ) return "MISSING_COMMENT";
        if (
            review.getUser() == null ||
            !"ACTIVE".equals(review.getUser().getStatus())
        ) return "INACTIVE_USER";
        if (
            review.getUser().getFullName() == null ||
            review.getUser().getFullName().isBlank()
        ) return "MISSING_USER_NAME";
        if (review.getCreatedAt() == null) return "MISSING_CREATED_AT";
        return null;
    }
}
