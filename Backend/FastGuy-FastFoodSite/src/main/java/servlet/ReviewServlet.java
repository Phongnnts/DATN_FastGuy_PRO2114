package servlet;

import dao.OrdersDAO;
import dao.ProductDAO;
import dao.ReviewDAO;
import entity.Orders;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import service.ReviewService;
import utils.ApiResponse;
import utils.JsonUtil;
import utils.JwtUtil;

@WebServlet("/api/reviews/*")
public class ReviewServlet extends HttpServlet {

    private ReviewService reviewService = new ReviewService();
    private OrdersDAO ordersDAO = new OrdersDAO();
    private ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String path = req.getPathInfo();
        if (path != null && path.startsWith("/product/")) {
            if (!path.matches("/product/[^/]+")) {
                ApiResponse.error(resp, "Not found", 404);
                return;
            }
            int productId;
            int page;
            int size;
            try {
                productId = positiveInt(path.substring("/product/".length()));
                page = queryInt(req, "page", 1);
                size = queryInt(req, "size", 10);
                if (size > 50) throw new IllegalArgumentException();
            } catch (IllegalArgumentException e) {
                ApiResponse.error(resp, "Trang đánh giá không hợp lệ", 400);
                return;
            }
            try {
                if (productDAO.findById(productId) == null) {
                    ApiResponse.error(resp, "Không tìm thấy sản phẩm", 404);
                    return;
                }
                ApiResponse.ok(
                    resp,
                    reviewService.getByProductId(productId, page, size)
                );
            } catch (RuntimeException e) {
                ApiResponse.error(resp, "Review failed", 500);
            }
            return;
        }
        if (path != null && path.startsWith("/order/")) {
            if (!path.matches("/order/[^/]+")) {
                ApiResponse.error(resp, "Not found", 404);
                return;
            }
            int userId = getUserId(req);
            if (userId <= 0) {
                ApiResponse.error(resp, "Unauthorized", 401);
                return;
            }
            int orderId;
            try {
                orderId = positiveInt(path.substring("/order/".length()));
            } catch (IllegalArgumentException e) {
                ApiResponse.error(resp, "Mã đơn hàng không hợp lệ", 400);
                return;
            }
            try {
                Orders order = ordersDAO.findById(orderId);
                if (
                    order == null ||
                    order.getUser() == null ||
                    order.getUser().getUserId() != userId
                ) {
                    ApiResponse.error(resp, "Không tìm thấy đánh giá", 404);
                    return;
                }
                ApiResponse.ok(
                    resp,
                    reviewService.getByOrderId(userId, orderId)
                );
            } catch (RuntimeException e) {
                ApiResponse.error(resp, "Review failed", 500);
            }
            return;
        }
        ApiResponse.error(resp, "Not found", 404);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req);
        if (userId <= 0) {
            ApiResponse.error(resp, "Unauthorized", 401);
            return;
        }

        try {
            Map<String, Object> body = JsonUtil.fromJson(
                req.getReader(),
                Map.class
            );
            if (body == null) throw new IllegalArgumentException(
                "Dữ liệu không hợp lệ"
            );
            Object rawOrderId = body.get("orderId");
            Object rawProductId = body.get("productId");
            Object rawRating = body.get("rating");
            Object rawComment = body.get("comment");
            Object rawHomepageConsent = body.get("homepageConsent");
            if (
                !isIntegral(rawOrderId) ||
                !isIntegral(rawProductId) ||
                !isIntegral(rawRating) ||
                (rawComment != null && !(rawComment instanceof String)) ||
                (rawHomepageConsent != null &&
                    !(rawHomepageConsent instanceof Boolean))
            ) {
                throw new IllegalArgumentException("Dữ liệu không hợp lệ");
            }
            int orderId = ((Number) rawOrderId).intValue();
            Orders order = ordersDAO.findById(orderId);
            if (
                order == null ||
                order.getUser() == null ||
                order.getUser().getUserId() != userId
            ) {
                ApiResponse.error(resp, "Không tìm thấy đánh giá", 404);
                return;
            }
            ApiResponse.ok(
                resp,
                reviewService.create(
                    userId,
                    orderId,
                    ((Number) rawProductId).intValue(),
                    ((Number) rawRating).intValue(),
                    (String) rawComment,
                    false
                ),
                "Reviewed"
            );
        } catch (ReviewDAO.AlreadyReviewedException e) {
            ApiResponse.error(resp, "ALREADY_REVIEWED", 409);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(
                resp,
                e.getMessage() == null
                    ? "Dữ liệu không hợp lệ"
                    : e.getMessage(),
                400
            );
        } catch (RuntimeException e) {
            ApiResponse.error(resp, "Review failed", 500);
        }
    }

    private int getUserId(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return -1;
        return JwtUtil.getUserId(auth.substring(7));
    }

    private int queryInt(
        HttpServletRequest req,
        String name,
        int defaultValue
    ) {
        String value = req.getParameter(name);
        return value == null ? defaultValue : positiveInt(value);
    }

    private int positiveInt(String value) {
        int parsed = Integer.parseInt(value);
        if (parsed < 1) throw new IllegalArgumentException();
        return parsed;
    }

    private boolean isIntegral(Object value) {
        if (!(value instanceof Number number)) return false;
        double decimal = number.doubleValue();
        return (
            Double.isFinite(decimal) &&
            decimal == number.intValue() &&
            number.intValue() > 0
        );
    }
}
