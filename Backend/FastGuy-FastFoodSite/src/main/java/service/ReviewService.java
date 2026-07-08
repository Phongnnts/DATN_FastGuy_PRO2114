package service;

import dao.ReviewDAO;
import entity.Review;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReviewService {
    private ReviewDAO reviewDAO = new ReviewDAO();

    public Map<String, Object> getByOrderId(int orderId) {
        List<Review> list = reviewDAO.findByOrderId(orderId);
        return list.isEmpty() ? null : toMap(list.get(0));
    }

    public Map<String, Object> create(int userId, int orderId, int rating, String comment) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Số sao phải từ 1 đến 5");
        if (!reviewDAO.isOrderDelivered(userId, orderId)) {
            throw new IllegalArgumentException("Chỉ được đánh giá đơn hàng đã giao thành công");
        }
        if (comment != null) {
            comment = comment.trim();
            if (comment.length() > 1000) comment = comment.substring(0, 1000);
        }
        return toMap(reviewDAO.save(userId, orderId, rating, comment));
    }

    private Map<String, Object> toMap(Review review) {
        Map<String, Object> data = new HashMap<>();
        data.put("reviewId", review.getReviewId());
        data.put("rating", review.getRating());
        data.put("comment", review.getComment());
        data.put("createdAt", review.getCreatedAt());
        data.put("userName", review.getUser() != null ? review.getUser().getFullName() : "Khách hàng");
        data.put("avatarUrl", review.getUser() != null ? review.getUser().getAvatarUrl() : "");
        data.put("orderId", review.getOrder() != null ? review.getOrder().getOrderId() : null);
        return data;
    }
}
