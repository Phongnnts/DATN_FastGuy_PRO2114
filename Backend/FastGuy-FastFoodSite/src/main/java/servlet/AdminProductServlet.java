package servlet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.CategoryDAO;
import dao.ProductDAO;
import entity.Category;
import entity.Product;
import entity.ProductOption;
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

    private Map<String, Object> toOptionMap(ProductOption o) {
        Map<String, Object> m = new HashMap<>();
        m.put("optionId", o.getOptionId());
        m.put("optionName", o.getOptionName());
        m.put("extraPrice", o.getExtraPrice());
        m.put("stockControlled", o.getStockControlled());
        m.put("quantityAvailable", o.getQuantityAvailable());
        return m;
    }

    private Map<String, Object> toMap(Product p) {
        Map<String, Object> m = new HashMap<>();
        m.put("productId", p.getProductId());
        m.put("name", p.getName());
        m.put("categoryId", p.getCategory().getCategoryId());
        m.put("categoryName", p.getCategory().getName());
        m.put("price", p.getPrice());
        m.put("imageUrl", p.getImageUrl() != null ? p.getImageUrl() : "");
        m.put("description", p.getDescription() != null ? p.getDescription() : "");
        m.put("status", p.getStatus());
        m.put("galleryImages", parseGalleryImages(p));
        m.put("options", productDAO.findOptionsByProductId(p.getProductId()).stream()
                .map(this::toOptionMap).collect(Collectors.toList()));
        return m;
    }

    private String[] splitPath(String path) {
        if (path == null || path.equals("/")) return new String[0];
        return Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).toArray(String[]::new);
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
            try {
                int id = Integer.parseInt(segs[0]);
                Product p = productDAO.findById(id);
                if (p == null) { ApiResponse.error(resp, "Not found", 404); return; }
                ApiResponse.ok(resp, toMap(p));
            } catch (NumberFormatException e) {
                resp.sendError(404);
            }
            return;
        }

        if (segs.length == 2 && "options".equals(segs[1])) {
            try {
                int id = Integer.parseInt(segs[0]);
                Product p = productDAO.findById(id);
                if (p == null) { ApiResponse.error(resp, "Not found", 404); return; }
                List<Map<String, Object>> options = productDAO.findOptionsByProductId(id).stream()
                        .map(this::toOptionMap).collect(Collectors.toList());
                ApiResponse.ok(resp, options);
            } catch (NumberFormatException e) {
                resp.sendError(404);
            }
            return;
        }

        resp.sendError(404);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String[] segs = splitPath(req.getPathInfo());

        if (segs.length == 0) {
            Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
            if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }

            Integer categoryId = ((Number) body.get("categoryId")).intValue();
            Category category = categoryDAO.findById(categoryId);
            if (category == null) { ApiResponse.error(resp, "Category not found", 400); return; }

            Product p = new Product();
            p.setCategory(category);
            p.setName((String) body.get("name"));
            p.setDescription((String) body.get("description"));
            p.setPrice(BigDecimal.valueOf(((Number) body.get("price")).doubleValue()));
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

        if (segs.length == 2 && "options".equals(segs[1])) {
            try {
                int productId = Integer.parseInt(segs[0]);
                Product p = productDAO.findById(productId);
                if (p == null) { ApiResponse.error(resp, "Product not found", 404); return; }

                Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
                if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }

                ProductOption opt = new ProductOption();
                opt.setProduct(p);
                opt.setOptionName((String) body.get("optionName"));
                opt.setExtraPrice(body.containsKey("extraPrice")
                        ? BigDecimal.valueOf(((Number) body.get("extraPrice")).doubleValue())
                        : BigDecimal.ZERO);
                opt.setStockControlled(body.containsKey("stockControlled")
                        ? (Boolean) body.get("stockControlled") : false);
                opt.setQuantityAvailable(body.containsKey("quantityAvailable")
                        ? ((Number) body.get("quantityAvailable")).intValue() : null);
                productDAO.saveOption(opt);

                resp.setStatus(201);
                ApiResponse.ok(resp, toOptionMap(opt), "Created");
            } catch (NumberFormatException e) {
                resp.sendError(404);
            }
            return;
        }

        resp.sendError(404);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String[] segs = splitPath(req.getPathInfo());

        if (segs.length == 1) {
            try {
                int id = Integer.parseInt(segs[0]);
                Product p = productDAO.findById(id);
                if (p == null) { ApiResponse.error(resp, "Not found", 404); return; }

                Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
                if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }

                if (body.containsKey("categoryId")) {
                    Category c = categoryDAO.findById(((Number) body.get("categoryId")).intValue());
                    if (c != null) p.setCategory(c);
                }
                if (body.containsKey("name")) p.setName((String) body.get("name"));
                if (body.containsKey("description")) p.setDescription((String) body.get("description"));
                if (body.containsKey("price")) p.setPrice(BigDecimal.valueOf(((Number) body.get("price")).doubleValue()));
                if (body.containsKey("imageUrl")) p.setImageUrl((String) body.get("imageUrl"));
                if (body.containsKey("galleryImages")) {
                    p.setGalleryImages(toGalleryJson((List<String>) body.get("galleryImages")));
                }
                if (body.containsKey("status")) p.setStatus((String) body.get("status"));
                p.setUpdatedAt(LocalDateTime.now());
                productDAO.save(p);

                ApiResponse.ok(resp, toMap(p), "Updated");
            } catch (NumberFormatException e) {
                resp.sendError(404);
            }
            return;
        }

        if (segs.length == 2 && "options".equals(segs[0])) {
            try {
                int optionId = Integer.parseInt(segs[1]);
                ProductOption opt = productDAO.findOptionById(optionId);
                if (opt == null) { ApiResponse.error(resp, "Not found", 404); return; }

                Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
                if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }

                if (body.containsKey("optionName")) opt.setOptionName((String) body.get("optionName"));
                if (body.containsKey("extraPrice")) opt.setExtraPrice(BigDecimal.valueOf(((Number) body.get("extraPrice")).doubleValue()));
                if (body.containsKey("stockControlled")) opt.setStockControlled((Boolean) body.get("stockControlled"));
                if (body.containsKey("quantityAvailable")) opt.setQuantityAvailable(((Number) body.get("quantityAvailable")).intValue());
                productDAO.saveOption(opt);

                ApiResponse.ok(resp, toOptionMap(opt), "Updated");
            } catch (NumberFormatException e) {
                resp.sendError(404);
            }
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
            try {
                int id = Integer.parseInt(segs[0]);
                productDAO.delete(id);
                ApiResponse.ok(resp, null, "Deleted");
            } catch (NumberFormatException e) {
                resp.sendError(404);
            }
            return;
        }

        if (segs.length == 2 && "options".equals(segs[0])) {
            try {
                int optionId = Integer.parseInt(segs[1]);
                productDAO.deleteOption(optionId);
                ApiResponse.ok(resp, null, "Deleted");
            } catch (NumberFormatException e) {
                resp.sendError(404);
            }
            return;
        }

        resp.sendError(404);
    }
}
