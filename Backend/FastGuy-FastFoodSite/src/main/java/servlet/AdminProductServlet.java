package servlet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.CategoryDAO;
import dao.ProductDAO;
import entity.Category;
import entity.Product;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/admin/products/*")
public class AdminProductServlet extends HttpServlet {
    private static final ObjectMapper mapper = new ObjectMapper();
    private ProductDAO productDAO = new ProductDAO();
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

    private String validateProduct(Map<String, Object> body, boolean create) {
        if (create && !(body.get("categoryId") instanceof Number)) return "Category is required";
        if (body.containsKey("categoryId") && !(body.get("categoryId") instanceof Number)) return "Category is invalid";
        if (create && !(body.get("name") instanceof String)) return "Name is required";
        if (body.containsKey("name")) {
            String name = ((String) body.get("name")).trim();
            if (name.length() < 2 || name.length() > 150) return "Name must be 2-150 characters";
        }
        if (body.containsKey("basePrice")) {
            if (!(body.get("basePrice") instanceof Number)) return "Base price is invalid";
            if (((Number) body.get("basePrice")).doubleValue() < 0) return "Base price cannot be negative";
        }
        if (body.containsKey("status") && !List.of("AVAILABLE", "UNAVAILABLE").contains(body.get("status"))) return "Status is invalid";
        return null;
    }

    private String validateVariant(Map<String, Object> body, boolean create) {
        if (create && !(body.get("variantName") instanceof String)) return "Variant name is required";
        if (body.containsKey("variantName")) {
            String name = ((String) body.get("variantName")).trim();
            if (name.length() < 1 || name.length() > 100) return "Variant name is invalid";
        }
        if (body.containsKey("price")) {
            if (!(body.get("price") instanceof Number)) return "Variant price is invalid";
            if (((Number) body.get("price")).doubleValue() < 0) return "Variant price cannot be negative";
        }
        if (body.containsKey("quantityAvailable")) {
            if (!(body.get("quantityAvailable") instanceof Number)) return "Quantity is invalid";
            if (((Number) body.get("quantityAvailable")).intValue() < 0) return "Quantity cannot be negative";
        }
        if (body.containsKey("status") && !List.of("AVAILABLE", "UNAVAILABLE").contains(body.get("status"))) return "Status is invalid";
        return null;
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
        m.put("galleryImages", parseGalleryImages(p));
        m.put("variants", productDAO.findVariantsByProductId(p.getProductId()).stream()
                .map(this::toVariantMap).collect(Collectors.toList()));
        m.put("discountPrice", null);
        m.put("rating", 0);
        m.put("reviewCount", 0);
        m.put("inStock", "AVAILABLE".equals(p.getStatus()));
        m.put("featured", false);
        return m;
    }

    private String[] splitPath(String path) {
        if (path == null || path.equals("/")) return new String[0];
        return Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    private Integer parseId(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
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
            String validationError = validateProduct(body, true);
            if (validationError != null) { ApiResponse.error(resp, validationError, 400); return; }
            Integer categoryId = ((Number) body.get("categoryId")).intValue();
            Category category = categoryDAO.findById(categoryId);
            if (category == null) { ApiResponse.error(resp, "Category not found", 400); return; }

            Product p = new Product();
            p.setCategory(category);
            p.setName(((String) body.get("name")).trim());
            p.setDescription((String) body.get("description"));
            if (body.containsKey("basePrice")) {
                p.setBasePrice(BigDecimal.valueOf(((Number) body.get("basePrice")).doubleValue()));
            }
            p.setImageUrl((String) body.get("imageUrl"));
            if (body.containsKey("galleryImages")) {
                p.setGalleryImages(toGalleryJson((List<String>) body.get("galleryImages")));
            }
            p.setStatus((String) body.getOrDefault("status", "AVAILABLE"));
            p.setCreatedAt(LocalDateTime.now());
            productDAO.save(p);

            resp.setStatus(201);
            ApiResponse.ok(resp, toMap(p), "Created");
            return;
        }

        if (segs.length == 2 && "variants".equals(segs[1])) {
            Integer productId = parseId(segs[0]);
            if (productId == null) { resp.sendError(404); return; }
            Product p = productDAO.findById(productId);
            if (p == null) { ApiResponse.error(resp, "Product not found", 404); return; }

            String validationError = validateVariant(body, true);
            if (validationError != null) { ApiResponse.error(resp, validationError, 400); return; }
            ProductVariant v = new ProductVariant();
            v.setProduct(p);
            v.setVariantName(((String) body.get("variantName")).trim());
            if (body.containsKey("price")) {
                v.setPrice(BigDecimal.valueOf(((Number) body.get("price")).doubleValue()));
            }
            if (body.containsKey("originalPrice")) {
                v.setOriginalPrice(BigDecimal.valueOf(((Number) body.get("originalPrice")).doubleValue()));
            }
            v.setSku((String) body.get("sku"));
            if (body.containsKey("quantityAvailable")) {
                v.setQuantityAvailable(((Number) body.get("quantityAvailable")).intValue());
            }
            v.setIsDefault(body.containsKey("isDefault") ? (Boolean) body.get("isDefault") : false);
            v.setStatus((String) body.getOrDefault("status", "AVAILABLE"));
            v.setCreatedAt(LocalDateTime.now());
            productDAO.saveVariant(v);

            resp.setStatus(201);
            ApiResponse.ok(resp, toVariantMap(v), "Created");
            return;
        }

        resp.sendError(404);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String[] segs = splitPath(req.getPathInfo());
        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }

        if (segs.length == 1) {
            Integer id = parseId(segs[0]);
            if (id == null) { resp.sendError(404); return; }
            Product p = productDAO.findById(id);
            if (p == null) { ApiResponse.error(resp, "Not found", 404); return; }
            String validationError = validateProduct(body, false);
            if (validationError != null) { ApiResponse.error(resp, validationError, 400); return; }

            if (body.containsKey("categoryId")) {
                Category c = categoryDAO.findById(((Number) body.get("categoryId")).intValue());
                if (c != null) p.setCategory(c);
            }
            if (body.containsKey("name")) p.setName(((String) body.get("name")).trim());
            if (body.containsKey("description")) p.setDescription((String) body.get("description"));
            if (body.containsKey("basePrice")) p.setBasePrice(BigDecimal.valueOf(((Number) body.get("basePrice")).doubleValue()));
            if (body.containsKey("imageUrl")) p.setImageUrl((String) body.get("imageUrl"));
            if (body.containsKey("galleryImages")) {
                p.setGalleryImages(toGalleryJson((List<String>) body.get("galleryImages")));
            }
            if (body.containsKey("status")) p.setStatus((String) body.get("status"));
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
            String validationError = validateVariant(body, false);
            if (validationError != null) { ApiResponse.error(resp, validationError, 400); return; }

            if (body.containsKey("variantName")) v.setVariantName(((String) body.get("variantName")).trim());
            if (body.containsKey("price")) v.setPrice(BigDecimal.valueOf(((Number) body.get("price")).doubleValue()));
            if (body.containsKey("originalPrice")) v.setOriginalPrice(BigDecimal.valueOf(((Number) body.get("originalPrice")).doubleValue()));
            if (body.containsKey("sku")) v.setSku((String) body.get("sku"));
            if (body.containsKey("quantityAvailable")) v.setQuantityAvailable(((Number) body.get("quantityAvailable")).intValue());
            if (body.containsKey("isDefault")) v.setIsDefault((Boolean) body.get("isDefault"));
            if (body.containsKey("status")) v.setStatus((String) body.get("status"));
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
