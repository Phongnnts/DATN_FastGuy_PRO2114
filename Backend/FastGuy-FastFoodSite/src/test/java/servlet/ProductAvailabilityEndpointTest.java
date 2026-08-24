package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dao.ProductDAO;
import dao.ProductModifierDAO;
import dao.ReviewDAO;
import entity.Category;
import entity.Product;
import entity.ProductModifierGroup;
import entity.ProductVariant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.InventoryAvailabilityService;

class ProductAvailabilityEndpointTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void everyPublicProductRouteUsesTheSameFailClosedSerializer() throws Exception {
        Product product = product(1);
        Product related = product(2);
        FakeProductDAO products = new FakeProductDAO(product, related, Map.of(
                1, List.of(variant(11, 999)), 2, List.of(variant(22, 999))));
        InventoryAvailabilityService availability = new InventoryAvailabilityService() {
            @Override public Map<Integer, Map<String, Object>> publicAvailability(List<Integer> ids) {
                Map<Integer, Map<String, Object>> result = new HashMap<>();
                ids.forEach(id -> result.put(id, Map.of("availabilityStatus", "OUT_OF_STOCK")));
                return result;
            }
        };
        ProductServlet servlet = new ProductServlet(products, new EmptyModifiers(), new EmptyReviews(), availability);

        for (Route route : List.of(
                new Route(null, Map.of()), new Route("/1", Map.of()), new Route("/1", Map.of("related", "true")),
                new Route("/featured", Map.of()), new Route("/new", Map.of()), new Route("/promotions", Map.of()))) {
            JsonNode data = invoke(servlet, route).path("data");
            JsonNode item = data.isArray() ? data.get(0) : data;
            assertEquals(false, item.path("inStock").asBoolean(), route.path);
            JsonNode variant = item.path("variants").get(0);
            assertEquals("OUT_OF_STOCK", variant.path("availabilityStatus").asText(), route.path);
            assertPrivateFieldsAbsent(variant);
        }
    }

    private JsonNode invoke(ProductServlet servlet, Route route) throws Exception {
        StringWriter body = new StringWriter();
        PrintWriter writer = new PrintWriter(body);
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { HttpServletRequest.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getPathInfo" -> route.path;
                    case "getParameter" -> route.params.get((String) args[0]);
                    default -> defaultValue(method.getReturnType());
                });
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { HttpServletResponse.class },
                (proxy, method, args) -> "getWriter".equals(method.getName()) ? writer : defaultValue(method.getReturnType()));
        servlet.doGet(request, response);
        writer.flush();
        return JSON.readTree(body.toString());
    }

    private void assertPrivateFieldsAbsent(JsonNode variant) {
        assertFalse(variant.has("inventoryItemId"));
        assertFalse(variant.has("onHandQuantity"));
        assertFalse(variant.has("reservedQuantity"));
        assertFalse(variant.has("limitingItemId"));
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        return 0;
    }

    private Product product(int id) {
        Category category = new Category(); category.setCategoryId(1); category.setName("Food");
        Product product = new Product(); product.setProductId(id); product.setCategory(category); product.setName("Product " + id);
        product.setBasePrice(BigDecimal.TEN); product.setStatus("AVAILABLE"); return product;
    }

    private ProductVariant variant(int id, int legacyQuantity) {
        ProductVariant variant = new ProductVariant(); variant.setVariantId(id); variant.setVariantName("Size");
        variant.setPrice(BigDecimal.TEN); variant.setQuantityAvailable(legacyQuantity); variant.setStatus("AVAILABLE"); variant.setIsDefault(true); return variant;
    }

    private record Route(String path, Map<String, String> params) {}

    private static class FakeProductDAO extends ProductDAO {
        private final Product product; private final Product related; private final Map<Integer, List<ProductVariant>> variants;
        FakeProductDAO(Product product, Product related, Map<Integer, List<ProductVariant>> variants) { this.product = product; this.related = related; this.variants = variants; }
        @Override public List<Product> search(String q, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, String availability, String productType, Boolean discounted, Long minSold, String sort, Integer page, Integer size) { return List.of(product); }
        @Override public List<Product> findAllAvailable() { return List.of(product); }
        @Override public Product findById(int id) { return id == product.getProductId() ? product : null; }
        @Override public List<Product> findByCategoryId(int id) { return List.of(product, related); }
        @Override public Map<Integer, Long> soldCounts(List<Integer> ids) { return Map.of(); }
        @Override public Map<Integer, Integer> featureFlags(List<Integer> ids) { return Map.of(); }
        @Override public Map<Integer, ProductVariant> defaultVariants(List<Integer> ids) { return ids.stream().collect(java.util.stream.Collectors.toMap(id -> id, id -> variants.get(id).get(0))); }
        @Override public Map<Integer, List<ProductVariant>> variantsByProductIds(List<Integer> ids) { return ids.stream().collect(java.util.stream.Collectors.toMap(id -> id, variants::get)); }
    }

    private static class EmptyModifiers extends ProductModifierDAO {
        @Override public Map<Integer, List<ProductModifierGroup>> groupsByProductIds(List<Integer> ids) { return Map.of(); }
        @Override public Map<Integer, List<entity.ProductModifierOption>> optionsByGroupIds(List<Integer> ids) { return Map.of(); }
    }

    private static class EmptyReviews extends ReviewDAO {
        @Override public Map<Integer, ProductReviewSummary> summariesByProductIds(List<Integer> ids) { return Map.of(); }
    }
}
