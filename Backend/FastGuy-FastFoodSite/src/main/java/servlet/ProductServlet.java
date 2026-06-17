package servlet;

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

@WebServlet("/api/products")
public class ProductServlet extends HttpServlet {
    private ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String categoryParam = req.getParameter("categoryId");

        List<Map<String, Object>> products;

        if (categoryParam != null) {
            int categoryId = Integer.parseInt(categoryParam);
            products = productDAO.findByCategoryId(categoryId)
                    .stream()
                    .map(this::toMap)
                    .collect(Collectors.toList());
        } else {
            products = productDAO.findAllAvailable()
                    .stream()
                    .map(this::toMap)
                    .collect(Collectors.toList());
        }

        ApiResponse.ok(resp, products);
    }

    private Map<String, Object> toMap(entity.Product p) {
        Map<String, Object> m = new HashMap<>();
        m.put("productId", p.getProductId());
        m.put("name", p.getName());
        m.put("description", p.getDescription() != null ? p.getDescription() : "");
        m.put("price", p.getPrice());
        m.put("imageUrl", p.getImageUrl() != null ? p.getImageUrl() : "");
        m.put("categoryId", p.getCategory().getCategoryId());
        m.put("categoryName", p.getCategory().getName());
        return m;
    }
}
