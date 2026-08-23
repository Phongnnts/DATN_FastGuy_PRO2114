package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dao.ProductDAO;
import dao.ProductModifierDAO;
import dao.ReviewDAO;
import entity.Category;
import entity.Product;
import entity.ProductModifierGroup;
import entity.ProductVariant;

class ProductReviewSummarySerializerTest {
    @Test
    void threeProductListLoadsReviewSummariesOnceAndSerializesDefaults() {
        FakeReviewDAO reviews = new FakeReviewDAO(Map.of(1, new ReviewDAO.ProductReviewSummary(1, 4.2, 16L)));
        ProductServlet servlet = servlet(reviews);

        List<Map<String, Object>> maps = servlet.toMaps(List.of(product(1), product(2), product(3)));

        assertEquals(1, reviews.calls);
        assertEquals(List.of(1, 2, 3), reviews.requestedIds);
        assertEquals(4.2, maps.get(0).get("averageRating"));
        assertEquals(16L, maps.get(0).get("reviewCount"));
        assertEquals(0.0, maps.get(1).get("averageRating"));
        assertEquals(0L, maps.get(1).get("reviewCount"));
        maps.forEach(this::assertSummaryShape);
    }

    @Test
    void detailLoadsReviewSummaryOnceWithoutEmbeddedReviewData() {
        FakeReviewDAO reviews = new FakeReviewDAO(Map.of(1, new ReviewDAO.ProductReviewSummary(1, 4.2, 16L)));
        Map<String, Object> detail = servlet(reviews).toDetailMap(product(1));

        assertEquals(1, reviews.calls);
        assertEquals(List.of(1), reviews.requestedIds);
        assertSummaryShape(detail);
        assertTrue(detail.containsKey("galleryImages"));
    }

    @Test
    void relatedSetLoadsReviewSummariesOnceForAllSerializedProducts() {
        FakeReviewDAO reviews = new FakeReviewDAO(Map.of());
        List<Map<String, Object>> related = servlet(reviews).toMaps(List.of(product(2), product(3)));

        assertEquals(1, reviews.calls);
        assertEquals(List.of(2, 3), reviews.requestedIds);
        related.forEach(this::assertSummaryShape);
    }

    private ProductServlet servlet(FakeReviewDAO reviews) {
        return new ProductServlet(new FakeProductDAO(), new FakeProductModifierDAO(), reviews);
    }

    private void assertSummaryShape(Map<String, Object> map) {
        assertInstanceOf(Number.class, map.get("averageRating"));
        assertInstanceOf(Long.class, map.get("reviewCount"));
        assertTrue(map.containsKey("variants"));
        assertTrue(map.containsKey("modifierGroups"));
        assertFalse(map.containsKey("reviews"));
        assertFalse(map.containsKey("items"));
        assertFalse(map.containsKey("ratingDistribution"));
    }

    private Product product(int id) {
        Category category = new Category();
        category.setCategoryId(1);
        category.setName("Food");
        Product product = new Product();
        product.setProductId(id);
        product.setCategory(category);
        product.setName("Product " + id);
        product.setBasePrice(BigDecimal.TEN);
        product.setStatus("AVAILABLE");
        return product;
    }

    private static class FakeReviewDAO extends ReviewDAO {
        private final Map<Integer, ProductReviewSummary> summaries;
        private int calls;
        private List<Integer> requestedIds;

        private FakeReviewDAO(Map<Integer, ProductReviewSummary> summaries) { this.summaries = summaries; }

        @Override
        public Map<Integer, ProductReviewSummary> summariesByProductIds(List<Integer> productIds) {
            calls++;
            requestedIds = List.copyOf(productIds);
            return summaries;
        }
    }

    private static class FakeProductDAO extends ProductDAO {
        @Override public Map<Integer, Long> soldCounts(List<Integer> productIds) { return new HashMap<>(); }
        @Override public Map<Integer, Integer> featureFlags(List<Integer> productIds) { return new HashMap<>(); }
        @Override public Map<Integer, ProductVariant> defaultVariants(List<Integer> productIds) { return new HashMap<>(); }
        @Override public Map<Integer, List<ProductVariant>> variantsByProductIds(List<Integer> productIds) { return new HashMap<>(); }
        @Override public List<ProductVariant> findVariantsByProductId(int productId) { throw new AssertionError("detail must reuse batch variants"); }
    }

    private static class FakeProductModifierDAO extends ProductModifierDAO {
        @Override public Map<Integer, List<ProductModifierGroup>> groupsByProductIds(List<Integer> productIds) { return Map.of(); }
        @Override public Map<Integer, List<entity.ProductModifierOption>> optionsByGroupIds(List<Integer> groupIds) { return Map.of(); }
        @Override public List<ProductModifierGroup> groups(int productId) { throw new AssertionError("detail must reuse batch modifier groups"); }
    }
}
