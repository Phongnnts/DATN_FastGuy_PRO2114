package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import entity.Orders;
import dao.OrderItemDAO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.StaffOrderService;
import utils.JsonUtil;

class StaffOrderServletBehaviorTest {
    @Test
    void rejectsMissingAndUnknownDispatchFilters() throws Exception {
        TestStaffOrderServlet servlet = servlet();
        ResponseCapture missing = new ResponseCapture();
        ResponseCapture unknown = new ResponseCapture();

        servlet.get(request(null), response(missing));
        servlet.get(request("OTHER"), response(unknown));

        assertEquals(400, missing.status);
        assertEquals(400, unknown.status);
        assertEquals(0, servlet.service.calls);
    }

    @Test
    void serializesExactDispatchContractFields() throws Exception {
        TestStaffOrderServlet servlet = servlet();
        ResponseCapture capture = new ResponseCapture();

        servlet.get(request("PRIORITY"), response(capture));

        JsonNode body = JsonUtil.getMapper().readTree(capture.body.toString());
        assertEquals(200, capture.status);
        List<String> fields = new java.util.ArrayList<>();
        body.fieldNames().forEachRemaining(fields::add);
        assertEquals(List.of("items", "counts", "serverTime", "openTime", "closeTime"), fields);
        assertEquals("2026-08-25T21:30:00", body.path("serverTime").asText());
        assertEquals("08:00", body.path("openTime").asText());
        assertEquals("22:00", body.path("closeTime").asText());
        assertEquals(2, body.path("counts").path("priority").asInt());
        assertEquals(1, body.path("counts").path("new").asInt());
        assertEquals(1, body.path("counts").path("review").asInt());
        JsonNode item = body.path("items").get(0);
        for (String field : List.of("customerAddress", "readyAt", "classification", "minutesUntilClose",
                "deliveryAttemptCount", "deliveryAttemptLimit", "deliveryFailureCode", "failureNote",
                "deliveryFailedAt", "retryScheduledAt", "returnedToStoreAt")) assertTrue(item.has(field), field);
        assertEquals("PRIORITY", item.path("classification").asText());
        assertEquals("2026-08-25T21:10:00", item.path("readyAt").asText());
        assertEquals(30, item.path("minutesUntilClose").asLong());
        assertFalse(item.has("data"));
    }

    @Test
    void dispatchStillRequiresCheckedInShift() {
        assertTrue(StaffOrderServlet.requiresCheckedInShift("GET", "/dispatch"));
        assertFalse(StaffOrderServlet.hasRouteAccess("GET", "/dispatch", true, false));
    }

    private TestStaffOrderServlet servlet() throws Exception {
        TestStaffOrderServlet servlet = new TestStaffOrderServlet();
        servlet.service = new StubStaffOrderService();
        set(servlet, "staffOrderService", servlet.service);
        set(servlet, "orderItemDAO", new OrderItemDAO() {
            @Override public List<entity.OrderItem> findByOrderId(int orderId) { return List.of(); }
        });
        return servlet;
    }

    private HttpServletRequest request(String filter) {
        return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { HttpServletRequest.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getPathInfo" -> "/dispatch";
                    case "getParameter" -> "filter".equals(args[0]) ? filter : null;
                    case "getMethod" -> "GET";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private HttpServletResponse response(ResponseCapture capture) {
        return (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { HttpServletResponse.class },
                (proxy, method, args) -> {
                    if ("setStatus".equals(method.getName())) capture.status = (int) args[0];
                    if ("getWriter".equals(method.getName())) return capture.writer;
                    return defaultValue(method.getReturnType());
                });
    }

    private void set(Object target, String name, Object value) throws Exception {
        Field field = StaffOrderServlet.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        return 0;
    }

    private static class TestStaffOrderServlet extends StaffOrderServlet {
        private StubStaffOrderService service;
        @Override protected int getStaffId(HttpServletRequest req, HttpServletResponse resp) { return 7; }
        @Override protected boolean requireCheckedInShift(HttpServletRequest req, HttpServletResponse resp, int staffId) { return true; }
        private void get(HttpServletRequest req, HttpServletResponse resp) throws Exception { doGet(req, resp); }
    }

    private static class StubStaffOrderService extends StaffOrderService {
        private int calls;
        @Override public DispatchResult getDispatchOrders(String filter) {
            calls++;
            Orders order = new Orders();
            order.setOrderId(11);
            order.setOrderCode("FG-0011");
            order.setOrderStatus("READY");
            order.setCustomerAddress("11 Test Street");
            order.setReadyAt(LocalDateTime.of(2026, 8, 25, 21, 10));
            return new DispatchResult(List.of(new DispatchItem(order, "PRIORITY", 30L)),
                    Map.of("priority", 2L, "new", 1L, "review", 1L),
                    LocalDateTime.of(2026, 8, 25, 21, 30), LocalTime.of(8, 0), LocalTime.of(22, 0));
        }
    }

    private static class ResponseCapture {
        private int status = 200;
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body);
    }
}
