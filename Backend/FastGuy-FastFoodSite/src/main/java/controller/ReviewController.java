package controller;

import dto.ApiResponse;
import dto.ReviewDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.ReviewService;
import utils.JsonUtil;
import utils.RoleRequired;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/reviews/*")
public class ReviewController extends HttpServlet {
    private final ReviewService reviewService = new ReviewService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");

        String productId = req.getParameter("product");
        if (productId != null && !productId.isBlank()) {
            writeJson(resp, ApiResponse.ok(reviewService.getByProduct(Long.parseLong(productId))));
        } else {
            resp.setStatus(400);
            writeJson(resp, ApiResponse.error("Vui lòng chọn sản phẩm để xem đánh giá"));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "USER");
        Long userId = (Long) req.getAttribute("userId");
        resp.setContentType("application/json; charset=UTF-8");
        resp.setStatus(201);

        String path = req.getPathInfo();
        if (path != null && path.equals("/order")) {
            Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
            Long orderId = Long.valueOf(body.get("orderId").toString());
            Integer rating = Integer.valueOf(body.get("rating").toString());
            String comment = (String) body.get("comment");
            writeJson(resp, ApiResponse.ok(reviewService.createOrderReview(userId, orderId, rating, comment), "Đánh giá đơn hàng thành công"));
        } else {
            ReviewDTO dto = JsonUtil.fromJson(req.getReader(), ReviewDTO.class);
            writeJson(resp, ApiResponse.ok(reviewService.create(userId, dto), "Gửi đánh giá thành công"));
        }
    }

    private void writeJson(HttpServletResponse resp, Object data) throws IOException {
        resp.getWriter().write(JsonUtil.toJson(data));
    }
}
