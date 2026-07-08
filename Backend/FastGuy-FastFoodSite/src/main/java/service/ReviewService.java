package service;

import dao.ReviewDAO;
import entity.Review;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReviewService {
    private ReviewDAO reviewDAO = new ReviewDAO();

    public List<Map<String, Object>> getByProductId(int productId) {
        return reviewDAO.findByProductId(productId).stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getByOrderId(int orderId) {
        return reviewDAO.findByOrderId(orderId).stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getStats(int productId) {
        Map<String, Object> stats = new HashMap<>();
        long count = reviewDAO.countByProductId(productId);
        double average = reviewDAO.averageByProductId(productId);
        BigDecimal rounded = BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP);
        stats.put("rating", rounded.doubleValue());
        stats.put("reviewCount", count);
        return stats;
    }

    public Map<String, Object> create(int userId, int orderId, int productId, int rating, String comment) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Số sao phải từ 1 đến 5");
        if (!reviewDAO.hasUserBoughtProduct(userId, orderId, productId)) {
            throw new IllegalArgumentException("Bạn chỉ có thể đánh giá món đã giao thành công");
        }
        if (comment != null) {
            comment = comment.trim();
            if (comment.length() > 1000) comment = comment.substring(0, 1000);
        }
        return toMap(reviewDAO.save(userId, orderId, productId, rating, comment));
    }

    private Map<String, Object> toMap(Review review) {
        Map<String, Object> data = new HashMap<>();
        data.put("reviewId", review.getReviewId());
        data.put("rating", review.getRating());
        data.put("comment", review.getComment());
        data.put("createdAt", review.getCreatedAt());
        data.put("userName", review.getUser() != null ? review.getUser().getFullName() : "Khách hàng");
        data.put("avatarUrl", review.getUser() != null ? review.getUser().getAvatarUrl() : "");
        data.put("productId", review.getProduct() != null ? review.getProduct().getProductId() : null);
        data.put("orderId", review.getOrder() != null ? review.getOrder().getOrderId() : null);
        return data;
    }
}
