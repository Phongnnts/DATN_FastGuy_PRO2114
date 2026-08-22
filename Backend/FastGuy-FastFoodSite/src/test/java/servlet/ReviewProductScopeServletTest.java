package servlet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dao.OrdersDAO;
import dao.ProductDAO;
import dao.ReviewDAO;
import entity.Orders;
import entity.Product;
import entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.ReviewService;
import utils.JwtUtil;

class ReviewProductScopeServletTest {
    @Test
    void authenticatedOrderGetReturnsGroupedCurrentUserReviews() throws Exception {
        RecordingReviewService service = new RecordingReviewService();
        service.orderResult = Map.of("orderId", 44, "reviews", List.of(Map.of("productId", 11)));
        ReviewServlet servlet = servlet(service, ownedOrder(7), product(11));

        Capture response = get(servlet, "/order/44", Map.of(), token(7));

        assertEquals(200, response.status);
        assertEquals(7, service.userId);
        assertEquals(44, service.orderId);
        assertTrue(response.body().contains("\"reviews\""));
    }

    @Test
    void orderGetRequiresAuthenticationAndHidesMissingOrOtherOwners() throws Exception {
        ReviewServlet missing = servlet(new RecordingReviewService(), null, product(11));
        ReviewServlet otherOwner = servlet(new RecordingReviewService(), ownedOrder(8), product(11));

        Capture unauthorized = get(missing, "/order/44", Map.of(), null);
        Capture absent = get(missing, "/order/44", Map.of(), token(7));
        Capture forbidden = get(otherOwner, "/order/44", Map.of(), token(7));

        assertEquals(401, unauthorized.status);
        assertEquals(404, absent.status);
        assertEquals(404, forbidden.status);
        assertEquals(absent.body(), forbidden.body());
    }

    @Test
    void publicProductGetUsesDefaultsValidatesPaginationAndExactServiceAllowlist() throws Exception {
        RecordingReviewService service = new RecordingReviewService();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("reviewId", 1);
        item.put("productId", 11);
        item.put("rating", 5);
        item.put("comment", "Ngon");
        item.put("userName", "An");
        item.put("createdAt", LocalDateTime.of(2026, 8, 22, 10, 0));
        service.productResult = Map.of("items", List.of(item), "total", 1, "page", 1, "size", 10,
                "averageRating", 5.0, "reviewCount", 1,
                "ratingDistribution", Map.of("1", 0, "2", 0, "3", 0, "4", 0, "5", 1));
        ReviewServlet servlet = servlet(service, ownedOrder(7), product(11));

        Capture response = get(servlet, "/product/11", Map.of(), null);

        assertEquals(200, response.status);
        assertEquals(11, service.productId);
        assertEquals(1, service.page);
        assertEquals(10, service.size);
        assertFalse(response.body().contains("orderId"));
        assertFalse(response.body().contains("avatarUrl"));
        assertFalse(response.body().contains("homepageConsent"));
        assertFalse(response.body().contains("featured"));
        assertFalse(response.body().contains("updatedAt"));
        assertEquals(400, get(servlet, "/product/x", Map.of(), null).status);
        assertEquals(400, get(servlet, "/product/11", Map.of("page", "0"), null).status);
        assertEquals(400, get(servlet, "/product/11", Map.of("size", "51"), null).status);
        assertEquals(400, get(servlet, "/product/11", Map.of("page", "1.5"), null).status);
        assertEquals(404, get(servlet(service, ownedOrder(7), null), "/product/11", Map.of(), null).status);
    }

    @Test
    void presentBlankPaginationIsMalformedAndExtraRouteSuffixIsNotFound() throws Exception {
        ReviewServlet servlet = servlet(new RecordingReviewService(), ownedOrder(7), product(11));

        assertEquals(400, get(servlet, "/product/11", Map.of("page", " "), null).status);
        assertEquals(400, get(servlet, "/product/11", Map.of("size", ""), null).status);
        assertEquals(404, get(servlet, "/product/11/extra", Map.of(), null).status);
        assertEquals(404, get(servlet, "/order/44/extra", Map.of(), token(7)).status);
    }

    @Test
    void getUnexpectedServiceAndDaoFailuresReturnGeneric500WithoutLeakage() throws Exception {
        RecordingReviewService service = new RecordingReviewService();
        service.getFailure = new IllegalStateException("SECRET_SERVICE_DETAIL");
        ReviewServlet serviceFailure = servlet(service, ownedOrder(7), product(11));
        Capture productService = assertDoesNotThrow(() -> get(serviceFailure, "/product/11", Map.of(), null));
        Capture orderService = assertDoesNotThrow(() -> get(serviceFailure, "/order/44", Map.of(), token(7)));

        ReviewServlet productDaoFailure = servlet(new RecordingReviewService(), ownedOrder(7), product(11));
        set(productDaoFailure, "productDAO", new ProductDAO() {
            @Override public Product findById(int id) { throw new IllegalStateException("SECRET_PRODUCT_DAO_DETAIL"); }
        });
        Capture productDao = assertDoesNotThrow(() -> get(productDaoFailure, "/product/11", Map.of(), null));

        ReviewServlet orderDaoFailure = servlet(new RecordingReviewService(), ownedOrder(7), product(11));
        set(orderDaoFailure, "ordersDAO", new OrdersDAO() {
            @Override public Orders findById(int id) { throw new IllegalStateException("SECRET_ORDER_DAO_DETAIL"); }
        });
        Capture orderDao = assertDoesNotThrow(() -> get(orderDaoFailure, "/order/44", Map.of(), token(7)));

        for (Capture response : List.of(productService, orderService, productDao, orderDao)) {
            assertEquals(500, response.status);
            assertEquals(response.body(), productService.body());
            assertTrue(response.body().contains("Review failed"));
            assertFalse(response.body().contains("SECRET_"));
        }
    }

    @Test
    void postRequiresIntegralIdsAndRatingWithOptionalComment() throws Exception {
        RecordingReviewService service = new RecordingReviewService();
        ReviewServlet servlet = servlet(service, ownedOrder(7), product(11));

        assertEquals(200, post(servlet, "{\"orderId\":44,\"productId\":11,\"rating\":5,\"comment\":null}", token(7)).status);
        assertEquals(List.of(7, 44, 11, 5, false), service.created);
        assertEquals(400, post(servlet, "{\"orderId\":44.5,\"productId\":11,\"rating\":5}", token(7)).status);
        assertEquals(400, post(servlet, "{\"orderId\":44,\"productId\":11.5,\"rating\":5}", token(7)).status);
        assertEquals(400, post(servlet, "{\"orderId\":44,\"productId\":11,\"rating\":4.5}", token(7)).status);
        assertEquals(400, post(servlet, "{\"orderId\":44,\"productId\":11,\"rating\":5,\"comment\":7}", token(7)).status);
        assertEquals(401, post(servlet, "{\"orderId\":44,\"productId\":11,\"rating\":5}", null).status);
    }

    @Test
    void postMapsOwnershipEligibilityAndDuplicatePerContract() throws Exception {
        RecordingReviewService service = new RecordingReviewService();
        ReviewServlet missing = servlet(service, null, product(11));
        ReviewServlet otherOwner = servlet(service, ownedOrder(8), product(11));
        ReviewServlet eligible = servlet(service, ownedOrder(7), product(11));

        Capture absent = post(missing, "{\"orderId\":44,\"productId\":11,\"rating\":5}", token(7));
        Capture forbidden = post(otherOwner, "{\"orderId\":44,\"productId\":11,\"rating\":5}", token(7));
        assertEquals(404, absent.status);
        assertEquals(404, forbidden.status);
        assertEquals(absent.body(), forbidden.body());

        service.failure = new IllegalArgumentException("NOT_DELIVERED_OR_NOT_PURCHASED");
        assertEquals(400, post(eligible, "{\"orderId\":44,\"productId\":11,\"rating\":5}", token(7)).status);
        service.failure = new ReviewDAO.AlreadyReviewedException();
        assertEquals(409, post(eligible, "{\"orderId\":44,\"productId\":11,\"rating\":5}", token(7)).status);

        service.failure = new IllegalStateException("SECRET_RUNTIME_DETAIL");
        Capture unexpected = post(eligible, "{\"orderId\":44,\"productId\":11,\"rating\":5}", token(7));
        assertEquals(500, unexpected.status);
        assertTrue(unexpected.body().contains("Review failed"));
        assertFalse(unexpected.body().contains("SECRET_RUNTIME_DETAIL"));
    }

    private ReviewServlet servlet(RecordingReviewService service, Orders order, Product product) throws Exception {
        ReviewServlet servlet = new ReviewServlet();
        set(servlet, "reviewService", service);
        set(servlet, "ordersDAO", new OrdersDAO() { @Override public Orders findById(int id) { return order; } });
        set(servlet, "productDAO", new ProductDAO() { @Override public Product findById(int id) { return product; } });
        return servlet;
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Capture get(ReviewServlet servlet, String path, Map<String, String> parameters, String authorization) throws Exception {
        return invoke(servlet, path, parameters, authorization, null, false);
    }

    private Capture post(ReviewServlet servlet, String body, String authorization) throws Exception {
        return invoke(servlet, null, Map.of(), authorization, body, true);
    }

    private Capture invoke(ReviewServlet servlet, String path, Map<String, String> parameters, String authorization,
            String body, boolean post) throws Exception {
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {HttpServletRequest.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getPathInfo" -> path;
                    case "getParameter" -> parameters.get((String) args[0]);
                    case "getHeader" -> authorization;
                    case "getReader" -> new BufferedReader(new StringReader(body));
                    default -> defaultValue(method.getReturnType());
                });
        Capture capture = new Capture();
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {HttpServletResponse.class}, (proxy, method, args) -> {
                    if ("setStatus".equals(method.getName())) capture.status = (int) args[0];
                    if ("getWriter".equals(method.getName())) return capture.writer;
                    return defaultValue(method.getReturnType());
                });
        if (post) servlet.doPost(request, response); else servlet.doGet(request, response);
        capture.writer.flush();
        return capture;
    }

    private static Orders ownedOrder(int userId) {
        User user = new User();
        user.setUserId(userId);
        Orders order = new Orders();
        order.setOrderId(44);
        order.setUser(user);
        return order;
    }

    private static Product product(int productId) {
        Product product = new Product();
        product.setProductId(productId);
        return product;
    }

    private static String token(int userId) { return "Bearer " + JwtUtil.generate(userId, "USER"); }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        return 0;
    }

    private static class RecordingReviewService extends ReviewService {
        private Map<String, Object> orderResult = Map.of("orderId", 44, "reviews", List.of());
        private Map<String, Object> productResult = Map.of("items", List.of());
        private RuntimeException failure;
        private RuntimeException getFailure;
        private int userId;
        private int orderId;
        private int productId;
        private int page;
        private int size;
        private List<Object> created;

        @Override public Map<String, Object> getByOrderId(int userId, int orderId) {
            if (getFailure != null) throw getFailure;
            this.userId = userId;
            this.orderId = orderId;
            return orderResult;
        }

        @Override public Map<String, Object> getByProductId(int productId, int page, int size) {
            if (getFailure != null) throw getFailure;
            this.productId = productId;
            this.page = page;
            this.size = size;
            return productResult;
        }

        @Override public Map<String, Object> create(int userId, int orderId, int productId, int rating, String comment,
                boolean homepageConsent) {
            if (failure != null) throw failure;
            created = List.of(userId, orderId, productId, rating, homepageConsent);
            return Map.of("reviewId", 1, "productId", productId, "rating", rating);
        }
    }

    private static class Capture {
        private int status = 200;
        private final StringWriter output = new StringWriter();
        private final PrintWriter writer = new PrintWriter(output);
        private String body() { return output.toString(); }
    }
}
