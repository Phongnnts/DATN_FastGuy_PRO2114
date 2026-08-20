package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dao.ProductDAO;
import dao.ProductModifierDAO;
import dao.ReviewDAO;
import entity.Category;
import entity.Product;
import entity.ProductCombo;
import entity.ProductModifierGroup;
import entity.ProductModifierOption;
import entity.ProductVariant;

class HomepageServiceTest {
    private static final Set<String> PRODUCT_KEYS = Set.of("productId", "name", "description", "basePrice", "price",
            "defaultVariant", "imageUrl", "categoryId", "categoryName", "originalPrice", "discountPercent",
            "soldCount", "hasVariants", "hasModifiers", "isCombo", "productType", "availableFrom", "availableTo",
            "isAvailableNow", "inStock", "isNew", "spiceLevel", "bestSeller", "variants", "modifierGroups");

    @Test
    void homepageMapsExactDirectAddContractAndPolicies() {
        Product product = product(10, "Gà cay");
        ProductVariant variant = new ProductVariant();
        variant.setVariantId(100); variant.setProduct(product); variant.setVariantName("Mặc định");
        variant.setPrice(new BigDecimal("49000")); variant.setOriginalPrice(new BigDecimal("59000"));
        variant.setQuantityAvailable(5); variant.setIsDefault(true); variant.setStatus("AVAILABLE");
        ProductModifierOption option = new ProductModifierOption();
        option.setModifierOptionId(300); option.setName("Phô mai"); option.setPrice(new BigDecimal("5000")); option.setIsActive(true);
        ProductModifierGroup group = new ProductModifierGroup();
        group.setModifierGroupId(200); group.setProduct(product); group.setName("Topping");
        group.setMinSelections(0); group.setMaxSelections(1); group.setIsActive(true);
        ProductCombo first = combo(1, product, "QUICK_BREAK", 1);
        ProductCombo duplicate = combo(2, product, "QUICK_BREAK", 2);
        ReviewDAO.FeaturedReview review = new ReviewDAO.FeaturedReview(7, 5, "Ngon", "An", null,
                LocalDateTime.of(2026, 8, 18, 10, 0));

        HomepageService service = new HomepageService(
                new FakeProductDAO(List.of(product), Map.of(10, List.of(variant)), Map.of(10, 12L)),
                new FakeModifierDAO(List.of(first, duplicate), Map.of(10, List.of(group)), Map.of(200, List.of(option))),
                new FakeReviewDAO(List.of(review)));

        Map<String, Object> data = service.getHomepage();
        List<Map<String, Object>> bestSellers = castList(data.get("bestSellers"));
        Map<String, Object> summary = bestSellers.get(0);
        assertEquals(PRODUCT_KEYS, summary.keySet());
        assertEquals(true, summary.get("bestSeller"));
        assertEquals(true, summary.get("inStock"));
        assertEquals(true, summary.get("hasModifiers"));
        assertEquals("COMBO", summary.get("productType"));
        assertEquals(1, castList(summary.get("variants")).size());
        assertEquals(1, castList(summary.get("modifierGroups")).size());

        List<Map<String, Object>> combos = castList(data.get("occasionCombos"));
        assertEquals(1, combos.size());
        assertEquals("Bữa nhanh gọn", combos.get(0).get("label"));
        assertEquals(false, ((Map<?, ?>) combos.get(0).get("product")).get("bestSeller"));

        List<Map<String, Object>> reviews = castList(data.get("featuredReviews"));
        assertEquals(Set.of("reviewId", "rating", "comment", "userName", "avatarUrl", "createdAt"), reviews.get(0).keySet());
        assertNull(reviews.get(0).get("avatarUrl"));
        assertFalse(reviews.get(0).containsKey("orderId"));
    }

    @Test
    void summaryChoosesSelectableDefaultThenFirstSelectableVariant() {
        Product product = product(10, "Gà cay");
        ProductVariant unavailableDefault = variant(product, 100, true, "UNAVAILABLE", 5, "39000", "49000");
        ProductVariant soldOut = variant(product, 101, false, "AVAILABLE", 0, "41000", null);
        ProductVariant selectable = variant(product, 102, false, "AVAILABLE", 3, "43000", "53000");
        HomepageService service = new HomepageService(
                new FakeProductDAO(List.of(product), Map.of(10, List.of(unavailableDefault, soldOut, selectable)), Map.of()),
                new FakeModifierDAO(List.of(), Map.of(), Map.of()), new FakeReviewDAO(List.of()));

        Map<String, Object> summary = castList(service.getHomepage().get("bestSellers")).get(0);

        assertEquals(102, ((Map<?, ?>) summary.get("defaultVariant")).get("variantId"));
        assertEquals(new BigDecimal("43000"), summary.get("price"));
        assertEquals(new BigDecimal("53000"), summary.get("originalPrice"));
        assertEquals(true, summary.get("inStock"));
    }

    @Test
    void summaryHasNoDefaultAndIsOutOfStockWithoutSelectableVariant() {
        Product product = product(10, "Gà cay");
        ProductVariant soldOutDefault = variant(product, 100, true, "AVAILABLE", 0, "39000", null);
        HomepageService service = new HomepageService(
                new FakeProductDAO(List.of(product), Map.of(10, List.of(soldOutDefault)), Map.of()),
                new FakeModifierDAO(List.of(), Map.of(), Map.of()), new FakeReviewDAO(List.of()));

        Map<String, Object> summary = castList(service.getHomepage().get("bestSellers")).get(0);

        assertNull(summary.get("defaultVariant"));
        assertEquals(product.getBasePrice(), summary.get("price"));
        assertEquals(false, summary.get("inStock"));
    }

    @Test
    void activeNonHomepageComboStillSetsComboProductType() {
        Product product = product(10, "Combo ẩn khỏi homepage");
        ProductVariant selectable = variant(product, 100, true, "AVAILABLE", 3, "49000", null);
        HomepageService service = new HomepageService(
                new FakeProductDAO(List.of(product), Map.of(10, List.of(selectable)), Map.of()),
                new FakeModifierDAO(List.of(), Map.of(), Map.of(), Set.of(10)), new FakeReviewDAO(List.of()));

        Map<String, Object> summary = castList(service.getHomepage().get("bestSellers")).get(0);

        assertEquals(true, summary.get("isCombo"));
        assertEquals("COMBO", summary.get("productType"));
    }

    @Test
    void occasionLabelsCoverContractEnum() {
        assertEquals("Bữa nhanh gọn", HomepageService.occasionLabel("QUICK_BREAK"));
        assertEquals("Bữa trưa văn phòng", HomepageService.occasionLabel("OFFICE_LUNCH"));
        assertEquals("Combo sinh viên", HomepageService.occasionLabel("STUDENT"));
        assertEquals("Ăn vui theo nhóm", HomepageService.occasionLabel("GROUP"));
    }

    private Product product(int id, String name) {
        Category category = new Category(); category.setCategoryId(2); category.setName("Gà rán");
        Product product = new Product(); product.setProductId(id); product.setCategory(category); product.setName(name);
        product.setDescription("Giòn"); product.setBasePrice(new BigDecimal("49000")); product.setStatus("AVAILABLE");
        product.setIsNew(true); product.setSpiceLevel(2);
        return product;
    }

    private ProductCombo combo(int id, Product product, String occasion, int sort) {
        ProductCombo combo = new ProductCombo(); combo.setComboId(id); combo.setProduct(product); combo.setIsActive(true);
        combo.setHomepageOccasion(occasion); combo.setHomepageSortOrder(sort); return combo;
    }

    private ProductVariant variant(Product product, int id, boolean isDefault, String status, Integer quantity, String price, String originalPrice) {
        ProductVariant variant = new ProductVariant(); variant.setProduct(product); variant.setVariantId(id); variant.setVariantName("V" + id);
        variant.setIsDefault(isDefault); variant.setStatus(status); variant.setQuantityAvailable(quantity); variant.setPrice(new BigDecimal(price));
        variant.setOriginalPrice(originalPrice == null ? null : new BigDecimal(originalPrice)); return variant;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) { return (List<Map<String, Object>>) value; }

    private static class FakeProductDAO extends ProductDAO {
        private final List<Product> products; private final Map<Integer, List<ProductVariant>> variants; private final Map<Integer, Long> sold;
        FakeProductDAO(List<Product> products, Map<Integer, List<ProductVariant>> variants, Map<Integer, Long> sold) { this.products = products; this.variants = variants; this.sold = sold; }
        @Override public List<Product> findHomepageBestSellers(int limit) { assertEquals(6, limit); return products; }
        @Override public Map<Integer, List<ProductVariant>> variantsByProductIds(List<Integer> ids) { return variants; }
        @Override public Map<Integer, Long> soldCounts(List<Integer> ids) { return sold; }
    }

    private static class FakeModifierDAO extends ProductModifierDAO {
        private final List<ProductCombo> combos; private final Map<Integer, List<ProductModifierGroup>> groups; private final Map<Integer, List<ProductModifierOption>> options; private final Set<Integer> activeComboIds;
        FakeModifierDAO(List<ProductCombo> combos, Map<Integer, List<ProductModifierGroup>> groups, Map<Integer, List<ProductModifierOption>> options) { this(combos, groups, options, combos.stream().map(c -> c.getProduct().getProductId()).collect(java.util.stream.Collectors.toSet())); }
        FakeModifierDAO(List<ProductCombo> combos, Map<Integer, List<ProductModifierGroup>> groups, Map<Integer, List<ProductModifierOption>> options, Set<Integer> activeComboIds) { this.combos = combos; this.groups = groups; this.options = options; this.activeComboIds = activeComboIds; }
        @Override public List<ProductCombo> homepageCombos() { return combos; }
        @Override public Map<Integer, List<ProductModifierGroup>> groupsByProductIds(List<Integer> ids) { return groups; }
        @Override public Map<Integer, List<ProductModifierOption>> optionsByGroupIds(List<Integer> ids) { return options; }
        @Override public Set<Integer> activeComboProductIds(List<Integer> ids) { return activeComboIds; }
    }

    private static class FakeReviewDAO extends ReviewDAO {
        private final List<FeaturedReview> reviews;
        FakeReviewDAO(List<FeaturedReview> reviews) { this.reviews = reviews; }
        @Override public List<FeaturedReview> findFeatured(int limit) { assertEquals(3, limit); return reviews; }
    }
}
