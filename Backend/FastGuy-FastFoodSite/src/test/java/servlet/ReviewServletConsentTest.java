package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.ReviewService;
import utils.JwtUtil;

class ReviewServletConsentTest {
    @Test
    void missingConsentDefaultsFalseAndExplicitBooleanPersists() throws Exception {
        RecordingReviewService service = new RecordingReviewService();
        ReviewServlet servlet = new ReviewServlet();
        Field field = ReviewServlet.class.getDeclaredField("reviewService");
        field.setAccessible(true);
        field.set(servlet, service);

        assertEquals(200, post(servlet, "{\"orderId\":7,\"rating\":5,\"comment\":\"Ngon\"}"));
        assertEquals(false, service.homepageConsent);
        assertEquals(200, post(servlet, "{\"orderId\":7,\"rating\":5,\"homepageConsent\":true}"));
        assertEquals(true, service.homepageConsent);
    }

    @Test
    void nonBooleanConsentReturns400BeforeServiceCall() throws Exception {
        RecordingReviewService service = new RecordingReviewService();
        ReviewServlet servlet = new ReviewServlet();
        Field field = ReviewServlet.class.getDeclaredField("reviewService");
        field.setAccessible(true);
        field.set(servlet, service);

        assertEquals(400, post(servlet, "{\"orderId\":7,\"rating\":5,\"homepageConsent\":\"true\"}"));
        assertEquals(0, service.calls);
    }

    private int post(ReviewServlet servlet, String body) throws Exception {
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getHeader" -> "Bearer " + JwtUtil.generate(3, "USER");
                    case "getReader" -> new BufferedReader(new StringReader(body));
                    default -> defaultValue(method.getReturnType());
                });
        Capture capture = new Capture();
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {HttpServletResponse.class},
                (proxy, method, args) -> {
                    if ("setStatus".equals(method.getName())) capture.status = (int) args[0];
                    if ("getWriter".equals(method.getName())) return capture.writer;
                    return defaultValue(method.getReturnType());
                });
        servlet.doPost(request, response);
        return capture.status;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        return 0;
    }

    private static class RecordingReviewService extends ReviewService {
        private int calls;
        private boolean homepageConsent;
        @Override public Map<String, Object> create(int userId, int orderId, int rating, String comment, boolean homepageConsent) {
            calls++;
            this.homepageConsent = homepageConsent;
            return Map.of("reviewId", 1);
        }
    }

    private static class Capture {
        private int status = 200;
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body);
    }
}
