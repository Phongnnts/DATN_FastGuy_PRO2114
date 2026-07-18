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
            String categoryParam = req.getParameter("categoryId");
            List<Map<String, Object>> products;
            if (categoryParam != null) {
                int categoryId = Integer.parseInt(categoryParam);
                products = productDAO.findByCategoryId(categoryId).stream().map(this::toMap).collect(Collectors.toList());
            } else {
                products = productDAO.findAllAvailable().stream().map(this::toMap).collect(Collectors.toList());
            }
            ApiResponse.ok(resp, products);
            return;
        }

        if ("/featured".equals(path)) {
            ApiResponse.ok(resp, productDAO.findAllAvailable().stream().limit(4).map(this::toMap).collect(Collectors.toList()));
            return;
        }

        if ("/new".equals(path)) {
            List<Map<String, Object>> products = productDAO.findAllAvailable().stream()
                    .sorted((a, b) -> b.getProductId() - a.getProductId())
                    .limit(8).map(this::toMap).collect(Collectors.toList());
            ApiResponse.ok(resp, products);
            return;
        }
        if ("/promotions".equals(path)) {
            List<Map<String, Object>> products = productDAO.findAllAvailable().stream()
                    .limit(8).map(this::toMap).collect(Collectors.toList());
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

    private Map<String, Object> toMap(Product p) {
        Map<String, Object> m = new HashMap<>();
        List<ProductVariant> variants = productDAO.findVariantsByProductId(p.getProductId());
        ProductVariant defaultVariant = variants.stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsDefault()))
                .findFirst()
                .orElse(variants.isEmpty() ? null : variants.get(0));
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
        m.put("discountPrice", null);
        m.put("availableFrom", p.getAvailableFrom() != null ? p.getAvailableFrom().toString() : null);
        m.put("availableTo", p.getAvailableTo() != null ? p.getAvailableTo().toString() : null);
        m.put("isAvailableNow", isAvailableNow(p));
        m.put("inStock", "AVAILABLE".equals(p.getStatus()) && hasStock && isAvailableNow(p));
        m.put("featured", false);
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
