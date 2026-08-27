package servlet;

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
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import entity.Orders;
import dao.OrderItemDAO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.StaffOrderService;
import service.StaffShiftAccessService;
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
        Set<String> expectedKeys = Set.of("orderId", "orderCode", "userId", "customerName", "customerPhone",
                "customerAddress", "status", "orderStatus", "itemCount", "items", "totalAmount", "shippingFee",
                "serviceFee", "discountAmount", "paymentMethod", "paymentStatus", "finalAmount", "refundAmount",
                "refundedAt", "shipperId", "shipperName", "assignedAt", "updatedAt", "endedAt", "createdAt",
                "deliveryAttemptCount", "deliveryAttemptLimit", "deliveryFailureCode", "failureNote",
                "deliveryFailedAt", "retryScheduledAt", "returnedToStoreAt", "readyAt", "classification",
                "minutesUntilClose", "staffShiftId", "ownerShiftLabel", "handoverRequired");
        Set<String> actualKeys = new java.util.HashSet<>();
        item.fieldNames().forEachRemaining(actualKeys::add);
        assertEquals(expectedKeys, actualKeys);
        assertEquals("PRIORITY", item.path("classification").asText());
        assertEquals("2026-08-25T21:10:00", item.path("readyAt").asText());
        assertEquals(30, item.path("minutesUntilClose").asLong());
        for (String field : List.of("userId", "customerName", "customerPhone", "totalAmount", "shippingFee",
                "serviceFee", "discountAmount", "paymentMethod", "paymentStatus", "finalAmount", "refundAmount",
                "refundedAt", "shipperId", "shipperName", "assignedAt", "updatedAt", "endedAt", "createdAt",
                "deliveryFailureCode", "failureNote", "deliveryFailedAt", "retryScheduledAt", "returnedToStoreAt"))
            assertTrue(item.path(field).isNull(), field);
        for (String field : List.of("orderId", "itemCount", "deliveryAttemptCount", "deliveryAttemptLimit", "minutesUntilClose"))
            assertTrue(item.path(field).isIntegralNumber(), field);
        for (String field : List.of("orderCode", "customerAddress", "status", "orderStatus", "readyAt", "classification"))
            assertTrue(item.path(field).isTextual(), field);
        assertTrue(item.path("items").isArray());
    }

    @Test
    void dispatchStillRequiresCheckedInShift() {
        assertTrue(StaffOrderServlet.requiresCheckedInShift("GET", "/dispatch"));
        assertFalse(StaffOrderServlet.hasRouteAccess("GET", "/dispatch", true, false));
    }

    @Test
    void uncheckedStaffRequestReturnsForbiddenWithoutDispatchLookup() throws Exception {
        TestStaffOrderServlet servlet = servlet();
        servlet.access.checkedIn = false;
        ResponseCapture capture = new ResponseCapture();

        servlet.get(request("PRIORITY"), response(capture));

        assertEquals(403, capture.status);
        assertEquals(0, servlet.service.calls);
    }

    @Test
    void invalidBusinessConfigReturnsBadRequest() throws Exception {
        TestStaffOrderServlet servlet = servlet();
        servlet.service.configFailure = true;
        ResponseCapture capture = new ResponseCapture();

        servlet.get(request("PRIORITY"), response(capture));

        assertEquals(400, capture.status);
        assertEquals(1, servlet.service.calls);
    }

    @Test
    void ownershipCountGetSerializesCurrentShiftCount() throws Exception {
        TestStaffOrderServlet servlet = servlet();
        ResponseCapture capture = new ResponseCapture();

        servlet.get(request("GET", "/ownership-count", null), response(capture));

        JsonNode body = JsonUtil.getMapper().readTree(capture.body.toString());
        assertEquals(200, capture.status);
        assertEquals("success", body.path("status").asText());
        assertEquals(3, body.path("data").path("activeOwnershipCount").asLong());
        assertEquals(7, servlet.service.ownershipStaffId);
    }

    @Test
    void handoverGetSerializesOwnershipContract() throws Exception {
        TestStaffOrderServlet servlet = servlet();
        ResponseCapture capture = new ResponseCapture();
        servlet.get(request("GET", "/handover", null), response(capture));
        JsonNode item = JsonUtil.getMapper().readTree(capture.body.toString()).path("data").get(0);
        for (String field : List.of("orderId", "orderCode", "status", "customerName", "itemCount", "waitingSince", "staffShiftId", "ownerShiftLabel", "handoverRequired")) assertTrue(item.has(field), field);
        assertTrue(item.path("staffShiftId").isNull());
        assertTrue(item.path("handoverRequired").asBoolean());
    }

    @Test
    void handoverPayloadRejectsUnknownFractionalAndNonpositiveOwner() {
        assertTrue(StaffOrderServlet.validateHandoverPayload(Map.of("expectedStatus", "READY", "expectedOwnerShiftId", 2, "extra", true)) == null);
        assertTrue(StaffOrderServlet.validateHandoverPayload(Map.of("expectedStatus", "READY", "expectedOwnerShiftId", 2.5)) == null);
        assertTrue(StaffOrderServlet.validateHandoverPayload(Map.of("expectedStatus", "READY", "expectedOwnerShiftId", 0)) == null);
    }

    @Test
    void handoverPutPassesExpectedNullableOwner() throws Exception {
        TestStaffOrderServlet servlet = servlet();
        ResponseCapture capture = new ResponseCapture();
        servlet.put(request("PUT", "/11/handover", "{\"expectedStatus\":\"PREPARING\",\"expectedOwnerShiftId\":null}"), response(capture));
        assertEquals(200, capture.status);
        assertEquals("Handover claimed", JsonUtil.getMapper().readTree(capture.body.toString()).path("message").asText());
        assertEquals("PREPARING", servlet.service.expectedStatus);
        assertTrue(servlet.service.expectedOwnerShiftId == null);
    }

    @Test
    void preservesRequiredNullableDispatchFieldsAsJsonNull() throws Exception {
        TestStaffOrderServlet servlet = servlet();
        servlet.service.nullableOrder = true;
        ResponseCapture capture = new ResponseCapture();

        servlet.get(request("PRIORITY"), response(capture));

        JsonNode item = JsonUtil.getMapper().readTree(capture.body.toString()).path("items").get(0);
        for (String field : List.of("orderCode", "customerName", "customerPhone", "customerAddress",
                "status", "orderStatus", "readyAt")) {
            assertTrue(item.has(field), field);
            assertTrue(item.path(field).isNull(), field);
        }
    }

    private TestStaffOrderServlet servlet() throws Exception {
        TestStaffOrderServlet servlet = new TestStaffOrderServlet();
        servlet.service = new StubStaffOrderService();
        set(servlet, "staffOrderService", servlet.service);
        set(servlet, "orderItemDAO", new OrderItemDAO() {
            @Override public List<entity.OrderItem> findByOrderId(int orderId) { return List.of(); }
            @Override public Map<Integer, Integer> countItemsByOrderIds(List<Integer> orderIds) { return Map.of(11, 0); }
        });
        servlet.access = new StubStaffShiftAccessService();
        set(servlet, "staffShiftAccessService", servlet.access);
        return servlet;
    }

    private HttpServletRequest request(String filter) {
        return request("GET", "/dispatch", null, filter);
    }

    private HttpServletRequest request(String methodName, String path, String body) {
        return request(methodName, path, body, null);
    }

    private HttpServletRequest request(String methodName, String path, String body, String filter) {
        return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { HttpServletRequest.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getPathInfo" -> path;
                    case "getParameter" -> "filter".equals(args[0]) ? filter : null;
                    case "getMethod" -> methodName;
                    case "getReader" -> new BufferedReader(new StringReader(body == null ? "" : body));
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
        private StubStaffShiftAccessService access;
        @Override protected int getStaffId(HttpServletRequest req, HttpServletResponse resp) { return 7; }
        private void get(HttpServletRequest req, HttpServletResponse resp) throws Exception { doGet(req, resp); }
        private void put(HttpServletRequest req, HttpServletResponse resp) throws Exception { doPut(req, resp); }
    }

    private static class StubStaffShiftAccessService extends StaffShiftAccessService {
        private boolean checkedIn = true;
        @Override public boolean hasValidStaffIdentity(int userId) { return true; }
        @Override public boolean hasCheckedInShift(int userId) { return checkedIn; }
    }

    private static class StubStaffOrderService extends StaffOrderService {
        private int calls;
        private boolean configFailure;
        private boolean nullableOrder;
        private String expectedStatus;
        private Integer expectedOwnerShiftId;
        private int ownershipStaffId;
        @Override public long getActiveOwnershipCount(int staffId) { ownershipStaffId = staffId; return 3; }
        @Override public List<Orders> getHandoverOrders(int staffId) {
            Orders order = new Orders(); order.setOrderId(11); order.setOrderCode("FG-0011"); order.setOrderStatus("PREPARING");
            order.setCustomerName("Test"); order.setCreatedAt(LocalDateTime.of(2026, 8, 25, 20, 0));
            return List.of(order);
        }
        @Override public service.OrderTransitionService.MutationResult claimHandover(int orderId, int staffId, String expectedStatus, Integer expectedOwnerShiftId) {
            this.expectedStatus = expectedStatus; this.expectedOwnerShiftId = expectedOwnerShiftId;
            return service.OrderTransitionService.MutationResult.SUCCESS;
        }
        @Override public DispatchResult getDispatchOrders(String filter) {
            calls++;
            if (configFailure) throw new IllegalArgumentException("Missing business hours config");
            Orders order = new Orders();
            order.setOrderId(11);
            if (!nullableOrder) {
                order.setOrderCode("FG-0011");
                order.setOrderStatus("READY");
                order.setCustomerAddress("11 Test Street");
                order.setReadyAt(LocalDateTime.of(2026, 8, 25, 21, 10));
            }
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
