package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.ReviewService;
import dao.OrdersDAO;
import entity.Orders;
import utils.ApiResponse;
import utils.JsonUtil;
import utils.JwtUtil;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/reviews/*")
public class ReviewServlet extends HttpServlet {
    private ReviewService reviewService = new ReviewService();
    private OrdersDAO ordersDAO = new OrdersDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String path = req.getPathInfo();
        int userId = getUserId(req);
        if (userId <= 0) {
            ApiResponse.error(resp, "Unauthorized", 401);
            return;
        }
        if (path != null && path.startsWith("/order/")) {
            try {
                int orderId = Integer.parseInt(path.substring("/order/".length()));
                Orders order = ordersDAO.findById(orderId);
                if (order == null || order.getUser() == null || order.getUser().getUserId() != userId) {
                    ApiResponse.error(resp, "Không tìm thấy đánh giá", 404);
                    return;
                }
                Map<String, Object> review = reviewService.getByOrderId(orderId);
                ApiResponse.ok(resp, review == null ? java.util.Collections.emptyMap() : review);
            } catch (NumberFormatException e) {
                ApiResponse.error(resp, "Mã đơn hàng không hợp lệ", 400);
            }
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
            Object rawOrderId = body.get("orderId");
            Object rawRating = body.get("rating");
            if (!(rawOrderId instanceof Number) || !(rawRating instanceof Number)) {
                ApiResponse.error(resp, "Missing orderId or rating", 400);
                return;
            }
            int orderId = ((Number) rawOrderId).intValue();
            int rating = ((Number) rawRating).intValue();
            String comment = body.get("comment") != null ? body.get("comment").toString() : "";
            ApiResponse.ok(resp, reviewService.create(userId, orderId, rating, comment), "Reviewed");
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
