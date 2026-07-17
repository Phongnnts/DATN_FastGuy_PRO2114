package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.CartService;
import utils.ApiResponse;
import utils.JwtUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/api/cart/*")
public class CartServlet extends HttpServlet {
    private CartService cartService = new CartService();

    private int getUserId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return -1;
        }
        int userId = JwtUtil.getUserId(authHeader.substring(7));
        if (userId < 0) {
            ApiResponse.error(resp, "Invalid token", 401);
            return -1;
        }
        return userId;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId < 0) return;

        var cart = cartService.getCart(new entity.User() {{ setUserId(userId); }});
        ApiResponse.ok(resp, cart);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId < 0) return;

        Map<String, Object> body = utils.JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) {
            ApiResponse.error(resp, "Invalid data", 400);
            return;
        }

        int productId = body.get("productId") instanceof Number ? ((Number) body.get("productId")).intValue() : -1;
        int variantId = body.get("variantId") instanceof Number ? ((Number) body.get("variantId")).intValue() : -1;
        if (productId <= 0 || variantId <= 0) {
            ApiResponse.error(resp, "Invalid productId or variantId", 400);
            return;
        }
        int quantity = body.containsKey("quantity") && body.get("quantity") instanceof Number ? ((Number) body.get("quantity")).intValue() : 1;
        List<Integer> modifierOptionIds;
        try {
            modifierOptionIds = body.containsKey("modifierOptionIds") ? ((List<?>) body.get("modifierOptionIds")).stream().map(v -> ((Number) v).intValue()).toList() : List.of();
        } catch (Exception e) {
            ApiResponse.error(resp, "Invalid modifier options", 400);
            return;
        }

        boolean ok = cartService.addItem(
                new entity.User() {{ setUserId(userId); }},
                productId, variantId, quantity, modifierOptionIds);
        if (!ok) {
            ApiResponse.error(resp, "Cannot add to cart: invalid product/variant or insufficient stock", 400);
            return;
        }
        resp.setStatus(201);
        ApiResponse.ok(resp, null, "Added to cart");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId < 0) return;

        Map<String, Object> body = utils.JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) {
            ApiResponse.error(resp, "Invalid data", 400);
            return;
        }

        int cartItemId = body.get("cartItemId") instanceof Number ? ((Number) body.get("cartItemId")).intValue() : -1;
        int quantity = body.get("quantity") instanceof Number ? ((Number) body.get("quantity")).intValue() : 0;
        if (cartItemId <= 0 || quantity < 0) {
            ApiResponse.error(resp, "Invalid cartItemId or quantity", 400);
            return;
        }

        boolean ok = cartService.updateItemQuantity(cartItemId, userId, quantity);
        if (!ok) {
            ApiResponse.error(resp, "Cannot update: item not found or insufficient stock", 400);
            return;
        }
        ApiResponse.ok(resp, null, "Updated");
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId < 0) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            ApiResponse.error(resp, "Missing cartItemId", 400);
            return;
        }

        int cartItemId = Integer.parseInt(path.substring(1));
        boolean ok = cartService.removeItem(cartItemId, userId);
        if (!ok) {
            ApiResponse.error(resp, "Item not found", 404);
            return;
        }
        ApiResponse.ok(resp, null, "Removed");
    }
}
