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

@WebServlet("/api/reviews/*")
public class ReviewController extends HttpServlet {
    private final ReviewService reviewService = new ReviewService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");

        String productId = req.getParameter("product");
        if (productId != null) {
            writeJson(resp, ApiResponse.ok(reviewService.getByProduct(Long.parseLong(productId))));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "USER");
        Long userId = (Long) req.getAttribute("userId");
        resp.setContentType("application/json; charset=UTF-8");
        resp.setStatus(201);

        ReviewDTO dto = JsonUtil.fromJson(req.getReader(), ReviewDTO.class);
        writeJson(resp, ApiResponse.ok(reviewService.create(userId, dto), "Gửi đánh giá thành công"));
    }

    private void writeJson(HttpServletResponse resp, Object data) throws IOException {
        resp.getWriter().write(JsonUtil.toJson(data));
    }
}
