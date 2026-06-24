package servlet;

import dao.ProductDAO;
import entity.ProductOption;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/products/*")
public class ProductServlet extends HttpServlet {
    private ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
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
            return;
        }

        try {
            int productId = Integer.parseInt(path.substring(1));
            entity.Product p = productDAO.findById(productId);
            if (p == null || !"AVAILABLE".equals(p.getStatus())) {
                ApiResponse.error(resp, "Product not found", 404);
                return;
            }
            ApiResponse.ok(resp, toDetailMap(p));
        } catch (NumberFormatException e) {
            resp.sendError(404);
        }
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

    private Map<String, Object> toDetailMap(entity.Product p) {
        Map<String, Object> m = toMap(p);

        List<Map<String, Object>> options = productDAO.findOptionsByProductId(p.getProductId())
                .stream()
                .map(o -> {
                    Map<String, Object> om = new HashMap<>();
                    om.put("optionId", o.getOptionId());
                    om.put("optionName", o.getOptionName());
                    om.put("extraPrice", o.getExtraPrice());
                    om.put("stockControlled", o.getStockControlled() != null ? o.getStockControlled() : false);
                    om.put("quantityAvailable", o.getQuantityAvailable());
                    return om;
                })
                .collect(Collectors.toList());
        m.put("options", options);

        String gallery = p.getGalleryImages();
        List<String> galleryList = new ArrayList<>();
        if (gallery != null && !gallery.isEmpty()) {
            for (String url : gallery.split(",")) {
                String trimmed = url.trim();
                if (!trimmed.isEmpty()) galleryList.add(trimmed);
            }
        }
        m.put("galleryImages", galleryList);

        return m;
    }
}
