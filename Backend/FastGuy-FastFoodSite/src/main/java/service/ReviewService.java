package service;

import dao.ReviewDAO;
import entity.Review;

import java.util.HashMap;
import java.util.Map;

public class ReviewService {
    private final ReviewDAO reviewDAO;

    public ReviewService() { this(new ReviewDAO()); }
    ReviewService(ReviewDAO reviewDAO) { this.reviewDAO = reviewDAO; }

    public Map<String, Object> getByOrderId(int userId, int orderId) {
        Review review = reviewDAO.findByUserOrder(userId, orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("reviewed", review != null);
        result.put("review", review == null ? null : toMap(review));
        return result;
    }

    public Map<String, Object> create(int userId, int orderId, int rating, String comment, boolean homepageConsent) {

        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Số sao phải là số nguyên từ 1 đến 5");
        if (comment != null) {
            comment = comment.trim();
            if (comment.length() > 1000) throw new IllegalArgumentException("Bình luận không được vượt quá 1000 ký tự");
            if (comment.isEmpty()) comment = null;
        }
        return toMap(reviewDAO.save(userId, orderId, rating, comment, homepageConsent));
    }

    public Map<String, Object> setFeaturedByOrderId(int orderId, boolean featured) {
        return toAdminMap(reviewDAO.setFeaturedByOrderId(orderId, featured));
    }

    static void requireFeaturedEligibility(Review review) {
        if (review == null) throw new IllegalArgumentException("Review not found");
        if (!Boolean.TRUE.equals(review.getHomepageConsent()) || review.getComment() == null || review.getComment().isBlank() || review.getUser() == null

                || !"ACTIVE".equals(review.getUser().getStatus()) || review.getUser().getFullName() == null
                || review.getUser().getFullName().isBlank() || review.getCreatedAt() == null) {
            throw new IllegalStateException("Review is not eligible for homepage");
        }
    }

    public Map<String, Object> getAdminByOrderId(int orderId) {
        Review review = reviewDAO.findByOrderId(orderId);
        return review == null ? null : toAdminMap(review);
    }

    private Map<String, Object> toAdminMap(Review review) {
        Map<String, Object> data = toMap(review);
        String reason = featureIneligibilityReason(review);
        data.put("featureEligible", reason == null);
        data.put("featureIneligibilityReason", reason);
        return data;
    }

    private String featureIneligibilityReason(Review review) {
        if (!Boolean.TRUE.equals(review.getHomepageConsent())) return "MISSING_HOMEPAGE_CONSENT";
        if (review.getComment() == null || review.getComment().isBlank()) return "MISSING_COMMENT";
        if (review.getUser() == null || !"ACTIVE".equals(review.getUser().getStatus())) return "INACTIVE_USER";
        if (review.getUser().getFullName() == null || review.getUser().getFullName().isBlank()) return "MISSING_USER_NAME";
        if (review.getCreatedAt() == null) return "MISSING_CREATED_AT";
        return null;
    }

    private Map<String, Object> toMap(Review review) {
        Map<String, Object> data = new HashMap<>();
        data.put("reviewId", review.getReviewId());
        data.put("rating", review.getRating());
        data.put("comment", review.getComment());
        data.put("createdAt", review.getCreatedAt());
        data.put("updatedAt", review.getUpdatedAt());
        data.put("userName", review.getUser() != null ? review.getUser().getFullName() : "Khách hàng");
        data.put("avatarUrl", review.getUser() != null ? review.getUser().getAvatarUrl() : "");
        data.put("orderId", review.getOrder() != null ? review.getOrder().getOrderId() : null);
        data.put("featured", Boolean.TRUE.equals(review.getFeatured()));
        data.put("homepageConsent", Boolean.TRUE.equals(review.getHomepageConsent()));

        return data;
    }
}
