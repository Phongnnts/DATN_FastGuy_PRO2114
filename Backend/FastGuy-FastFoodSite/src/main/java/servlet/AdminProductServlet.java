package servlet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.CategoryDAO;
import dao.ProductDAO;
import dao.ProductModifierDAO;
import entity.Category;
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
import utils.JwtUtil;
import utils.JsonUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/admin/products/*")
public class AdminProductServlet extends HttpServlet {
    private static final ObjectMapper mapper = new ObjectMapper();
    private ProductDAO productDAO = new ProductDAO();
    private ProductModifierDAO modifierDAO = new ProductModifierDAO();
    private CategoryDAO categoryDAO = new CategoryDAO();

    private boolean checkAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return false;
        }
        String role = JwtUtil.getRole(authHeader.substring(7));
        if (!"ADMIN".equals(role)) {
            ApiResponse.error(resp, "Forbidden", 403);
            return false;
        }
        return true;
    }

    private List<String> parseGalleryImages(Product p) {
        String raw = p.getGalleryImages();
        if (raw == null || raw.isEmpty()) return List.of();
        try {
            return mapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toGalleryJson(List<String> images) {
        if (images == null || images.isEmpty()) return "[]";
        try {
            return mapper.writeValueAsString(images);
        } catch (Exception e) {
            return "[]";
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

    private Map<String, Object> toMap(Product p) {
        Map<String, Object> m = new HashMap<>();
        m.put("productId", p.getProductId());
        m.put("name", p.getName());
        m.put("categoryId", p.getCategory().getCategoryId());
        m.put("categoryName", p.getCategory().getName());
        m.put("basePrice", p.getBasePrice());
        m.put("imageUrl", p.getImageUrl() != null ? p.getImageUrl() : "");
        m.put("description", p.getDescription() != null ? p.getDescription() : "");
        m.put("status", p.getStatus());
        m.put("availableFrom", p.getAvailableFrom() != null ? p.getAvailableFrom().toString() : null);
        m.put("availableTo", p.getAvailableTo() != null ? p.getAvailableTo().toString() : null);
        m.put("galleryImages", parseGalleryImages(p));
        m.put("variants", productDAO.findVariantsByProductId(p.getProductId()).stream()
                .map(this::toVariantMap).collect(Collectors.toList()));
        m.put("modifierGroups", modifierDAO.groups(p.getProductId()).stream().map(this::modifierGroupMap).collect(Collectors.toList()));
        m.put("combo", comboMap(modifierDAO.combo(p.getProductId())));
        m.put("discountPrice", null);
        m.put("rating", 0);
        m.put("reviewCount", 0);
        m.put("inStock", "AVAILABLE".equals(p.getStatus()));
        m.put("featured", false);
        return m;
    }

    private Map<String, Object> modifierGroupMap(ProductModifierGroup group) {
        Map<String, Object> m = new HashMap<>();
        m.put("modifierGroupId", group.getModifierGroupId()); m.put("name", group.getName()); m.put("minSelections", group.getMinSelections()); m.put("maxSelections", group.getMaxSelections()); m.put("isActive", Boolean.TRUE.equals(group.getIsActive()));
        m.put("options", modifierDAO.options(group.getModifierGroupId()).stream().map(option -> { Map<String, Object> o = new HashMap<>(); o.put("modifierOptionId", option.getModifierOptionId()); o.put("name", option.getName()); o.put("price", option.getPrice()); o.put("isActive", Boolean.TRUE.equals(option.getIsActive())); return o; }).collect(Collectors.toList()));
        return m;
    }

    private Map<String, Object> comboMap(ProductCombo combo) {
        if (combo == null) return null;
        Map<String, Object> m = new HashMap<>(); m.put("comboId", combo.getComboId()); m.put("isActive", Boolean.TRUE.equals(combo.getIsActive()));
        m.put("items", modifierDAO.comboItems(combo.getComboId()).stream().map(item -> Map.of("comboItemId", item.getComboItemId(), "productId", item.getProduct().getProductId(), "variantId", item.getVariant().getVariantId(), "quantity", item.getQuantity())).collect(Collectors.toList()));
        return m;
    }

    private String[] splitPath(String path) {
        if (path == null || path.equals("/")) return new String[0];
        return Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    private Integer parseId(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }

    private BigDecimal readMoney(Map<String, Object> body, String key, BigDecimal fallback) {
        if (!body.containsKey(key) || body.get(key) == null) return fallback;
        if (!(body.get(key) instanceof Number)) throw new IllegalArgumentException(key + " must be a number");
        BigDecimal value = BigDecimal.valueOf(((Number) body.get(key)).doubleValue());
        if (value.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException(key + " must be >= 0");
        return value;
    }

    private Integer readStock(Map<String, Object> body, String key, Integer fallback) {
        if (!body.containsKey(key) || body.get(key) == null) return fallback;
        if (!(body.get(key) instanceof Number)) throw new IllegalArgumentException(key + " must be a number");
        int value = ((Number) body.get(key)).intValue();
        if (value < 0) throw new IllegalArgumentException("Tồn kho không được âm");
        return value;
    }

    private String readStatus(Map<String, Object> body, String key, String fallback) {
        String status = body.containsKey(key) ? (String) body.get(key) : fallback;
        if (status == null) return fallback;
        if (!"AVAILABLE".equals(status) && !"UNAVAILABLE".equals(status)) throw new IllegalArgumentException("Trạng thái không hợp lệ");
        return status;
    }

    private LocalTime readTime(Map<String, Object> body, String key, LocalTime fallback) {
        if (!body.containsKey(key) || body.get(key) == null || ((String) body.get(key)).isBlank()) return body.containsKey(key) ? null : fallback;
        try { return LocalTime.parse((String) body.get(key)); } catch (Exception e) { throw new IllegalArgumentException(key + " không hợp lệ"); }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String[] segs = splitPath(req.getPathInfo());
        if (segs.length == 0) {
            List<Map<String, Object>> list = productDAO.findAll().stream()
                    .map(this::toMap).collect(Collectors.toList());
            ApiResponse.ok(resp, list);
            return;
        }

        if (segs.length == 1) {
            Integer id = parseId(segs[0]);
            if (id == null) { resp.sendError(404); return; }
            Product p = productDAO.findById(id);
            if (p == null) { ApiResponse.error(resp, "Not found", 404); return; }
            ApiResponse.ok(resp, toMap(p));
            return;
        }

        if (segs.length == 2 && "modifier-groups".equals(segs[1])) {
            Integer pid = parseId(segs[0]);
            Product p = pid == null ? null : productDAO.findById(pid);
            if (p == null) { ApiResponse.error(resp, "Product not found", 404); return; }
            ApiResponse.ok(resp, modifierDAO.groups(pid).stream().map(this::modifierGroupMap).collect(Collectors.toList()));
            return;
        }

        if (segs.length == 2 && "combo".equals(segs[1])) {
            Integer pid = parseId(segs[0]);
            Product p = pid == null ? null : productDAO.findById(pid);
            if (p == null) { ApiResponse.error(resp, "Product not found", 404); return; }
            ApiResponse.ok(resp, comboMap(modifierDAO.combo(pid)));
            return;
        }

        if (segs.length == 2 && "variants".equals(segs[1])) {
            Integer pid = parseId(segs[0]);
            if (pid == null) { resp.sendError(404); return; }
            Product p = productDAO.findById(pid);
            if (p == null) { ApiResponse.error(resp, "Product not found", 404); return; }
            List<Map<String, Object>> variants = productDAO.findVariantsByProductId(pid).stream()
                    .map(this::toVariantMap).collect(Collectors.toList());
            ApiResponse.ok(resp, variants);
            return;
        }

        if (segs.length == 2 && "variants".equals(segs[0])) {
            Integer vid = parseId(segs[1]);
            if (vid == null) { resp.sendError(404); return; }
            ProductVariant v = productDAO.findVariantById(vid);
            if (v == null) { ApiResponse.error(resp, "Not found", 404); return; }
            ApiResponse.ok(resp, toVariantMap(v));
            return;
        }

        resp.sendError(404);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String[] segs = splitPath(req.getPathInfo());
        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }

        if (segs.length == 0) {
            Integer categoryId = ((Number) body.get("categoryId")).intValue();
            Category category = categoryDAO.findById(categoryId);
            if (category == null) { ApiResponse.error(resp, "Category not found", 400); return; }

            Product p = new Product();
            p.setCategory(category);
            p.setName((String) body.get("name"));
            p.setDescription((String) body.get("description"));
            try {
                p.setBasePrice(readMoney(body, "basePrice", BigDecimal.ZERO));
                p.setStatus(readStatus(body, "status", "AVAILABLE"));
                p.setAvailableFrom(readTime(body, "availableFrom", null));
                p.setAvailableTo(readTime(body, "availableTo", null));
            } catch (IllegalArgumentException e) {
                ApiResponse.error(resp, e.getMessage(), 400);
                return;
            }
            p.setImageUrl((String) body.get("imageUrl"));
            if (body.containsKey("galleryImages")) {
                p.setGalleryImages(toGalleryJson((List<String>) body.get("galleryImages")));
            }
            p.setCreatedAt(LocalDateTime.now());
            productDAO.save(p);

            resp.setStatus(201);
            ApiResponse.ok(resp, toMap(p), "Created");
            return;
        }

        if (segs.length == 2 && "modifier-groups".equals(segs[1])) {
            Integer productId = parseId(segs[0]);
            Product product = productId == null ? null : productDAO.findById(productId);
            String name = (String) body.get("name");
            if (product == null || name == null || name.isBlank()) { ApiResponse.error(resp, "Invalid modifier group", 400); return; }
            int min = body.get("minSelections") instanceof Number ? ((Number) body.get("minSelections")).intValue() : 0;
            int max = body.get("maxSelections") instanceof Number ? ((Number) body.get("maxSelections")).intValue() : 1;
            if (min < 0 || max < min) { ApiResponse.error(resp, "Invalid selection limits", 400); return; }
            ProductModifierGroup group = new ProductModifierGroup(); group.setProduct(product); group.setName(name.trim()); group.setMinSelections(min); group.setMaxSelections(max); group.setIsActive(true); modifierDAO.save(group);
            resp.setStatus(201); ApiResponse.ok(resp, modifierGroupMap(group), "Created"); return;
        }

        if (segs.length == 3 && "modifier-groups".equals(segs[1]) && "options".equals(segs[2])) {
            Integer groupId = parseId(segs[0]); ProductModifierGroup group = groupId == null ? null : modifierDAO.group(groupId);
            String name = (String) body.get("name");
            if (group == null || name == null || name.isBlank()) { ApiResponse.error(resp, "Invalid modifier option", 400); return; }
            ProductModifierOption option = new ProductModifierOption(); option.setGroup(group); option.setName(name.trim());
            try { option.setPrice(readMoney(body, "price", BigDecimal.ZERO)); } catch (IllegalArgumentException e) { ApiResponse.error(resp, e.getMessage(), 400); return; }
            option.setIsActive(true); modifierDAO.save(option); resp.setStatus(201); ApiResponse.ok(resp, option, "Created"); return;
        }

        if (segs.length == 2 && "combo".equals(segs[1])) {
            Integer productId = parseId(segs[0]); Product product = productId == null ? null : productDAO.findById(productId);
            if (product == null) { ApiResponse.error(resp, "Product not found", 404); return; }
            ProductCombo combo = modifierDAO.combo(productId); if (combo == null) { combo = new ProductCombo(); combo.setProduct(product); } combo.setIsActive(!body.containsKey("isActive") || Boolean.TRUE.equals(body.get("isActive"))); modifierDAO.save(combo); ApiResponse.ok(resp, comboMap(combo), "Saved"); return;
        }

        if (segs.length == 3 && "combo".equals(segs[1]) && "items".equals(segs[2])) {
            Integer productId = parseId(segs[0]); ProductCombo combo = productId == null ? null : modifierDAO.combo(productId);
            Integer variantId = body.get("variantId") instanceof Number ? ((Number) body.get("variantId")).intValue() : null; ProductVariant variant = variantId == null ? null : productDAO.findVariantById(variantId);
            int qty = body.get("quantity") instanceof Number ? ((Number) body.get("quantity")).intValue() : 0;
            if (combo == null || variant == null || qty <= 0) { ApiResponse.error(resp, "Invalid combo item", 400); return; }
            ProductComboItem item = new ProductComboItem(); item.setCombo(combo); item.setProduct(variant.getProduct()); item.setVariant(variant); item.setQuantity(qty); modifierDAO.save(item); resp.setStatus(201); ApiResponse.ok(resp, comboMap(combo), "Created"); return;
        }

        if (segs.length == 2 && "variants".equals(segs[1])) {
            Integer productId = parseId(segs[0]);
            if (productId == null) { resp.sendError(404); return; }
            Product p = productDAO.findById(productId);
            if (p == null) { ApiResponse.error(resp, "Product not found", 404); return; }

            ProductVariant v = new ProductVariant();
            v.setProduct(p);
            String variantName = (String) body.get("variantName");
            if (variantName == null || variantName.trim().isEmpty()) { ApiResponse.error(resp, "Tên biến thể không được trống", 400); return; }
            v.setVariantName(variantName.trim());
            try {
                v.setPrice(readMoney(body, "price", BigDecimal.ZERO));
                v.setOriginalPrice(readMoney(body, "originalPrice", null));
                v.setQuantityAvailable(readStock(body, "quantityAvailable", null));
                v.setStatus(readStatus(body, "status", "AVAILABLE"));
            } catch (IllegalArgumentException e) {
                ApiResponse.error(resp, e.getMessage(), 400);
                return;
            }
            v.setSku((String) body.get("sku"));
            v.setIsDefault(body.containsKey("isDefault") ? (Boolean) body.get("isDefault") : false);
            v.setCreatedAt(LocalDateTime.now());
            productDAO.saveVariant(v);

            resp.setStatus(201);
            ApiResponse.ok(resp, toVariantMap(v), "Created");
            return;
        }

        resp.sendError(404);
    }

    private Integer readSelection(Map<String, Object> body, String key, Integer fallback) {
        if (!body.containsKey(key)) return fallback;
        if (!(body.get(key) instanceof Number)) throw new IllegalArgumentException(key + " must be a number");
        return ((Number) body.get(key)).intValue();
    }

    private Boolean readActive(Map<String, Object> body, Boolean fallback) {
        if (!body.containsKey("isActive")) return fallback;
        if (!(body.get("isActive") instanceof Boolean)) throw new IllegalArgumentException("isActive must be a boolean");
        return (Boolean) body.get("isActive");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String[] segs = splitPath(req.getPathInfo());
        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }

        if (segs.length == 2 && "modifier-groups".equals(segs[1])) {
            Integer groupId = parseId(segs[0]);
            ProductModifierGroup group = groupId == null ? null : modifierDAO.group(groupId);
            if (group == null) { ApiResponse.error(resp, "Modifier group not found", 404); return; }
            try {
                if (body.containsKey("name") && (!(body.get("name") instanceof String) || ((String) body.get("name")).isBlank())) throw new IllegalArgumentException("Invalid modifier group name");
                if (body.containsKey("name")) group.setName(((String) body.get("name")).trim());
                int min = readSelection(body, "minSelections", group.getMinSelections());
                int max = readSelection(body, "maxSelections", group.getMaxSelections());
                if (min < 0 || max < min) throw new IllegalArgumentException("Invalid selection limits");
                group.setMinSelections(min); group.setMaxSelections(max); group.setIsActive(readActive(body, group.getIsActive()));
                modifierDAO.save(group);
                ApiResponse.ok(resp, modifierGroupMap(group), "Updated");
            } catch (IllegalArgumentException e) { ApiResponse.error(resp, e.getMessage(), 400); }
            return;
        }

        if (segs.length == 4 && "modifier-groups".equals(segs[1]) && "options".equals(segs[2])) {
            Integer groupId = parseId(segs[0]); Integer optionId = parseId(segs[3]);
            ProductModifierOption option = optionId == null ? null : modifierDAO.option(optionId);
            if (groupId == null || option == null || option.getGroup().getModifierGroupId() != groupId) { ApiResponse.error(resp, "Modifier option not found", 404); return; }
            try {
                if (body.containsKey("name") && (!(body.get("name") instanceof String) || ((String) body.get("name")).isBlank())) throw new IllegalArgumentException("Invalid modifier option name");
                if (body.containsKey("name")) option.setName(((String) body.get("name")).trim());
                option.setPrice(readMoney(body, "price", option.getPrice())); option.setIsActive(readActive(body, option.getIsActive()));
                modifierDAO.save(option);
                ApiResponse.ok(resp, option, "Updated");
            } catch (IllegalArgumentException e) { ApiResponse.error(resp, e.getMessage(), 400); }
            return;
        }

        if (segs.length == 2 && "combo".equals(segs[1])) {
            Integer productId = parseId(segs[0]); ProductCombo combo = productId == null ? null : modifierDAO.combo(productId);
            if (combo == null) { ApiResponse.error(resp, "Combo not found", 404); return; }
            try { combo.setIsActive(readActive(body, combo.getIsActive())); modifierDAO.save(combo); ApiResponse.ok(resp, comboMap(combo), "Updated"); }
            catch (IllegalArgumentException e) { ApiResponse.error(resp, e.getMessage(), 400); }
            return;
        }

        if (segs.length == 1) {
            Integer id = parseId(segs[0]);
            if (id == null) { resp.sendError(404); return; }
            Product p = productDAO.findById(id);
            if (p == null) { ApiResponse.error(resp, "Not found", 404); return; }

            if (body.containsKey("categoryId")) {
                Category c = categoryDAO.findById(((Number) body.get("categoryId")).intValue());
                if (c != null) p.setCategory(c);
            }
            if (body.containsKey("name")) p.setName((String) body.get("name"));
            if (body.containsKey("description")) p.setDescription((String) body.get("description"));
            try {
                if (body.containsKey("basePrice")) p.setBasePrice(readMoney(body, "basePrice", p.getBasePrice()));
                if (body.containsKey("status")) p.setStatus(readStatus(body, "status", p.getStatus()));
                if (body.containsKey("availableFrom")) p.setAvailableFrom(readTime(body, "availableFrom", p.getAvailableFrom()));
                if (body.containsKey("availableTo")) p.setAvailableTo(readTime(body, "availableTo", p.getAvailableTo()));
            } catch (IllegalArgumentException e) {
                ApiResponse.error(resp, e.getMessage(), 400);
                return;
            }
            if (body.containsKey("imageUrl")) p.setImageUrl((String) body.get("imageUrl"));
            if (body.containsKey("galleryImages")) {
                p.setGalleryImages(toGalleryJson((List<String>) body.get("galleryImages")));
            }
            p.setUpdatedAt(LocalDateTime.now());
            productDAO.save(p);

            ApiResponse.ok(resp, toMap(p), "Updated");
            return;
        }

        if (segs.length == 2 && "variants".equals(segs[0])) {
            Integer vid = parseId(segs[1]);
            if (vid == null) { resp.sendError(404); return; }
            ProductVariant v = productDAO.findVariantById(vid);
            if (v == null) { ApiResponse.error(resp, "Not found", 404); return; }

            if (body.containsKey("variantName")) {
                String variantName = (String) body.get("variantName");
                if (variantName == null || variantName.trim().isEmpty()) { ApiResponse.error(resp, "Tên biến thể không được trống", 400); return; }
                v.setVariantName(variantName.trim());
            }
            try {
                if (body.containsKey("price")) v.setPrice(readMoney(body, "price", v.getPrice()));
                if (body.containsKey("originalPrice")) v.setOriginalPrice(readMoney(body, "originalPrice", v.getOriginalPrice()));
                if (body.containsKey("quantityAvailable")) v.setQuantityAvailable(readStock(body, "quantityAvailable", v.getQuantityAvailable()));
                if (body.containsKey("status")) v.setStatus(readStatus(body, "status", v.getStatus()));
            } catch (IllegalArgumentException e) {
                ApiResponse.error(resp, e.getMessage(), 400);
                return;
            }
            if (body.containsKey("sku")) v.setSku((String) body.get("sku"));
            if (body.containsKey("isDefault")) v.setIsDefault((Boolean) body.get("isDefault"));
            v.setUpdatedAt(LocalDateTime.now());
            productDAO.saveVariant(v);

            ApiResponse.ok(resp, toVariantMap(v), "Updated");
            return;
        }

        resp.sendError(404);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String[] segs = splitPath(req.getPathInfo());

        if (segs.length == 2 && "modifier-groups".equals(segs[0])) {
            Integer groupId = parseId(segs[1]);
            if (groupId == null || modifierDAO.group(groupId) == null) { ApiResponse.error(resp, "Modifier group not found", 404); return; }
            modifierDAO.deleteGroup(groupId);
            ApiResponse.ok(resp, null, "Deleted");
            return;
        }

        if (segs.length == 4 && "modifier-groups".equals(segs[1]) && "options".equals(segs[2])) {
            Integer groupId = parseId(segs[0]); Integer optionId = parseId(segs[3]);
            ProductModifierOption option = optionId == null ? null : modifierDAO.option(optionId);
            if (groupId == null || option == null || option.getGroup().getModifierGroupId() != groupId) { ApiResponse.error(resp, "Modifier option not found", 404); return; }
            modifierDAO.deleteOption(optionId);
            ApiResponse.ok(resp, null, "Deleted");
            return;
        }

        if (segs.length == 4 && "combo".equals(segs[1]) && "items".equals(segs[2])) {
            Integer productId = parseId(segs[0]); Integer itemId = parseId(segs[3]);
            ProductCombo combo = productId == null ? null : modifierDAO.combo(productId);
            ProductComboItem item = itemId == null ? null : modifierDAO.comboItem(itemId);
            if (combo == null || item == null || item.getCombo().getComboId() != combo.getComboId()) { ApiResponse.error(resp, "Combo item not found", 404); return; }
            modifierDAO.deleteComboItem(itemId);
            ApiResponse.ok(resp, null, "Deleted");
            return;
        }

        if (segs.length == 1) {
            Integer id = parseId(segs[0]);
            if (id == null) { resp.sendError(404); return; }
            productDAO.delete(id);
            ApiResponse.ok(resp, null, "Deleted");
            return;
        }

        if (segs.length == 2 && "variants".equals(segs[0])) {
            Integer vid = parseId(segs[1]);
            if (vid == null) { resp.sendError(404); return; }
            productDAO.deleteVariant(vid);
            ApiResponse.ok(resp, null, "Deleted");
            return;
        }

        resp.sendError(404);
    }
}
