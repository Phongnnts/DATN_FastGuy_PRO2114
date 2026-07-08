package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.ReviewService;
import utils.ApiResponse;
import utils.JsonUtil;
import utils.JwtUtil;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/reviews/*")
public class ReviewServlet extends HttpServlet {
    private ReviewService reviewService = new ReviewService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String path = req.getPathInfo();
        if (path != null && path.startsWith("/product/")) {
            int productId = Integer.parseInt(path.substring("/product/".length()));
            ApiResponse.ok(resp, reviewService.getByProductId(productId));
            return;
        }
        if (path != null && path.startsWith("/order/")) {
            int userId = getUserId(req);
            if (userId <= 0) {
                ApiResponse.error(resp, "Unauthorized", 401);
                return;
            }
            int orderId = Integer.parseInt(path.substring("/order/".length()));
            ApiResponse.ok(resp, reviewService.getByOrderId(orderId));
            return;
        }
        ApiResponse.error(resp, "Not found", 404);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req);
        if (userId <= 0) {
            ApiResponse.error(resp, "Unauthorized", 401);
            return;
        }

        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) {
            ApiResponse.error(resp, "Invalid data", 400);
            return;
        }

        try {
            int orderId = ((Number) body.get("orderId")).intValue();
            int productId = ((Number) body.get("productId")).intValue();
            int rating = ((Number) body.get("rating")).intValue();
            String comment = body.get("comment") != null ? body.get("comment").toString() : "";
            ApiResponse.ok(resp, reviewService.create(userId, orderId, productId, rating, comment), "Reviewed");
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        } catch (Exception e) {
            ApiResponse.error(resp, "Review failed", 500);
        }
    }

    private int getUserId(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return -1;
        return JwtUtil.getUserId(auth.substring(7));
    }
}
