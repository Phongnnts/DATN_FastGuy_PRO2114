package servlet;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import dao.ProductDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;
import utils.JsonUtil;
import utils.JwtUtil;

@WebServlet("/api/user/favorites/*")
public class FavoritesServlet extends HttpServlet {
    private ProductDAO productDAO = new ProductDAO();
    // ponytail: in-memory store, replace with DB table when favorites feature matures
    private static final ConcurrentHashMap<Integer, CopyOnWriteArrayList<Integer>> userFavorites = new ConcurrentHashMap<>();

    private int getUserId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return -1;
        }
        return JwtUtil.getUserId(authHeader.substring(7));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId < 0) return;

        CopyOnWriteArrayList<Integer> ids = userFavorites.getOrDefault(userId, new CopyOnWriteArrayList<>());
        java.util.List<Map<String, Object>> products = new java.util.ArrayList<>();
        for (Integer pid : ids) {
            var p = productDAO.findById(pid);
            if (p != null) {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("productId", p.getProductId());
                m.put("name", p.getName());
                m.put("imageUrl", p.getImageUrl());
                m.put("basePrice", p.getBasePrice());
                products.add(m);
            }
        }
        ApiResponse.ok(resp, products);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId < 0) return;

        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null || !body.containsKey("productId")) {
            ApiResponse.error(resp, "Missing productId", 400);
            return;
        }
        int productId = ((Number) body.get("productId")).intValue();
        userFavorites.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).addIfAbsent(productId);
        ApiResponse.ok(resp, null, "Added to favorites");
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId < 0) return;

        String path = req.getPathInfo();
        if (path != null && path.length() > 1) {
            try {
                int productId = Integer.parseInt(path.substring(1));
                CopyOnWriteArrayList<Integer> ids = userFavorites.get(userId);
                if (ids != null) ids.remove(Integer.valueOf(productId));
            } catch (NumberFormatException ignored) {}
        }
        ApiResponse.ok(resp, null, "Removed from favorites");
    }
}
