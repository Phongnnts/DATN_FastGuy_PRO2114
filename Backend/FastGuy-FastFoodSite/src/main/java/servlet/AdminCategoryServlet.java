package servlet;

import dao.CategoryDAO;
import dao.ProductDAO;
import entity.Category;
import jakarta.persistence.PersistenceException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;
import utils.JsonUtil;
import utils.JwtUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/admin/categories/*")
public class AdminCategoryServlet extends HttpServlet {
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final ProductDAO productDAO = new ProductDAO();

    private boolean checkAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) { ApiResponse.error(resp, "Missing token", 401); return false; }
        if (!"ADMIN".equals(JwtUtil.getRole(auth.substring(7)))) { ApiResponse.error(resp, "Forbidden", 403); return false; }
        return true;
    }

    private Map<String, Object> toMap(Category category) {
        Map<String, Object> result = new HashMap<>();
        result.put("categoryId", category.getCategoryId());
        result.put("name", category.getName());
        result.put("description", category.getDescription() != null ? category.getDescription() : "");
        result.put("productCount", productDAO.findByCategoryId(category.getCategoryId()).size());
        return result;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            List<Map<String, Object>> list = categoryDAO.findAll().stream().map(this::toMap).collect(Collectors.toList());
            ApiResponse.ok(resp, list);
            return;
        }
        try {
            Category category = categoryDAO.findById(Integer.parseInt(path.substring(1)));
            if (category == null) { ApiResponse.error(resp, "Not found", 404); return; }
            ApiResponse.ok(resp, toMap(category));
        } catch (NumberFormatException e) { ApiResponse.error(resp, "Invalid ID", 400); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;
        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }
        try {
            Category category = new Category();
            apply(body, category, true);
            category.setStatus("ACTIVE");
            categoryDAO.save(category);
            resp.setStatus(201);
            ApiResponse.ok(resp, toMap(category), "Created");
        } catch (IllegalArgumentException e) { ApiResponse.error(resp, e.getMessage(), 400); }
        catch (PersistenceException e) { ApiResponse.error(resp, "Không thể lưu danh mục", 409); }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;
        try {
            Category category = categoryDAO.findById(id(req));
            if (category == null) { ApiResponse.error(resp, "Not found", 404); return; }
            Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
            if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }
            apply(body, category, false);
            categoryDAO.save(category);
            ApiResponse.ok(resp, toMap(category), "Updated");
        } catch (NumberFormatException e) { ApiResponse.error(resp, "Invalid ID", 400); }
        catch (IllegalArgumentException e) { ApiResponse.error(resp, e.getMessage(), 400); }
        catch (PersistenceException e) { ApiResponse.error(resp, "Không thể lưu danh mục", 409); }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;
        try {
            int id = id(req);
            if (categoryDAO.findById(id) == null) { ApiResponse.error(resp, "Not found", 404); return; }
            if (categoryDAO.hasProducts(id)) { ApiResponse.error(resp, "Không thể xóa danh mục đang có sản phẩm", 409); return; }
            categoryDAO.delete(id);
            ApiResponse.ok(resp, null, "Deleted");
        } catch (NumberFormatException e) { ApiResponse.error(resp, "Invalid ID", 400); }
        catch (PersistenceException e) { ApiResponse.error(resp, "Không thể xóa danh mục", 409); }
    }

    private int id(HttpServletRequest req) {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) throw new NumberFormatException();
        return Integer.parseInt(path.substring(1));
    }

    private void apply(Map<String, Object> body, Category category, boolean creating) {
        if (creating || body.containsKey("name")) {
            Object raw = body.get("name");
            if (!(raw instanceof String name) || name.trim().isEmpty() || name.trim().length() > 100) throw new IllegalArgumentException("Tên danh mục phải từ 1 đến 100 ký tự");
            category.setName(name.trim());
        }
        if (body.containsKey("description")) {
            Object raw = body.get("description");
            if (raw != null && (!(raw instanceof String text) || text.length() > 500)) throw new IllegalArgumentException("Mô tả không hợp lệ");
            category.setDescription(raw == null ? "" : ((String) raw).trim());
        }
    }
}
