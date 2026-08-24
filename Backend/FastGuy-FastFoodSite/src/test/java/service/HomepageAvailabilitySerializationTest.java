package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
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

class HomepageAvailabilitySerializationTest {
    @Test
    void homepageAggregatesSafeAvailabilityAndOmitsPrivateInventory() {
        Product product = product();
        ProductVariant variant = variant();
        InventoryAvailabilityService availability = new InventoryAvailabilityService() {
            @Override public Map<Integer, Map<String, Object>> publicAvailability(List<Integer> ids) {
                return Map.of(11, Map.of("availabilityStatus", "UNTRACKED"));
            }
        };
        HomepageService service = new HomepageService(new Products(product, variant), new Modifiers(), new Reviews(), availability);

        Map<String, Object> item = (Map<String, Object>) ((List<?>) service.getHomepage().get("bestSellers")).get(0);
        Map<String, Object> serializedVariant = (Map<String, Object>) ((List<?>) item.get("variants")).get(0);

        assertEquals(true, item.get("inStock"));
        assertEquals("UNTRACKED", serializedVariant.get("availabilityStatus"));
        for (String field : List.of("inventoryItemId", "onHandQuantity", "reservedQuantity", "limitingItemId")) assertFalse(serializedVariant.containsKey(field));
    }

    private Product product() {
        Category category = new Category(); category.setCategoryId(1); category.setName("Food");
        Product product = new Product(); product.setProductId(1); product.setCategory(category); product.setName("Product");
        product.setBasePrice(BigDecimal.TEN); product.setStatus("AVAILABLE"); return product;
    }

    private ProductVariant variant() {
        ProductVariant variant = new ProductVariant(); variant.setVariantId(11); variant.setVariantName("Size"); variant.setPrice(BigDecimal.TEN);
        variant.setQuantityAvailable(0); variant.setStatus("AVAILABLE"); variant.setIsDefault(true); return variant;
    }

    private static class Products extends ProductDAO {
        private final Product product; private final ProductVariant variant;
        Products(Product product, ProductVariant variant) { this.product = product; this.variant = variant; }
        @Override public List<Product> findHomepageBestSellers(int limit) { return List.of(product); }
        @Override public Map<Integer, List<ProductVariant>> variantsByProductIds(List<Integer> ids) { return Map.of(1, List.of(variant)); }
        @Override public Map<Integer, Long> soldCounts(List<Integer> ids) { return Map.of(); }
    }
    private static class Modifiers extends ProductModifierDAO {
        @Override public Map<Integer, List<ProductModifierGroup>> groupsByProductIds(List<Integer> ids) { return Map.of(); }
        @Override public Map<Integer, List<entity.ProductModifierOption>> optionsByGroupIds(List<Integer> ids) { return Map.of(); }
    }
    private static class Reviews extends ReviewDAO {
        @Override public Map<Integer, ProductReviewSummary> summariesByProductIds(List<Integer> ids) { return Map.of(); }
        @Override public List<FeaturedReview> findFeatured(int limit) { return List.of(); }
    }
}
