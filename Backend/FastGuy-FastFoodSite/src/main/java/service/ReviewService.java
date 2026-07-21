package service;

import dao.ReviewDAO;
import entity.Review;

import java.util.HashMap;
import java.util.Map;

public class ReviewService {
    private ReviewDAO reviewDAO = new ReviewDAO();

    public Map<String, Object> getByOrderId(int userId, int orderId) {
        Review review = reviewDAO.findByUserOrder(userId, orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("reviewed", review != null);
        result.put("review", review == null ? null : toMap(review));
        return result;
    }

    public Map<String, Object> create(int userId, int orderId, int rating, String comment) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Số sao phải là số nguyên từ 1 đến 5");
        if (comment != null) {
            comment = comment.trim();
            if (comment.length() > 1000) throw new IllegalArgumentException("Bình luận không được vượt quá 1000 ký tự");
            if (comment.isEmpty()) comment = null;
        }
        return toMap(reviewDAO.save(userId, orderId, rating, comment));
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
        return data;
    }
}
