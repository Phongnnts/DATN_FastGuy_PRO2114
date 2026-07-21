package servlet;

import dao.CategoryDAO;
import dao.ProductDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/categories")
public class CategoryServlet extends HttpServlet {
    private CategoryDAO categoryDAO = new CategoryDAO();
    private ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        Map<Integer, Long> productCounts = productDAO.countAvailableByCategory();

        List<Map<String, Object>> categories = categoryDAO.findAllActive()
                .stream()
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("categoryId", c.getCategoryId());
                    m.put("name", c.getName());
                    m.put("description", c.getDescription() != null ? c.getDescription() : "");
                    m.put("sortOrder", c.getSortOrder());
                    m.put("productCount", productCounts.getOrDefault(c.getCategoryId(), 0L));
                    return m;
                })
                .collect(Collectors.toList());

        ApiResponse.ok(resp, categories);
    }
}
