package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import entity.Product;

class HomepageAdminContractTest {
    @Test
    void homepageServletUsesPublicContractPath() {
        WebServlet mapping = HomepageServlet.class.getAnnotation(WebServlet.class);
        assertEquals("/api/homepage", mapping.value()[0]);
        assertEquals(Map.of("bestSellers", java.util.List.of(), "featuredReviews", java.util.List.of()),
                HomepageServlet.responseData(Map.of("bestSellers", java.util.List.of(), "featuredReviews", java.util.List.of())));
    }

    @Test
    void adminProductValidationIsTypedAndBounded() {
        assertEquals(true, AdminProductServlet.readBoolean(Map.of("isNew", true), "isNew", false));
        assertThrows(IllegalArgumentException.class, () -> AdminProductServlet.readBoolean(Map.of("isNew", "true"), "isNew", false));
        assertEquals(3, AdminProductServlet.readInteger(Map.of("spiceLevel", 3), "spiceLevel", 0, 0, 3));
        assertThrows(IllegalArgumentException.class, () -> AdminProductServlet.readInteger(Map.of("spiceLevel", 4), "spiceLevel", 0, 0, 3));
        assertEquals("GROUP", AdminProductServlet.readHomepageOccasion(Map.of("homepageOccasion", "GROUP"), null));
        Map<String, Object> nullableOccasion = new HashMap<>();
        nullableOccasion.put("homepageOccasion", null);
        assertEquals(null, AdminProductServlet.readHomepageOccasion(nullableOccasion, "GROUP"));
        assertThrows(IllegalArgumentException.class, () -> AdminProductServlet.readHomepageOccasion(Map.of("homepageOccasion", "OTHER"), null));
    }

    @Test
    void adminProductPutRejectsEmptyUnknownAndWrongTypedPayloads() throws Exception {
        TestAdminProductServlet servlet = new TestAdminProductServlet();
        assertEquals(400, invoke(servlet, "/10", "{}").status);
        assertEquals(400, invoke(servlet, "/10", "{\"unknown\":true}").status);
        assertEquals(400, invoke(servlet, "/10", "{\"name\":7}").status);
        assertEquals(400, invoke(servlet, "/10", "{\"galleryImages\":[\"ok\",7]}").status);
    }

    @Test
    void productValidatorAllowsExistingClientPayloadAndPartialMetadata() {
        AdminProductServlet.validateProductUpdate(Map.of("isNew", true, "spiceLevel", 3));
        Map<String, Object> client = new HashMap<>();
        client.put("name", "Burger"); client.put("categoryId", 2); client.put("basePrice", 49000);
        client.put("imageUrl", "/images/burger.jpg"); client.put("description", null); client.put("status", "AVAILABLE");
        client.put("availableFrom", null); client.put("availableTo", "22:00"); client.put("galleryImages", List.of("/images/1.jpg"));
        AdminProductServlet.validateProductUpdate(client);
    }

    @Test
    void productCreateValidatorRequiresCoreFieldsAndStrictHomepageMetadata() {
        Map<String, Object> valid = new HashMap<>();
        valid.put("name", "Burger"); valid.put("categoryId", 2); valid.put("basePrice", 49000);
        valid.put("isNew", true); valid.put("spiceLevel", 3);
        AdminProductServlet.validateProductCreate(valid);
        assertThrows(IllegalArgumentException.class, () -> AdminProductServlet.validateProductCreate(Map.of("name", "Burger", "categoryId", 2)));
        assertThrows(IllegalArgumentException.class, () -> AdminProductServlet.validateProductCreate(Map.of("name", "Burger", "categoryId", 2, "basePrice", 1, "isNew", "true")));
        assertThrows(IllegalArgumentException.class, () -> AdminProductServlet.validateProductCreate(Map.of("name", "Burger", "categoryId", 2, "basePrice", 1, "spiceLevel", 4)));
    }


    @Test
    void featuredRouteRejectsExtraSegmentBeforeMutation() throws Exception {
        TestAdminOrderServlet servlet = new TestAdminOrderServlet();
        ResponseCapture capture = invoke(servlet, "/44/featured-review/extra", "{\"featured\":true}");
        assertEquals(404, capture.status);
        assertEquals(0, servlet.mutations);
    }

    @Test
    void featuredRouteReturns422ForIneligibleReview() throws Exception {
        TestAdminOrderServlet servlet = new TestAdminOrderServlet(); servlet.ineligible = true;
        ResponseCapture capture = invoke(servlet, "/44/featured-review", "{\"featured\":true}");
        assertEquals(422, capture.status);
    }

    @Test
    void bestsellerRankingMarksOnlyRankedItems() {
        List<Map<String, Object>> ranked = new ArrayList<>(); ranked.add(new HashMap<>(Map.of("productId", 1, "bestSeller", false)));
        ProductServlet.setBestSeller(ranked, true);
        assertEquals(true, ranked.get(0).get("bestSeller"));
        ProductServlet.setBestSeller(ranked, false);
        assertEquals(false, ranked.get(0).get("bestSeller"));
    }

    private ResponseCapture invoke(jakarta.servlet.http.HttpServlet servlet, String path, String body) throws Exception {
        ResponseCapture capture = new ResponseCapture();
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getPathInfo" -> path;
                    case "getReader" -> new BufferedReader(new StringReader(body));
                    default -> defaultValue(method.getReturnType());
                });
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {HttpServletResponse.class},
                (proxy, method, args) -> { if ("setStatus".equals(method.getName())) capture.status = (int) args[0]; if ("sendError".equals(method.getName())) capture.status = (int) args[0]; if ("getWriter".equals(method.getName())) return capture.writer; return defaultValue(method.getReturnType()); });
        if (servlet instanceof AdminProductServlet product) product.doPut(request, response); else ((AdminOrderServlet) servlet).doPut(request, response);
        capture.writer.flush(); return capture;
    }

    private Object defaultValue(Class<?> type) { if (!type.isPrimitive()) return null; if (type == boolean.class) return false; if (type == char.class) return '\0'; return 0; }

    private static class TestAdminProductServlet extends AdminProductServlet { @Override protected boolean checkAdmin(HttpServletRequest req, HttpServletResponse resp) { return true; } }
    private static class TestAdminOrderServlet extends AdminOrderServlet {
        int mutations; boolean ineligible;
        @Override protected boolean checkAdmin(HttpServletRequest req, HttpServletResponse resp) { return true; }
        @Override protected void updateFeaturedReview(int orderId, Map<String, Object> body, HttpServletResponse resp) { mutations++; if (ineligible) throw new IllegalStateException("Review is not eligible for homepage"); }
    }
    private static class ResponseCapture { int status = 200; final StringWriter body = new StringWriter(); final PrintWriter writer = new PrintWriter(body); }
}
