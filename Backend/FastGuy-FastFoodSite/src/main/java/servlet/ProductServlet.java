package servlet;

import dao.ProductDAO;
import dao.ProductModifierDAO;
import entity.Product;
import entity.ProductCombo;
import entity.ProductComboItem;
import entity.ProductModifierGroup;
import entity.ProductModifierOption;
import entity.ProductVariant;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/products/*")
public class ProductServlet extends HttpServlet {
    private ProductDAO productDAO = new ProductDAO();
    private ProductModifierDAO modifierDAO = new ProductModifierDAO();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            try {
                String q = optional(req, "q");
                Integer categoryId = integer(req, "categoryId", 1, Integer.MAX_VALUE);
                BigDecimal minPrice = decimal(req, "minPrice");
                BigDecimal maxPrice = decimal(req, "maxPrice");
                if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) throw new IllegalArgumentException("minPrice must not exceed maxPrice");
                String availability = optional(req, "availability");
                availability = availability == null ? "ALL" : availability.toUpperCase();
                if (!List.of("ALL", "AVAILABLE", "OUT_OF_STOCK", "OUTSIDE_HOURS").contains(availability)) throw new IllegalArgumentException("Invalid availability");
                String productType = optional(req, "productType");
                productType = productType == null ? null : productType.toUpperCase();
                if (productType != null && !List.of("SIMPLE", "VARIANT", "COMBO", "CUSTOMIZABLE").contains(productType)) throw new IllegalArgumentException("Invalid productType");
                Boolean discounted = bool(req, "discounted");
                Long minSold = longInteger(req, "sold", 0, Long.MAX_VALUE);
                String sort = optional(req, "sort");
                sort = sort == null ? "default" : sort;
                if (!List.of("default", "name", "name-desc", "newest", "price-asc", "price-desc", "best-selling", "discount-desc").contains(sort)) throw new IllegalArgumentException("Invalid sort");
                boolean paged = req.getParameter("page") != null || req.getParameter("size") != null;
                Integer size = paged ? integer(req, "size", 1, 48) : null;
                Integer page = paged && size != null ? integer(req, "page", 0, Integer.MAX_VALUE / size) : null;
                if (paged && (page == null || size == null)) throw new IllegalArgumentException("page and size are both required");
                List<Map<String, Object>> products = toMaps(productDAO.search(q, categoryId, minPrice, maxPrice, availability, productType, discounted, minSold, sort, page, size));
                if (!paged) ApiResponse.ok(resp, products);
                else {
                    long total = productDAO.countSearch(q, categoryId, minPrice, maxPrice, availability, productType, discounted, minSold);
                    ApiResponse.ok(resp, Map.of("items", products, "page", page, "size", size, "totalItems", total, "totalPages", (total + size - 1) / size));
                }
            } catch (IllegalArgumentException e) {
                ApiResponse.error(resp, e.getMessage(), 400);
            }
            return;
        }

        if ("/best-sellers".equals(path)) {
            try {
                Integer limit = integer(req, "limit", 1, 20);
                ApiResponse.ok(resp, toMaps(productDAO.search(null, null, null, null, "AVAILABLE", null, null, null, "best-selling", 0, limit == null ? 10 : limit)));
            } catch (IllegalArgumentException e) {
                ApiResponse.error(resp, e.getMessage(), 400);
            }
            return;
        }

        if ("/featured".equals(path)) {
            ApiResponse.ok(resp, toMaps(productDAO.search(null, null, null, null, "AVAILABLE", null, null, null, "newest", 0, 4)));
            return;
        }

        if ("/new".equals(path)) {
            ApiResponse.ok(resp, toMaps(productDAO.search(null, null, null, null, "ALL", null, null, null, "newest", 0, 8)));
            return;
        }
        if ("/promotions".equals(path)) {
            List<Map<String, Object>> products = toMaps(productDAO.findAllAvailable().stream().limit(8).collect(Collectors.toList()));
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
                List<Map<String, Object>> related = productDAO.findByCategoryId(p.getCategory().getCategoryId())
                        .stream().filter(r -> r.getProductId() != productId).limit(4)
                        .map(this::toMap).collect(Collectors.toList());
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
        if (value.isBlank()) throw new IllegalArgumentException("Invalid " + name);
        return value.trim();
    }

    private Integer integer(HttpServletRequest req, String name, int min, int max) {
        String value = optional(req, name);
        if (value == null) return null;
        try {
            int result = Integer.parseInt(value);
            if (result < min || result > max) throw new IllegalArgumentException("Invalid " + name);
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + name);
        }
    }

    private Long longInteger(HttpServletRequest req, String name, long min, long max) {
        String value = optional(req, name);
        if (value == null) return null;
        try {
            long result = Long.parseLong(value);
            if (result < min || result > max) throw new IllegalArgumentException("Invalid " + name);
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
            if (result.signum() < 0) throw new IllegalArgumentException("Invalid " + name);
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + name);
        }
    }

    private Map<String, Object> toVariantMap(ProductVariant v) {
        Map<String, Object> m = new HashMap<>();
        m.put("variantId", v.getVariantId());
        m.put("variantName", v.getVariantName());
        m.put("price", v.getPrice());
        m.put("originalPrice", v.getOriginalPrice());
        m.put("sku", v.getSku());
        m.put("quantityAvailable", v.getQuantityAvailable());
        m.put("isDefault", v.getIsDefault() != null ? v.getIsDefault() : false);
        m.put("status", v.getStatus());
        return m;
    }

    private boolean isAvailableNow(Product p) {
        LocalTime now = LocalTime.now();
        LocalTime from = p.getAvailableFrom();
        LocalTime to = p.getAvailableTo();
        if (from == null) return to == null || now.isBefore(to);
        if (to == null) return !now.isBefore(from);
        return from.isBefore(to) ? !now.isBefore(from) && now.isBefore(to) : !now.isBefore(from) || now.isBefore(to);
    }

    private List<Map<String, Object>> toMaps(List<Product> products) {
        List<Integer> ids = products.stream().map(Product::getProductId).collect(Collectors.toList());
        Map<Integer, Long> sold = productDAO.soldCounts(ids);
        Map<Integer, Integer> flags = productDAO.featureFlags(ids);
        Map<Integer, ProductVariant> defaults = productDAO.defaultVariants(ids);
        return products.stream().map(p -> toMap(p, sold.getOrDefault(p.getProductId(), 0L), flags.getOrDefault(p.getProductId(), 0), defaults.get(p.getProductId()))).collect(Collectors.toList());
    }

    private Map<String, Object> toMap(Product p) {
        return toMap(p, productDAO.soldCounts(List.of(p.getProductId())).getOrDefault(p.getProductId(), 0L), productDAO.featureFlags(List.of(p.getProductId())).getOrDefault(p.getProductId(), 0), productDAO.findDefaultVariantByProductId(p.getProductId()));
    }

    private Map<String, Object> toMap(Product p, long soldCount, int flags, ProductVariant defaultVariant) {
        Map<String, Object> m = new HashMap<>();
        List<ProductVariant> variants = productDAO.findVariantsByProductId(p.getProductId());
        boolean hasStock = variants.stream().anyMatch(v -> "AVAILABLE".equals(v.getStatus())
                && (v.getQuantityAvailable() == null || v.getQuantityAvailable() > 0));
        m.put("productId", p.getProductId());
        m.put("name", p.getName());
        m.put("description", p.getDescription() != null ? p.getDescription() : "");
        m.put("basePrice", p.getBasePrice());
        m.put("price", defaultVariant != null ? defaultVariant.getPrice() : p.getBasePrice());
        m.put("defaultVariant", defaultVariant != null ? toVariantMap(defaultVariant) : null);
        m.put("imageUrl", p.getImageUrl() != null ? p.getImageUrl() : "");
        m.put("categoryId", p.getCategory().getCategoryId());
        m.put("categoryName", p.getCategory().getName());
        BigDecimal originalPrice = defaultVariant == null ? null : defaultVariant.getOriginalPrice();
        BigDecimal currentPrice = defaultVariant == null ? p.getBasePrice() : defaultVariant.getPrice();
        boolean discounted = originalPrice != null && currentPrice != null && originalPrice.signum() > 0 && originalPrice.compareTo(currentPrice) > 0;
        boolean hasVariants = (flags & 1) != 0;
        boolean hasModifiers = (flags & 2) != 0;
        boolean isCombo = (flags & 4) != 0;
        m.put("originalPrice", originalPrice);
        m.put("discountPercent", discounted ? originalPrice.subtract(currentPrice).multiply(BigDecimal.valueOf(100)).divide(originalPrice, 2, java.math.RoundingMode.HALF_UP) : null);
        m.put("soldCount", soldCount);
        m.put("hasVariants", hasVariants);
        m.put("hasModifiers", hasModifiers);
        m.put("isCombo", isCombo);
        m.put("productType", isCombo ? "COMBO" : hasModifiers ? "CUSTOMIZABLE" : hasVariants ? "VARIANT" : "SIMPLE");
        m.put("availableFrom", p.getAvailableFrom() != null ? p.getAvailableFrom().toString() : null);
        m.put("availableTo", p.getAvailableTo() != null ? p.getAvailableTo().toString() : null);
        m.put("isAvailableNow", isAvailableNow(p));
        m.put("inStock", "AVAILABLE".equals(p.getStatus()) && hasStock && isAvailableNow(p));
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

    private Map<String, Object> toGroupMap(ProductModifierGroup group) {
        Map<String, Object> m = new HashMap<>();
        m.put("modifierGroupId", group.getModifierGroupId());
        m.put("name", group.getName());
        m.put("minSelections", group.getMinSelections());
        m.put("maxSelections", group.getMaxSelections());
        m.put("isActive", Boolean.TRUE.equals(group.getIsActive()));
        m.put("options", modifierDAO.options(group.getModifierGroupId()).stream().filter(o -> Boolean.TRUE.equals(o.getIsActive())).map(this::toOptionMap).collect(Collectors.toList()));
        return m;
    }

    private Map<String, Object> toComboItemMap(ProductComboItem item) {
        Map<String, Object> m = new HashMap<>();
        m.put("comboItemId", item.getComboItemId());
        m.put("productId", item.getProduct().getProductId());
        m.put("productName", item.getProduct().getName());
        m.put("variantId", item.getVariant().getVariantId());
        m.put("variantName", item.getVariant().getVariantName());
        m.put("quantity", item.getQuantity());
        return m;
    }

    private Map<String, Object> toDetailMap(Product p) {
        Map<String, Object> m = toMap(p);
        List<Map<String, Object>> variants = productDAO.findVariantsByProductId(p.getProductId()).stream()
                .map(this::toVariantMap)
                .collect(Collectors.toList());
        m.put("variants", variants);

        String gallery = p.getGalleryImages();
        List<String> galleryList = new ArrayList<>();
        if (gallery != null && !gallery.isEmpty()) {
            try {
                galleryList = mapper.readValue(gallery, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                for (String url : gallery.split(",")) {
                    String trimmed = url.trim();
                    if (!trimmed.isEmpty()) galleryList.add(trimmed);
                }
            }
        }
        m.put("galleryImages", galleryList);
        m.put("modifierGroups", modifierDAO.groups(p.getProductId()).stream().filter(g -> Boolean.TRUE.equals(g.getIsActive())).map(this::toGroupMap).collect(Collectors.toList()));
        ProductCombo combo = modifierDAO.combo(p.getProductId());
        m.put("combo", combo != null && Boolean.TRUE.equals(combo.getIsActive()) ? Map.of("comboId", combo.getComboId(), "items", modifierDAO.comboItems(combo.getComboId()).stream().map(this::toComboItemMap).collect(Collectors.toList())) : null);
        return m;
    }
}
