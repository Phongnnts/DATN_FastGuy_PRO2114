package controller;

import dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.ProductService;
import utils.JsonUtil;

import java.io.IOException;

@WebServlet("/api/products/*")
public class ProductController extends HttpServlet {
    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");

        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            String category = req.getParameter("category");
            String keyword = req.getParameter("keyword");

            Object result;
            if (keyword != null && !keyword.isBlank()) {
                result = productService.search(keyword);
            } else if (category != null && !category.isBlank()) {
                result = productService.getByCategory(Long.parseLong(category));
            } else {
                result = productService.getAllAvailable();
            }
            writeJson(resp, ApiResponse.ok(result));
        } else {
            Long productId = parseIdFromPath(pathInfo);
            writeJson(resp, ApiResponse.ok(productService.getById(productId)));
        }
    }

    private Long parseIdFromPath(String pathInfo) {
        String[] parts = pathInfo.split("/");
        if (parts.length < 2) return null;
        return Long.parseLong(parts[1]);
    }

    private void writeJson(HttpServletResponse resp, Object data) throws IOException {
        resp.getWriter().write(JsonUtil.toJson(data));
    }
}
