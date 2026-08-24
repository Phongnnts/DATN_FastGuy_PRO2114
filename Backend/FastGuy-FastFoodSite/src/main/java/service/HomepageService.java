package service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dao.ProductDAO;
import dao.ProductModifierDAO;
import dao.ReviewDAO;
import entity.Product;
import entity.ProductModifierGroup;
import entity.ProductModifierOption;
import entity.ProductVariant;

public class HomepageService {
    private final ProductDAO productDAO;
    private final ProductModifierDAO modifierDAO;
    private final ReviewDAO reviewDAO;
    private final InventoryAvailabilityService inventoryAvailability;

    public HomepageService() { this(new ProductDAO(), new ProductModifierDAO(), new ReviewDAO()); }

    HomepageService(ProductDAO productDAO, ProductModifierDAO modifierDAO, ReviewDAO reviewDAO) {
        this(productDAO, modifierDAO, reviewDAO, new InventoryAvailabilityService());
    }

    HomepageService(ProductDAO productDAO, ProductModifierDAO modifierDAO, ReviewDAO reviewDAO, InventoryAvailabilityService inventoryAvailability) {
        this.productDAO = productDAO;
        this.modifierDAO = modifierDAO;
        this.reviewDAO = reviewDAO;
        this.inventoryAvailability = inventoryAvailability;
    }

    public Map<String, Object> getHomepage() {
        List<Product> bestSellers = productDAO.findHomepageBestSellers(6);
        List<Integer> ids = bestSellers.stream().map(Product::getProductId).toList();
        Map<Integer, List<ProductVariant>> variants = productDAO.variantsByProductIds(ids);
        Map<Integer, Long> sold = productDAO.soldCounts(ids);
        Map<Integer, List<ProductModifierGroup>> groups = modifierDAO.groupsByProductIds(ids);
        List<Integer> groupIds = groups.values().stream().flatMap(List::stream).map(ProductModifierGroup::getModifierGroupId).toList();
        Map<Integer, List<ProductModifierOption>> options = modifierDAO.optionsByGroupIds(groupIds);
        Map<Integer, ReviewDAO.ProductReviewSummary> reviewSummaries = reviewDAO.summariesByProductIds(ids);
        List<Integer> variantIds = variants.values().stream().flatMap(List::stream).map(ProductVariant::getVariantId).toList();
        Map<Integer, Map<String, Object>> availability = inventoryAvailability.publicAvailability(variantIds);

        List<Map<String, Object>> bestSellerMaps = bestSellers.stream()
                .map(p -> productMap(p, true, sold, variants, groups, options, reviewSummaries, availability)).toList();
        List<Map<String, Object>> reviews = reviewDAO.findFeatured(3).stream().map(this::reviewMap).toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bestSellers", bestSellerMaps);
        data.put("featuredReviews", reviews);
        return data;
    }

    private Map<String, Object> productMap(Product product, boolean bestSeller, Map<Integer, Long> sold,
            Map<Integer, List<ProductVariant>> variantsByProduct, Map<Integer, List<ProductModifierGroup>> groupsByProduct,
            Map<Integer, List<ProductModifierOption>> optionsByGroup,
            Map<Integer, ReviewDAO.ProductReviewSummary> reviewSummaries,
            Map<Integer, Map<String, Object>> availability) {
        int id = product.getProductId();
        List<ProductVariant> variants = variantsByProduct.getOrDefault(id, List.of());
        ProductVariant defaultVariant = variants.stream().filter(this::selectable).filter(v -> Boolean.TRUE.equals(v.getIsDefault())).findFirst()
                .orElseGet(() -> variants.stream().filter(this::selectable).findFirst().orElse(null));
        List<ProductModifierGroup> groups = groupsByProduct.getOrDefault(id, List.of());
        boolean hasVariants = variants.stream().anyMatch(v -> !Boolean.TRUE.equals(v.getIsDefault()));
        boolean hasModifiers = !groups.isEmpty();
        BigDecimal price = defaultVariant == null ? product.getBasePrice() : defaultVariant.getPrice();
        BigDecimal originalPrice = defaultVariant == null ? null : defaultVariant.getOriginalPrice();
        boolean discounted = originalPrice != null && price != null && originalPrice.signum() > 0 && originalPrice.compareTo(price) > 0;
        boolean inStock = variants.stream().map(v -> availability.get(v.getVariantId())).filter(java.util.Objects::nonNull)
                .map(a -> a.get("availabilityStatus")).anyMatch(List.of("IN_STOCK", "LOW_STOCK", "UNTRACKED")::contains);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("productId", id); map.put("name", product.getName());
        map.put("description", product.getDescription() == null ? "" : product.getDescription());
        map.put("basePrice", product.getBasePrice()); map.put("price", price);
        map.put("defaultVariant", defaultVariant == null ? null : variantMap(defaultVariant, availability));
        map.put("imageUrl", product.getImageUrl() == null ? "" : product.getImageUrl());
        map.put("categoryId", product.getCategory().getCategoryId()); map.put("categoryName", product.getCategory().getName());
        map.put("originalPrice", originalPrice);
        map.put("discountPercent", discounted ? originalPrice.subtract(price).multiply(BigDecimal.valueOf(100)).divide(originalPrice, 2, RoundingMode.HALF_UP) : null);
        map.put("soldCount", sold.getOrDefault(id, 0L)); map.put("hasVariants", hasVariants);
        map.put("hasModifiers", hasModifiers);
        map.put("productType", hasModifiers ? "CUSTOMIZABLE" : hasVariants ? "VARIANT" : "SIMPLE");
        map.put("availableFrom", product.getAvailableFrom() == null ? null : product.getAvailableFrom().toString());
        map.put("availableTo", product.getAvailableTo() == null ? null : product.getAvailableTo().toString());
        map.put("isAvailableNow", isAvailableNow(product));
        map.put("inStock", "AVAILABLE".equals(product.getStatus()) && inStock && isAvailableNow(product));
        map.put("isNew", Boolean.TRUE.equals(product.getIsNew())); map.put("spiceLevel", product.getSpiceLevel());
        map.put("bestSeller", bestSeller);
        ReviewDAO.ProductReviewSummary reviewSummary = reviewSummaries.get(id);
        map.put("averageRating", BigDecimal.valueOf(reviewSummary == null || reviewSummary.averageRating() == null ? 0 : reviewSummary.averageRating()).setScale(1, RoundingMode.HALF_UP));
        map.put("reviewCount", reviewSummary == null ? 0L : reviewSummary.reviewCount());
        map.put("variants", variants.stream().map(v -> variantMap(v, availability)).toList());
        map.put("modifierGroups", groups.stream().map(g -> groupMap(g, optionsByGroup)).toList());
        return map;
    }

    private Map<String, Object> variantMap(ProductVariant variant, Map<Integer, Map<String, Object>> availability) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("variantId", variant.getVariantId()); map.put("variantName", variant.getVariantName());
        map.put("price", variant.getPrice()); map.put("originalPrice", variant.getOriginalPrice()); map.put("sku", variant.getSku());
        map.put("quantityAvailable", variant.getQuantityAvailable()); map.put("isDefault", Boolean.TRUE.equals(variant.getIsDefault()));
        map.put("status", variant.getStatus()); map.putAll(availability.get(variant.getVariantId())); return map;
    }

    private boolean selectable(ProductVariant variant) {
        return "AVAILABLE".equals(variant.getStatus()) && (variant.getQuantityAvailable() == null || variant.getQuantityAvailable() > 0);
    }

    private Map<String, Object> groupMap(ProductModifierGroup group, Map<Integer, List<ProductModifierOption>> optionsByGroup) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("modifierGroupId", group.getModifierGroupId()); map.put("name", group.getName());
        map.put("minSelections", group.getMinSelections()); map.put("maxSelections", group.getMaxSelections());
        map.put("isActive", Boolean.TRUE.equals(group.getIsActive()));
        map.put("options", optionsByGroup.getOrDefault(group.getModifierGroupId(), List.of()).stream().map(option -> {
            Map<String, Object> item = new LinkedHashMap<>(); item.put("modifierOptionId", option.getModifierOptionId());
            item.put("name", option.getName()); item.put("price", option.getPrice()); item.put("isActive", Boolean.TRUE.equals(option.getIsActive())); return item;
        }).toList()); return map;
    }

    private Map<String, Object> reviewMap(ReviewDAO.FeaturedReview review) {
        Map<String, Object> map = new LinkedHashMap<>(); map.put("reviewId", review.reviewId()); map.put("rating", review.rating());
        map.put("comment", review.comment()); map.put("userName", review.userName());
        map.put("avatarUrl", review.avatarUrl()); map.put("createdAt", review.createdAt().toString()); return map;
    }

    private boolean isAvailableNow(Product product) {
        LocalTime now = LocalTime.now(); LocalTime from = product.getAvailableFrom(); LocalTime to = product.getAvailableTo();
        if (from == null) return to == null || now.isBefore(to);
        if (to == null) return !now.isBefore(from);
        return from.isBefore(to) ? !now.isBefore(from) && now.isBefore(to) : !now.isBefore(from) || now.isBefore(to);
    }

}
