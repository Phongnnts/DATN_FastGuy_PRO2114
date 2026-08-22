package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dao.ReviewDAO;
import entity.Orders;
import entity.Product;
import entity.Review;

class ReviewProductScopeServiceTest {
    @Test
    void orderResponseGroupsReviewsByProductWithCustomerAllowlist() {
        Review second = review(2, 22, 4, "Khá", LocalDateTime.of(2026, 8, 21, 11, 0));
        Review first = review(1, 11, 5, "Ngon", LocalDateTime.of(2026, 8, 21, 10, 0));
        ReviewService service = new ReviewService(new FakeReviewDAO(List.of(second, first), List.of(), null));

        Map<String, Object> result = service.getByOrderId(7, 44);
        List<Map<String, Object>> reviews = items(result, "reviews");

        assertEquals(Set.of("orderId", "reviews"), result.keySet());
        assertEquals(44, result.get("orderId"));
        assertEquals(List.of(11, 22), reviews.stream().map(item -> item.get("productId")).toList());
        assertEquals(Set.of("reviewId", "productId", "rating", "comment", "createdAt"), reviews.get(0).keySet());
    }

    @Test
    void createValidatesAndPassesProductScopeTrimmedCommentAndConsent() {
        FakeReviewDAO dao = new FakeReviewDAO(List.of(), List.of(), null);
        ReviewService service = new ReviewService(dao);

        service.create(7, 44, 11, 5, "  Ngon  ", false);

        assertEquals(List.of(7, 44, 11, 5, "Ngon", false), dao.saved);
        service.create(7, 44, 11, 4, "   ");
        assertEquals(Arrays.asList(7, 44, 11, 4, null, false), dao.saved);
        assertThrows(IllegalArgumentException.class, () -> service.create(7, 44, 11, 0, null, false));
        assertThrows(IllegalArgumentException.class, () -> service.create(7, 44, 11, 6, null, false));
        assertThrows(IllegalArgumentException.class, () -> service.create(7, 44, 11, 5, "x".repeat(1001), false));
    }

    @Test
    void productPageUsesExactPublicAllowlistRoundsHalfUpAndPreservesTotal() {
        LocalDateTime newer = LocalDateTime.of(2026, 8, 21, 12, 0);
        LocalDateTime older = LocalDateTime.of(2026, 8, 21, 10, 0);
        List<ReviewDAO.PublicReview> publicReviews = List.of(
                new ReviewDAO.PublicReview(1, 11, 4, "Cũ", "An", older),
                new ReviewDAO.PublicReview(2, 11, 5, "Mới", "Bình", newer));
        ReviewDAO.ProductReviewAggregate aggregate = new ReviewDAO.ProductReviewAggregate(
                4.15, 16L, 1L, 0L, 2L, 5L, 8L);
        ReviewService service = new ReviewService(new FakeReviewDAO(List.of(), publicReviews, aggregate));

        Map<String, Object> result = service.getByProductId(11, 1, 10);
        List<Map<String, Object>> items = items(result, "items");

        assertEquals(Set.of("items", "total", "page", "size", "averageRating", "reviewCount", "ratingDistribution"), result.keySet());
        assertEquals(List.of(2, 1), items.stream().map(item -> item.get("reviewId")).toList());
        assertEquals(Set.of("reviewId", "productId", "rating", "comment", "userName", "createdAt"), items.get(0).keySet());
        assertEquals(new BigDecimal("4.2"), result.get("averageRating"));
        assertEquals(16L, result.get("total"));
        assertEquals(16L, result.get("reviewCount"));
        assertEquals(Map.of("1", 1L, "2", 0L, "3", 2L, "4", 5L, "5", 8L), result.get("ratingDistribution"));
    }

    @Test
    void productPageHasExactZeroSemanticsAndValidatesBounds() {
        ReviewService service = new ReviewService(new FakeReviewDAO(List.of(), List.of(), null));

        Map<String, Object> result = service.getByProductId(11, 1, 10);

        assertEquals(new BigDecimal("0.0"), result.get("averageRating"));
        assertEquals(0L, result.get("total"));
        assertEquals(0L, result.get("reviewCount"));
        assertEquals(Map.of("1", 0L, "2", 0L, "3", 0L, "4", 0L, "5", 0L), result.get("ratingDistribution"));
        assertThrows(IllegalArgumentException.class, () -> service.getByProductId(11, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> service.getByProductId(11, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> service.getByProductId(11, 1, 51));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(Map<String, Object> map, String key) {
        return (List<Map<String, Object>>) map.get(key);
    }

    private static Review review(int reviewId, int productId, int rating, String comment, LocalDateTime createdAt) {
        Review review = new Review();
        review.setReviewId(reviewId);
        Product product = new Product();
        product.setProductId(productId);
        review.setProduct(product);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(createdAt);
        Orders order = new Orders();
        order.setOrderId(44);
        review.setOrder(order);
        return review;
    }

    private static class FakeReviewDAO extends ReviewDAO {
        private final List<Review> orderReviews;
        private final List<PublicReview> publicReviews;
        private final ProductReviewAggregate aggregate;
        private List<Object> saved;

        FakeReviewDAO(List<Review> orderReviews, List<PublicReview> publicReviews, ProductReviewAggregate aggregate) {
            this.orderReviews = orderReviews;
            this.publicReviews = publicReviews;
            this.aggregate = aggregate;
        }

        @Override public List<Review> findAllByUserOrder(int userId, int orderId) { return orderReviews; }
        @Override public List<PublicReview> findPublicByProductId(int productId, int page, int size) { return publicReviews; }
        @Override public ProductReviewAggregate aggregateByProductId(int productId) { return aggregate; }
        @Override public Review save(int userId, int orderId, int productId, int rating, String comment, boolean homepageConsent) {
            saved = Arrays.asList(userId, orderId, productId, rating, comment, homepageConsent);
            return review(9, productId, rating, comment, LocalDateTime.of(2026, 8, 21, 12, 0));
        }
    }
}
