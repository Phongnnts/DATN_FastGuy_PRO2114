package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AdminService;
import utils.JsonUtil;

class AdminDashboardServletContractTest {
    private static final Set<String> DASHBOARD_KEYS = Set.of(
            "netCashRevenueToday", "activeOrderCount", "pendingRefundCount", "pendingCodCount", "lowStockItemCount", "staffingGapCount",
            "activeOrdersByStatus", "operationalOrderCountToday", "operationalCompletedCountToday", "completionRateToday", "attentionItems", "sectionAvailability",
            "customerCount", "totalUsers", "totalOrders", "activeProductCount", "totalProducts", "ordersByStatus", "operationalOrderCount",
            "operationalCompletedCount", "completionRate", "totalRevenue", "pendingOrders", "revenueToday", "ordersToday", "pendingCodAmount",
            "revenueByMonth", "topProducts", "lowStockThreshold", "outOfStockSkuCount", "lowStockSkuCount", "deliveredOrdersToday",
            "activeOrdersToday", "aovToday", "grossProfitToday", "costComplete");
    private static final Set<String> SECTION_KEYS = Set.of("financial", "orders", "refunds", "cod", "inventory", "staffing");

    @Test
    void dashboardRequiresActiveAdmin() throws Exception {
        AdminServlet servlet = new AdminServlet(new StubAdminService(dashboard()), new StubTokenReader());

        assertError(invoke(servlet, "/dashboard", null, Map.of()), 401, "Missing or invalid token");
        assertError(invoke(servlet, "/dashboard", "Bearer malformed", Map.of()), 401, "Missing or invalid token");
        assertError(invoke(servlet, "/dashboard", "Bearer user", Map.of()), 403, "Forbidden");
        assertError(invoke(servlet, "/dashboard", "Bearer inactive", Map.of()), 403, "Forbidden");
    }

    @Test
    void dashboardSerializesExactClosedContractAndPreservesNulls() throws Exception {
        ResponseCapture capture = invoke(new AdminServlet(new StubAdminService(dashboard()), new StubTokenReader()), "/dashboard", "Bearer admin", Map.of());
        JsonNode root = json(capture);
        JsonNode data = root.path("data");

        assertEquals(200, capture.status);
        assertEquals(Set.of("status", "data"), fields(root));
        assertEquals("success", root.path("status").asText());
        assertEquals(DASHBOARD_KEYS, fields(data));
        for (String key : List.of("activeOrderCount", "pendingRefundCount", "pendingCodCount", "lowStockItemCount", "staffingGapCount",
                "operationalOrderCountToday", "operationalCompletedCountToday", "totalOrders", "operationalOrderCount", "operationalCompletedCount",
                "pendingOrders", "ordersToday", "outOfStockSkuCount", "lowStockSkuCount", "deliveredOrdersToday", "activeOrdersToday")) {
            assertTrue(data.path(key).isIntegralNumber(), key);
        }
        for (String key : List.of("netCashRevenueToday", "completionRateToday", "completionRate", "totalRevenue", "revenueToday", "pendingCodAmount", "aovToday")) {
            assertTrue(data.path(key).isNumber(), key);
        }
        for (String key : List.of("customerCount", "totalUsers", "activeProductCount", "totalProducts", "lowStockThreshold", "grossProfitToday")) {
            assertTrue(data.get(key).isNull(), key);
        }
        assertTrue(data.path("activeOrdersByStatus").isObject());
        assertTrue(data.path("ordersByStatus").isObject());
        assertTrue(data.path("attentionItems").isArray());
        assertTrue(data.path("revenueByMonth").isArray());
        assertTrue(data.path("topProducts").isArray());
        assertTrue(data.path("costComplete").isBoolean());
        assertEquals(new BigDecimal("275.20"), data.path("netCashRevenueToday").decimalValue());
        assertEquals(new BigDecimal("1250.50"), data.path("totalRevenue").decimalValue());
        assertEquals(Set.of("type", "severity", "count"), fields(data.path("attentionItems").get(0)));
        assertEquals("OVERDUE_PENDING_ORDERS", data.path("attentionItems").get(0).path("type").asText());
        assertEquals("WARNING", data.path("attentionItems").get(0).path("severity").asText());
        assertEquals(2, data.path("attentionItems").get(0).path("count").asInt());
        assertAvailability(data.path("sectionAvailability"), Map.of());
    }

    @Test
    void dashboardReturnsAvailableDataWhenOneSectionIsUnavailable() throws Exception {
        Map<String, Object> dashboard = dashboard();
        dashboard.put("sectionAvailability", availability(Map.of("inventory", "UNAVAILABLE")));

        ResponseCapture capture = invoke(new AdminServlet(new StubAdminService(dashboard), new StubTokenReader()), "/dashboard", "Bearer admin", Map.of());
        JsonNode root = json(capture);

        assertEquals(200, capture.status);
        assertEquals("success", root.path("status").asText());
        assertEquals(DASHBOARD_KEYS, fields(root.path("data")));
        assertAvailability(root.path("data").path("sectionAvailability"), Map.of("inventory", "UNAVAILABLE"));
    }

    @Test
    void dashboardMapsUnexpectedServiceFailureToGeneric500() throws Exception {
        StubAdminService service = new StubAdminService(dashboard());
        service.dashboardFailure = new IllegalStateException("database password leaked");

        ResponseCapture capture = invoke(new AdminServlet(service, new StubTokenReader()), "/dashboard", "Bearer admin", Map.of());
        JsonNode root = json(capture);

        assertEquals(500, capture.status);
        assertEquals(Set.of("status", "message"), fields(root));
        assertEquals("error", root.path("status").asText());
        assertEquals("Internal server error", root.path("message").asText());
        assertFalse(capture.body.toString().contains("database password leaked"));
    }

    @Test
    void fullReportRetainsSuccessAndValidationBehavior() throws Exception {
        StubAdminService service = new StubAdminService(dashboard());
        AdminServlet servlet = new AdminServlet(service, new StubTokenReader());
        Map<String, String> parameters = Map.of("period", "30d", "startDate", "2026-08-01", "endDate", "2026-08-30");

        ResponseCapture success = invoke(servlet, "/reports/full", "Bearer admin", parameters);
        JsonNode root = json(success);
        assertEquals(200, success.status);
        assertEquals(Set.of("status", "data"), fields(root));
        assertEquals("success", root.path("status").asText());
        assertEquals("30d", service.period);
        assertEquals("2026-08-01", service.startDate);
        assertEquals("2026-08-30", service.endDate);

        service.reportFailure = new IllegalArgumentException("Kỳ báo cáo không hợp lệ");
        ResponseCapture validation = invoke(servlet, "/reports/full", "Bearer admin", Map.of("period", "invalid"));
        assertError(validation, 400, "Kỳ báo cáo không hợp lệ");
    }

    private void assertError(ResponseCapture capture, int status, String message) throws Exception {
        JsonNode root = json(capture);
        assertEquals(status, capture.status);
        assertEquals(Set.of("status", "message"), fields(root));
        assertEquals("error", root.path("status").asText());
        assertEquals(message, root.path("message").asText());
    }

    private void assertAvailability(JsonNode availability, Map<String, String> overrides) {
        assertEquals(SECTION_KEYS, fields(availability));
        for (String section : SECTION_KEYS) {
            String expected = overrides.getOrDefault(section, "AVAILABLE");
            assertEquals(expected, availability.path(section).asText(), section);
            assertTrue(Set.of("AVAILABLE", "UNAVAILABLE").contains(availability.path(section).asText()), section);
        }
    }

    private ResponseCapture invoke(AdminServlet servlet, String path, String authorization, Map<String, String> parameters) throws Exception {
        ResponseCapture capture = new ResponseCapture();
        servlet.doGet(request(path, authorization, parameters), response(capture));
        return capture;
    }

    private HttpServletRequest request(String path, String authorization, Map<String, String> parameters) {
        return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getHeader" -> "Authorization".equals(args[0]) ? authorization : null;
                    case "getPathInfo" -> path;
                    case "getParameter" -> parameters.get((String) args[0]);
                    default -> defaultValue(method.getReturnType());
                });
    }

    private HttpServletResponse response(ResponseCapture capture) {
        return (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {HttpServletResponse.class},
                (proxy, method, args) -> {
                    if ("setStatus".equals(method.getName())) capture.status = (int) args[0];
                    if ("setContentType".equals(method.getName())) capture.contentType = (String) args[0];
                    if ("getWriter".equals(method.getName())) return capture.writer;
                    return defaultValue(method.getReturnType());
                });
    }

    private JsonNode json(ResponseCapture capture) throws Exception {
        capture.writer.flush();
        return JsonUtil.getMapper().readTree(capture.body.toString());
    }

    private Set<String> fields(JsonNode node) {
        Set<String> result = new java.util.HashSet<>();
        node.fieldNames().forEachRemaining(result::add);
        return result;
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        return 0;
    }

    private static Map<String, Object> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("customerCount", null);
        data.put("totalUsers", null);
        data.put("totalOrders", 12L);
        data.put("ordersByStatus", Map.of("PENDING", 2L, "DELIVERED", 5L));
        data.put("pendingOrders", 2L);
        data.put("ordersToday", 4L);
        data.put("topProducts", List.of(Map.of("name", "Burger", "sold", 3L)));
        data.put("operationalOrderCount", 4L);
        data.put("operationalCompletedCount", 3L);
        data.put("completionRate", 75.0);
        data.put("activeOrderCount", 7L);
        data.put("activeOrdersByStatus", Map.of("PREPARING", 3L, "READY", 4L));
        data.put("operationalOrderCountToday", 4L);
        data.put("operationalCompletedCountToday", 3L);
        data.put("completionRateToday", 75.0);
        data.put("deliveredOrdersToday", 2L);
        data.put("activeOrdersToday", 7L);
        data.put("totalRevenue", new BigDecimal("1250.50"));
        data.put("revenueToday", new BigDecimal("300.20"));
        data.put("revenueByMonth", List.of(Map.of("month", 8, "year", 2026, "revenue", new BigDecimal("1250.50"))));
        data.put("grossProfitToday", null);
        data.put("costComplete", false);
        data.put("pendingRefundCount", 1L);
        data.put("pendingCodAmount", new BigDecimal("80.00"));
        data.put("pendingCodCount", 2L);
        data.put("netCashRevenueToday", new BigDecimal("275.20"));
        data.put("aovToday", new BigDecimal("150.10"));
        data.put("activeProductCount", null);
        data.put("totalProducts", null);
        data.put("lowStockThreshold", null);
        data.put("outOfStockSkuCount", 1L);
        data.put("lowStockSkuCount", 2L);
        data.put("lowStockItemCount", 3L);
        data.put("staffingGapCount", 1L);
        data.put("attentionItems", List.of(Map.of("type", "OVERDUE_PENDING_ORDERS", "severity", "WARNING", "count", 2L)));
        data.put("sectionAvailability", availability(Map.of()));
        return data;
    }

    private static Map<String, String> availability(Map<String, String> overrides) {
        Map<String, String> availability = new LinkedHashMap<>();
        for (String section : List.of("financial", "orders", "refunds", "cod", "inventory", "staffing")) {
            availability.put(section, overrides.getOrDefault(section, "AVAILABLE"));
        }
        return availability;
    }

    private static final class StubTokenReader implements AdminApiAuth.TokenReader {
        @Override
        public String role(String token) {
            if ("malformed".equals(token)) throw new IllegalArgumentException("bad token");
            return "user".equals(token) ? "USER" : "ADMIN";
        }

        @Override
        public int userId(String token) {
            return "inactive".equals(token) ? 3 : 1;
        }

        @Override
        public boolean isActiveRole(int userId, String role) {
            return userId != 3;
        }
    }

    private static final class StubAdminService extends AdminService {
        private final Map<String, Object> dashboard;
        private RuntimeException dashboardFailure;
        private RuntimeException reportFailure;
        private String period;
        private String startDate;
        private String endDate;

        private StubAdminService(Map<String, Object> dashboard) {
            this.dashboard = dashboard;
        }

        @Override
        public Map<String, Object> getDashboard() {
            if (dashboardFailure != null) throw dashboardFailure;
            return dashboard;
        }

        @Override
        public Map<String, Object> getDashboardWithPeriod(String period) {
            if (dashboardFailure != null) throw dashboardFailure;
            return dashboard;
        }

        @Override
        public Map<String, Object> getFullReport(String period, String startDate, String endDate) {
            if (reportFailure != null) throw reportFailure;
            this.period = period;
            this.startDate = startDate;
            this.endDate = endDate;
            return Map.of("period", period);
        }
    }

    private static final class ResponseCapture {
        private int status = 200;
        private String contentType;
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body);
    }
}
