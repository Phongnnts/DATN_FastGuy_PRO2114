package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import dao.OrderItemDAO;
import dao.OrdersDAO;
import dao.PaymentAttemptDAO;
import entity.Orders;
import entity.PaymentAttempt;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.OrderStatusHistoryService;
import service.ReviewService;
import utils.JsonUtil;

class AdminOrderServletBehaviorTest {
    @Test
    void getDetailSerializesExactAdminEnvelopeAndReviewFields() throws Exception {
        TestAdminOrderServlet servlet = servlet();
        ResponseCapture capture = new ResponseCapture();

        servlet.get(request("/44", ""), response(capture));

        JsonNode body = json(capture);
        JsonNode data = body.path("data");
        assertEquals(200, capture.status);
        assertEquals("success", body.path("status").asText());
        for (String field : List.of("orderId", "orderCode", "status", "customerName", "customerPhone", "customerAddress",
                "totalAmount", "shippingFee", "serviceFee", "finalAmount", "discountAmount", "paymentMethod", "paymentStatus",
                "deliveryNote", "cancelledBy", "failureNote", "failureReason", "deliveryFailureCode", "deliveryAttemptCount", "deliveryAttemptLimit",
                "deliveryFailedAt", "retryScheduledAt", "returnedToStoreAt", "refundStatus", "refundAmount", "refundNote", "refundedAt",
                "createdAt", "confirmedAt", "cancelledAt", "deliveredAt", "staffName", "shipperName", "internalNote", "review",
                "payment", "items", "statusHistory")) assertTrue(data.has(field), field);
        assertTrue(data.path("payment").isNull());
        assertTrue(data.path("items").isArray());
        assertTrue(data.path("statusHistory").isArray());
        JsonNode review = data.path("review");
        for (String field : List.of("reviewId", "rating", "comment", "createdAt", "updatedAt", "userName", "avatarUrl", "orderId", "featured", "homepageConsent", "featureEligible", "featureIneligibilityReason")) assertTrue(review.has(field), field);
        assertTrue(review.path("reviewId").isInt());
        assertTrue(review.path("rating").isInt());
        assertTrue(review.path("comment").isNull());
        assertTrue(review.path("createdAt").isTextual());
        assertTrue(review.path("updatedAt").isNull());
        assertTrue(review.path("userName").isTextual());
        assertTrue(review.path("avatarUrl").isTextual());
        assertTrue(review.path("orderId").isInt());
        assertTrue(review.path("featured").isBoolean());
        assertTrue(review.path("homepageConsent").isBoolean());
        assertTrue(review.path("featureEligible").isBoolean());
        assertTrue(review.path("featureIneligibilityReason").isNull());
    }

    @Test
    void putFeaturedReviewParsesBodyMutatesReviewAndReturnsSuccessEnvelope() throws Exception {
        TestAdminOrderServlet servlet = servlet();
        ResponseCapture capture = new ResponseCapture();

        servlet.put(request("/44/featured-review", "{\"featured\":true}"), response(capture));

        JsonNode body = json(capture);
        assertEquals(200, capture.status);
        assertEquals("success", body.path("status").asText());
        assertEquals("Featured review updated", body.path("message").asText());
        assertEquals(44, servlet.reviewService.orderId);
        assertTrue(servlet.reviewService.featured);
        assertTrue(body.path("data").path("featured").asBoolean());
    }

    private TestAdminOrderServlet servlet() throws Exception {
        Orders order = new Orders();
        order.setOrderId(44);
        order.setOrderCode("FG-0044");
        order.setOrderStatus("DELIVERY_FAILED");
        order.setCustomerName("Nguyễn An");
        order.setCustomerPhone("0900000000");
        order.setCustomerAddress("1 Đường Test");
        order.setTotalAmount(new BigDecimal("100000"));
        order.setShippingFee(new BigDecimal("15000"));
        order.setServiceFee(new BigDecimal("2000"));
        order.setFinalAmount(new BigDecimal("117000"));
        order.setDiscountAmount(new BigDecimal("0"));
        order.setPaymentMethod("COD");
        order.setPaymentStatus("PENDING");
        order.setDeliveryNote("Giao giờ hành chính");
        order.setFailureReason("Không liên lạc được");
        order.setDeliveryFailureCode("CUSTOMER_UNREACHABLE");
        order.setDeliveryAttemptCount(1);
        order.setDeliveryAttemptLimit(2);
        order.setCreatedAt(LocalDateTime.of(2026, 8, 18, 9, 30));
        order.setDeliveryFailedAt(LocalDateTime.of(2026, 8, 18, 10, 0));
        TestAdminOrderServlet servlet = new TestAdminOrderServlet();
        servlet.reviewService = new StubReviewService();
        set(servlet, "ordersDAO", new StubOrdersDAO(order));
        set(servlet, "orderItemDAO", new StubOrderItemDAO());
        set(servlet, "paymentAttemptDAO", new StubPaymentAttemptDAO());
        set(servlet, "historyService", new StubHistoryService());
        set(servlet, "reviewService", servlet.reviewService);
        return servlet;
    }

    private HttpServletRequest request(String path, String body) {
        return (HttpServletRequest) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "getPathInfo" -> path;
                    case "getReader" -> new BufferedReader(new StringReader(body));
                    default -> defaultValue(method.getReturnType());
                });
    }

    private HttpServletResponse response(ResponseCapture capture) {
        return (HttpServletResponse) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { HttpServletResponse.class }, (proxy, method, args) -> {
                    if ("setStatus".equals(method.getName())) capture.status = (int) args[0];
                    if ("getWriter".equals(method.getName())) return capture.writer;
                    return defaultValue(method.getReturnType());
                });
    }

    private JsonNode json(ResponseCapture capture) throws Exception {
        capture.writer.flush();
        return JsonUtil.getMapper().readTree(capture.body.toString());
    }

    private void set(Object target, String name, Object value) throws Exception {
        Field field = AdminOrderServlet.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        return 0;
    }

    private static class TestAdminOrderServlet extends AdminOrderServlet {
        private StubReviewService reviewService;
        @Override protected boolean checkAdmin(HttpServletRequest req, HttpServletResponse resp) { return true; }
        private void get(HttpServletRequest req, HttpServletResponse resp) throws Exception { doGet(req, resp); }
        private void put(HttpServletRequest req, HttpServletResponse resp) throws Exception { doPut(req, resp); }
    }

    private static class StubOrdersDAO extends OrdersDAO {
        private final Orders order;
        private StubOrdersDAO(Orders order) { this.order = order; }
        @Override public Orders findById(int orderId) { return orderId == 44 ? order : null; }
    }

    private static class StubOrderItemDAO extends OrderItemDAO {
        @Override public List<entity.OrderItem> findByOrderId(int orderId) { return List.of(); }
    }

    private static class StubPaymentAttemptDAO extends PaymentAttemptDAO {
        @Override public PaymentAttempt findByOrderId(int orderId) { return null; }
    }

    private static class StubHistoryService extends OrderStatusHistoryService {
        @Override public List<Map<String, Object>> getByOrderId(int orderId) { return List.of(); }
    }

    private static class StubReviewService extends ReviewService {
        private int orderId;
        private boolean featured;
        @Override public Map<String, Object> getAdminByOrderId(int orderId) {
            Map<String, Object> review = new java.util.HashMap<>();
            review.put("reviewId", 7); review.put("rating", 5); review.put("comment", null);
            review.put("createdAt", LocalDateTime.of(2026, 8, 18, 11, 0)); review.put("updatedAt", null);
            review.put("userName", "Nguyễn An"); review.put("avatarUrl", ""); review.put("orderId", orderId); review.put("featured", false); review.put("homepageConsent", true);
            review.put("featureEligible", true); review.put("featureIneligibilityReason", null);
            return review;
        }
        @Override public Map<String, Object> setFeaturedByOrderId(int orderId, boolean featured) {
            this.orderId = orderId;
            this.featured = featured;
            Map<String, Object> review = new java.util.HashMap<>(getAdminByOrderId(orderId));
            review.put("comment", "Ngon"); review.put("featured", featured);
            return review;
        }
    }

    private static class ResponseCapture {
        private int status = 200;
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body);
    }
}
