package servlet;

import dao.CategoryDAO;
import dao.ProductDAO;
import entity.Category;
import entity.Product;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/admin/products/*")
public class AdminProductServlet extends HttpServlet {
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
        return m;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            List<Map<String, Object>> list = productDAO.findAll().stream()
                    .map(this::toMap).collect(Collectors.toList());
            ApiResponse.ok(resp, list);
        } else {
            try {
                int id = Integer.parseInt(path.substring(1));
                Product p = productDAO.findById(id);
                if (p == null) { ApiResponse.error(resp, "Not found", 404); return; }
                ApiResponse.ok(resp, toMap(p));
            } catch (NumberFormatException e) {
                resp.sendError(404);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

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
        p.setStatus((String) body.getOrDefault("status", "AVAILABLE"));
        p.setCreatedAt(LocalDateTime.now());
        productDAO.save(p);

        resp.setStatus(201);
        ApiResponse.ok(resp, toMap(p), "Created");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { resp.sendError(404); return; }

        int id = Integer.parseInt(path.substring(1));
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
        if (body.containsKey("status")) p.setStatus((String) body.get("status"));
        p.setUpdatedAt(LocalDateTime.now());
        productDAO.save(p);

        ApiResponse.ok(resp, toMap(p), "Updated");
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { resp.sendError(404); return; }

        int id = Integer.parseInt(path.substring(1));
        productDAO.delete(id);
        ApiResponse.ok(resp, null, "Deleted");
    }
}
