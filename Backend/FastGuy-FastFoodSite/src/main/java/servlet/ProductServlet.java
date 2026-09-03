package servlet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.ProductDAO;
import dao.ProductModifierDAO;
import dao.ReviewDAO;
import entity.Product;
import entity.ProductModifierGroup;
import entity.ProductModifierOption;
import entity.ProductVariant;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import service.InventoryAvailabilityService;
import utils.ApiResponse;

@WebServlet("/api/products/*")
public class ProductServlet extends HttpServlet {

    private ProductDAO productDAO = new ProductDAO();
    private ProductModifierDAO modifierDAO = new ProductModifierDAO();
    private ReviewDAO reviewDAO = new ReviewDAO();
    private InventoryAvailabilityService inventoryAvailability =
        new InventoryAvailabilityService();
    private static final ObjectMapper mapper = new ObjectMapper();

    public ProductServlet() {}

    ProductServlet(
        ProductDAO productDAO,
        ProductModifierDAO modifierDAO,
        ReviewDAO reviewDAO
    ) {
        this(
            productDAO,
            modifierDAO,
            reviewDAO,
            new InventoryAvailabilityService()
        );
    }

    ProductServlet(
        ProductDAO productDAO,
        ProductModifierDAO modifierDAO,
        ReviewDAO reviewDAO,
        InventoryAvailabilityService inventoryAvailability
    ) {
        this.productDAO = productDAO;
        this.modifierDAO = modifierDAO;
        this.reviewDAO = reviewDAO;
        this.inventoryAvailability = inventoryAvailability;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            try {
                String q = optional(req, "q");
                Integer categoryId = integer(
                    req,
                    "categoryId",
                    1,
                    Integer.MAX_VALUE
                );
                BigDecimal minPrice = decimal(req, "minPrice");
                BigDecimal maxPrice = decimal(req, "maxPrice");
                if (
                    minPrice != null &&
                    maxPrice != null &&
                    minPrice.compareTo(maxPrice) > 0
                ) throw new IllegalArgumentException(
                    "minPrice must not exceed maxPrice"
                );
                String availability = optional(req, "availability");
                availability =
                    availability == null ? "ALL" : availability.toUpperCase();
                if (
                    !List.of(
                        "ALL",
                        "AVAILABLE",
                        "OUT_OF_STOCK",
                        "OUTSIDE_HOURS"
                    ).contains(availability)
                ) throw new IllegalArgumentException("Invalid availability");
                String productType = optional(req, "productType");
                productType =
                    productType == null ? null : productType.toUpperCase();
                if (
                    productType != null &&
                    !List.of("SIMPLE", "VARIANT", "CUSTOMIZABLE").contains(
                        productType
                    )
                ) throw new IllegalArgumentException("Invalid productType");
                Boolean discounted = bool(req, "discounted");
                Long minSold = longInteger(req, "sold", 0, Long.MAX_VALUE);
                String sort = optional(req, "sort");
                sort = sort == null ? "default" : sort;
                if (
                    !List.of(
                        "default",
                        "name",
                        "name-desc",
                        "newest",
                        "price-asc",
                        "price-desc",
                        "best-selling",
                        "discount-desc"
                    ).contains(sort)
                ) throw new IllegalArgumentException("Invalid sort");
                boolean paged =
                    req.getParameter("page") != null ||
                    req.getParameter("size") != null;
                Integer size = paged ? integer(req, "size", 1, 48) : null;
                Integer page =
                    paged && size != null
                        ? integer(req, "page", 0, Integer.MAX_VALUE / size)
                        : null;
                if (
                    paged && (page == null || size == null)
                ) throw new IllegalArgumentException(
                    "page and size are both required"
                );
                List<Map<String, Object>> products = toMaps(
                    productDAO.search(
                        q,
                        categoryId,
                        minPrice,
                        maxPrice,
                        availability,
                        productType,
                        discounted,
                        minSold,
                        sort,
                        page,
                        size
                    )
                );
                if (!paged) ApiResponse.ok(resp, products);
                else {
                    long total = productDAO.countSearch(
                        q,
                        categoryId,
                        minPrice,
                        maxPrice,
                        availability,
                        productType,
                        discounted,
                        minSold
                    );
                    ApiResponse.ok(
                        resp,
                        Map.of(
                            "items",
                            products,
                            "page",
                            page,
                            "size",
                            size,
                            "totalItems",
                            total,
                            "totalPages",
                            (total + size - 1) / size
                        )
                    );
                }
            } catch (IllegalArgumentException e) {
                ApiResponse.error(resp, e.getMessage(), 400);
            }
            return;
        }

        if ("/best-sellers".equals(path)) {
            try {
                Integer limit = integer(req, "limit", 1, 20);
                List<Map<String, Object>> products = toMaps(
                    productDAO.search(
                        null,
                        null,
                        null,
                        null,
                        "AVAILABLE",
                        null,
                        null,
                        null,
                        "best-selling",
                        0,
                        limit == null ? 10 : limit
                    )
                );
                setBestSeller(products, true);
                ApiResponse.ok(resp, products);
            } catch (IllegalArgumentException e) {
                ApiResponse.error(resp, e.getMessage(), 400);
            }
            return;
        }

        if ("/featured".equals(path)) {
            ApiResponse.ok(
                resp,
                toMaps(
                    productDAO.search(
                        null,
                        null,
                        null,
                        null,
                        "AVAILABLE",
                        null,
                        null,
                        null,
                        "newest",
                        0,
                        4
                    )
                )
            );
            return;
        }

        if ("/new".equals(path)) {
            ApiResponse.ok(
                resp,
                toMaps(
                    productDAO.search(
                        null,
                        null,
                        null,
                        null,
                        "ALL",
                        null,
                        null,
                        null,
                        "newest",
                        0,
                        8
                    )
                )
            );
            return;
        }
        if ("/promotions".equals(path)) {
            List<Map<String, Object>> products = toMaps(
                productDAO
                    .findAllAvailable()
                    .stream()
                    .limit(8)
                    .collect(Collectors.toList())
            );
            ApiResponse.ok(resp, products);
            return;
        }

        try {
            int productId = Integer.parseInt(path.substring(1));
            Product p = productDAO.findById(productId);
            if (p == null || !"AVAILABLE".equals(p.getStatus())) {
                ApiResponse.error(resp, "Product not found", 404);
                return;
            }

            // Related products (same category)
            if (req.getParameter("related") != null) {
                List<Product> relatedProducts = productDAO
                    .findByCategoryId(p.getCategory().getCategoryId())
                    .stream()
                    .filter(r -> r.getProductId() != productId)
                    .limit(4)
                    .collect(Collectors.toList());
                List<Map<String, Object>> related = toMaps(relatedProducts);
                ApiResponse.ok(resp, related);
                return;
            }

            ApiResponse.ok(resp, toDetailMap(p));
        } catch (NumberFormatException e) {
            resp.sendError(404);
        }
    }

    private String optional(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        if (value == null) return null;
        if (value.isBlank()) throw new IllegalArgumentException(
            "Invalid " + name
        );
        return value.trim();
    }

    private Integer integer(
        HttpServletRequest req,
        String name,
        int min,
        int max
    ) {
        String value = optional(req, name);
        if (value == null) return null;
        try {
            int result = Integer.parseInt(value);
            if (
                result < min || result > max
            ) throw new IllegalArgumentException("Invalid " + name);
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + name);
        }
    }

    private Long longInteger(
        HttpServletRequest req,
        String name,
        long min,
        long max
    ) {
        String value = optional(req, name);
        if (value == null) return null;
        try {
            long result = Long.parseLong(value);
            if (
                result < min || result > max
            ) throw new IllegalArgumentException("Invalid " + name);
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + name);
        }
    }

    private Boolean bool(HttpServletRequest req, String name) {
        String value = optional(req, name);
        if (value == null) return null;
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new IllegalArgumentException("Invalid " + name);
    }

    private BigDecimal decimal(HttpServletRequest req, String name) {
        String value = optional(req, name);
        if (value == null) return null;
        try {
            BigDecimal result = new BigDecimal(value);
            if (result.signum() < 0) throw new IllegalArgumentException(
                "Invalid " + name
            );
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + name);
        }
    }

    private Map<String, Object> toVariantMap(
        ProductVariant v,
        Map<Integer, Map<String, Object>> availability
    ) {
        Map<String, Object> m = new HashMap<>();
        m.put("variantId", v.getVariantId());
        m.put("variantName", v.getVariantName());
        m.put("price", v.getPrice());
        m.put("originalPrice", v.getOriginalPrice());
        m.put("sku", v.getSku());
        m.put("quantityAvailable", v.getQuantityAvailable());
        m.put("isDefault", v.getIsDefault() != null ? v.getIsDefault() : false);
        m.put("status", v.getStatus());
        m.putAll(availability.get(v.getVariantId()));
        return m;
    }

    private boolean isAvailableNow(Product p) {
        LocalTime now = LocalTime.now();
        LocalTime from = p.getAvailableFrom();
        LocalTime to = p.getAvailableTo();
        if (from == null) return to == null || now.isBefore(to);
        if (to == null) return !now.isBefore(from);
        return from.isBefore(to)
            ? !now.isBefore(from) && now.isBefore(to)
            : !now.isBefore(from) || now.isBefore(to);
    }

    List<Map<String, Object>> toMaps(List<Product> products) {
        List<Integer> ids = products
            .stream()
            .map(Product::getProductId)
            .collect(Collectors.toList());
        Map<Integer, Long> sold = productDAO.soldCounts(ids);
        Map<Integer, Integer> flags = productDAO.featureFlags(ids);
        Map<Integer, ProductVariant> defaults = productDAO.defaultVariants(ids);
        Map<Integer, List<ProductVariant>> variants =
            productDAO.variantsByProductIds(ids);
        Map<Integer, List<ProductModifierGroup>> groups =
            modifierDAO.groupsByProductIds(ids);
        List<Integer> groupIds = groups
            .values()
            .stream()
            .flatMap(List::stream)
            .map(ProductModifierGroup::getModifierGroupId)
            .collect(Collectors.toList());
        Map<Integer, List<ProductModifierOption>> options =
            modifierDAO.optionsByGroupIds(groupIds);
        Map<Integer, ReviewDAO.ProductReviewSummary> ratings =
            reviewDAO.summariesByProductIds(ids);
        List<Integer> variantIds = variants
            .values()
            .stream()
            .flatMap(List::stream)
            .map(ProductVariant::getVariantId)
            .toList();
        Map<Integer, Map<String, Object>> availability =
            inventoryAvailability.publicAvailability(variantIds);
        return products
            .stream()
            .map(p ->
                toMap(
                    p,
                    sold.getOrDefault(p.getProductId(), 0L),
                    flags.getOrDefault(p.getProductId(), 0),
                    defaults.get(p.getProductId()),
                    variants.getOrDefault(p.getProductId(), List.of()),
                    groups.getOrDefault(p.getProductId(), List.of()),
                    options,
                    ratings.get(p.getProductId()),
                    availability
                )
            )
            .collect(Collectors.toList());
    }

    static void setBestSeller(
        List<Map<String, Object>> products,
        boolean bestSeller
    ) {
        products.forEach(product -> product.put("bestSeller", bestSeller));
    }

    private Map<String, Object> toMap(Product p) {
        return toMaps(List.of(p)).get(0);
    }

    private Map<String, Object> toMap(
        Product p,
        long soldCount,
        int flags,
        ProductVariant defaultVariant,
        List<ProductVariant> variants,
        List<ProductModifierGroup> groups,
        Map<Integer, List<ProductModifierOption>> options,
        ReviewDAO.ProductReviewSummary rating,
        Map<Integer, Map<String, Object>> availability
    ) {
        Map<String, Object> m = new HashMap<>();
        boolean hasStock = variants
            .stream()
            .map(v -> availability.get(v.getVariantId()))
            .filter(java.util.Objects::nonNull)
            .map(a -> a.get("availabilityStatus"))
            .anyMatch(List.of("IN_STOCK", "LOW_STOCK", "UNTRACKED")::contains);
        m.put("productId", p.getProductId());
        m.put("name", p.getName());
        m.put(
            "description",
            p.getDescription() != null ? p.getDescription() : ""
        );
        m.put("basePrice", p.getBasePrice());
        m.put(
            "price",
            defaultVariant != null
                ? defaultVariant.getPrice()
                : p.getBasePrice()
        );
        m.put(
            "defaultVariant",
            defaultVariant != null
                ? toVariantMap(defaultVariant, availability)
                : null
        );
        m.put("imageUrl", p.getImageUrl() != null ? p.getImageUrl() : "");
        m.put("categoryId", p.getCategory().getCategoryId());
        m.put("categoryName", p.getCategory().getName());
        BigDecimal originalPrice =
            defaultVariant == null ? null : defaultVariant.getOriginalPrice();
        BigDecimal currentPrice =
            defaultVariant == null
                ? p.getBasePrice()
                : defaultVariant.getPrice();
        boolean discounted =
            originalPrice != null &&
            currentPrice != null &&
            originalPrice.signum() > 0 &&
            originalPrice.compareTo(currentPrice) > 0;
        boolean hasVariants = (flags & 1) != 0;
        boolean hasModifiers = (flags & 2) != 0;
        m.put("originalPrice", originalPrice);
        m.put(
            "discountPercent",
            discounted
                ? originalPrice
                      .subtract(currentPrice)
                      .multiply(BigDecimal.valueOf(100))
                      .divide(originalPrice, 2, java.math.RoundingMode.HALF_UP)
                : null
        );
        m.put("soldCount", soldCount);
        m.put(
            "averageRating",
            rating == null || rating.averageRating() == null
                ? 0.0
                : BigDecimal.valueOf(rating.averageRating())
                      .setScale(1, java.math.RoundingMode.HALF_UP)
                      .doubleValue()
        );
        m.put("reviewCount", rating == null ? 0L : rating.reviewCount());
        m.put("hasVariants", hasVariants);
        m.put("hasModifiers", hasModifiers);
        m.put(
            "productType",
            hasModifiers ? "CUSTOMIZABLE" : hasVariants ? "VARIANT" : "SIMPLE"
        );
        m.put(
            "availableFrom",
            p.getAvailableFrom() != null
                ? p.getAvailableFrom().toString()
                : null
        );
        m.put(
            "availableTo",
            p.getAvailableTo() != null ? p.getAvailableTo().toString() : null
        );
        m.put("isAvailableNow", isAvailableNow(p));
        m.put(
            "inStock",
            "AVAILABLE".equals(p.getStatus()) && hasStock && isAvailableNow(p)
        );
        m.put("isNew", Boolean.TRUE.equals(p.getIsNew()));
        m.put("spiceLevel", p.getSpiceLevel());
        m.put("bestSeller", false);
        m.put(
            "variants",
            variants
                .stream()
                .map(v -> toVariantMap(v, availability))
                .collect(Collectors.toList())
        );
        m.put(
            "modifierGroups",
            groups
                .stream()
                .map(group ->
                    toGroupMap(
                        group,
                        options.getOrDefault(
                            group.getModifierGroupId(),
                            List.of()
                        )
                    )
                )
                .collect(Collectors.toList())
        );
        return m;
    }

    private Map<String, Object> toOptionMap(ProductModifierOption option) {
        Map<String, Object> m = new HashMap<>();
        m.put("modifierOptionId", option.getModifierOptionId());
        m.put("name", option.getName());
        m.put("price", option.getPrice());
        m.put("isActive", Boolean.TRUE.equals(option.getIsActive()));
        return m;
    }

    private Map<String, Object> toGroupMap(
        ProductModifierGroup group,
        List<ProductModifierOption> options
    ) {
        Map<String, Object> m = new HashMap<>();
        m.put("modifierGroupId", group.getModifierGroupId());
        m.put("name", group.getName());
        m.put("minSelections", group.getMinSelections());
        m.put("maxSelections", group.getMaxSelections());
        m.put("isActive", Boolean.TRUE.equals(group.getIsActive()));
        m.put(
            "options",
            options.stream().map(this::toOptionMap).collect(Collectors.toList())
        );
        return m;
    }

    Map<String, Object> toDetailMap(Product p) {
        Map<String, Object> m = toMap(p);
        String gallery = p.getGalleryImages();
        List<String> galleryList = new ArrayList<>();
        if (gallery != null && !gallery.isEmpty()) {
            try {
                galleryList = mapper.readValue(
                    gallery,
                    new TypeReference<List<String>>() {}
                );
            } catch (Exception e) {
                for (String url : gallery.split(",")) {
                    String trimmed = url.trim();
                    if (!trimmed.isEmpty()) galleryList.add(trimmed);
                }
            }
        }
        m.put("galleryImages", galleryList);
        return m;
    }
}
