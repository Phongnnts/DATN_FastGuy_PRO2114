package servlet;

import dao.CategoryDAO;
import entity.Category;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;
import utils.JwtUtil;
import utils.JsonUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/admin/categories/*")
public class AdminCategoryServlet extends HttpServlet {
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

    private Map<String, Object> toMap(Category c) {
        Map<String, Object> m = new HashMap<>();
        m.put("categoryId", c.getCategoryId());
        m.put("name", c.getName());
        m.put("description", c.getDescription() != null ? c.getDescription() : "");
        m.put("productCount", 0);
        return m;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            List<Map<String, Object>> list = categoryDAO.findAll().stream()
                    .map(this::toMap).collect(Collectors.toList());
            ApiResponse.ok(resp, list);
        } else {
            try {
                int id = Integer.parseInt(path.substring(1));
                Category c = categoryDAO.findById(id);
                if (c == null) { ApiResponse.error(resp, "Not found", 404); return; }
                ApiResponse.ok(resp, toMap(c));
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

        Category c = new Category();
        c.setName((String) body.get("name"));
        c.setDescription((String) body.get("description"));
        c.setStatus("ACTIVE");
        categoryDAO.save(c);

        resp.setStatus(201);
        ApiResponse.ok(resp, toMap(c), "Created");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { resp.sendError(404); return; }

        int id = Integer.parseInt(path.substring(1));
        Category c = categoryDAO.findById(id);
        if (c == null) { ApiResponse.error(resp, "Not found", 404); return; }

        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }

        if (body.containsKey("name")) c.setName((String) body.get("name"));
        if (body.containsKey("description")) c.setDescription((String) body.get("description"));
        categoryDAO.save(c);

        ApiResponse.ok(resp, toMap(c), "Updated");
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { resp.sendError(404); return; }

        int id = Integer.parseInt(path.substring(1));
        categoryDAO.delete(id);
        ApiResponse.ok(resp, null, "Deleted");
    }
}
