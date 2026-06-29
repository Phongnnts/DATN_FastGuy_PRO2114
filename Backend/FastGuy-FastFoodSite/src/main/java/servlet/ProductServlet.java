package servlet;

import dao.ProductDAO;
import entity.Product;
import entity.ProductVariant;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/products/*")
public class ProductServlet extends HttpServlet {
    private ProductDAO productDAO = new ProductDAO();
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            String categoryParam = req.getParameter("categoryId");
            List<Map<String, Object>> products;
            if (categoryParam != null) {
                int categoryId = Integer.parseInt(categoryParam);
                products = productDAO.findByCategoryId(categoryId).stream().map(this::toMap).collect(Collectors.toList());
            } else {
                products = productDAO.findAllAvailable().stream().map(this::toMap).collect(Collectors.toList());
            }
            ApiResponse.ok(resp, products);
            return;
        }

        if ("/featured".equals(path)) {
            ApiResponse.ok(resp, productDAO.findAllAvailable().stream().limit(4).map(this::toMap).collect(Collectors.toList()));
            return;
        }

        if ("/new".equals(path)) {
            List<Map<String, Object>> products = productDAO.findAllAvailable().stream()
                    .sorted((a, b) -> b.getProductId() - a.getProductId())
                    .limit(8).map(this::toMap).collect(Collectors.toList());
            ApiResponse.ok(resp, products);
            return;
        }
        if ("/promotions".equals(path)) {
            List<Map<String, Object>> products = productDAO.findAllAvailable().stream()
                    .limit(8).map(this::toMap).collect(Collectors.toList());
            ApiResponse.ok(resp, products);
            return;
        }

        try {
            int productId = Integer.parseInt(path.substring(1));
            Product p = productDAO.findById(productId);
            if (p == null || !"AVAILABLE".equals(p.getStatus())) {
                ApiResponse.error(resp, "Product not found", 404);
                return;
            }

            // Related products (same category)
            if (req.getParameter("related") != null) {
                List<Map<String, Object>> related = productDAO.findByCategoryId(p.getCategory().getCategoryId())
                        .stream().filter(r -> r.getProductId() != productId).limit(4)
                        .map(this::toMap).collect(Collectors.toList());
                ApiResponse.ok(resp, related);
                return;
            }

            ApiResponse.ok(resp, toDetailMap(p));
        } catch (NumberFormatException e) {
            resp.sendError(404);
        }
    }

    private Map<String, Object> toVariantMap(ProductVariant v) {
        Map<String, Object> m = new HashMap<>();
        m.put("variantId", v.getVariantId());
        m.put("variantName", v.getVariantName());
        m.put("price", v.getPrice());
        m.put("originalPrice", v.getOriginalPrice());
        m.put("sku", v.getSku());
        m.put("quantityAvailable", v.getQuantityAvailable());
        m.put("isDefault", v.getIsDefault() != null ? v.getIsDefault() : false);
        m.put("status", v.getStatus());
        return m;
    }

    private Map<String, Object> toMap(Product p) {
        Map<String, Object> m = new HashMap<>();
        ProductVariant defaultVariant = productDAO.findDefaultVariantByProductId(p.getProductId());
        m.put("productId", p.getProductId());
        m.put("name", p.getName());
        m.put("description", p.getDescription() != null ? p.getDescription() : "");
        m.put("basePrice", p.getBasePrice());
        m.put("price", defaultVariant != null ? defaultVariant.getPrice() : p.getBasePrice());
        m.put("defaultVariant", defaultVariant != null ? toVariantMap(defaultVariant) : null);
        m.put("imageUrl", p.getImageUrl() != null ? p.getImageUrl() : "");
        m.put("categoryId", p.getCategory().getCategoryId());
        m.put("categoryName", p.getCategory().getName());
        m.put("discountPrice", null);
        m.put("rating", 0);
        m.put("reviewCount", 0);
        m.put("inStock", "AVAILABLE".equals(p.getStatus()));
        m.put("featured", false);
        return m;
    }

    private Map<String, Object> toDetailMap(Product p) {
        Map<String, Object> m = toMap(p);
        List<Map<String, Object>> variants = productDAO.findVariantsByProductId(p.getProductId()).stream()
                .map(this::toVariantMap)
                .collect(Collectors.toList());
        m.put("variants", variants);

        String gallery = p.getGalleryImages();
        List<String> galleryList = new ArrayList<>();
        if (gallery != null && !gallery.isEmpty()) {
            try {
                galleryList = mapper.readValue(gallery, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                for (String url : gallery.split(",")) {
                    String trimmed = url.trim();
                    if (!trimmed.isEmpty()) galleryList.add(trimmed);
                }
            }
        }
        m.put("galleryImages", galleryList);
        return m;
    }
}
