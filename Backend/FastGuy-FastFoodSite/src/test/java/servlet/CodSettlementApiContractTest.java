package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.CodSettlementService;
import service.CodSettlementService.SettlementConflictException;
import service.CodSettlementService.SettlementNotFoundException;

class CodSettlementApiContractTest {
    @Test void malformedClaimMapsFailClosedTo401ThroughRealExtraction() throws Exception {
        RecordingService service = new RecordingService();
        List<Map<String, Object>> invalidClaims = List.of(
                Map.of("role", "SHIPPER"),
                Map.of("userId", "41", "role", "SHIPPER"),
                Map.of("userId", 41, "role", 7),
                Map.of("userId", 0, "role", "SHIPPER"),
                Map.of("userId", -1, "role", "SHIPPER"),
                Map.of("userId", 41, "role", ""));

        for (Map<String, Object> claims : invalidClaims) {
            CodSettlementServlet servlet = new CodSettlementServlet(service, token -> claims, (userId, role) -> true);
            ResponseCapture response = invokeGet(servlet, request("/current", "Bearer signed", null, ""));
            assertEquals(401, response.status);
            assertTrue(response.body().contains("Invalid token"));
        }
        assertEquals(0, service.calls);
    }

    @Test void validClaimsExtractIdentityBeforeWrongRoleReturns403() throws Exception {
        RecordingService service = new RecordingService();
        CodSettlementServlet servlet = new CodSettlementServlet(service,
                token -> Map.of("userId", 7, "role", "ADMIN"), (userId, role) -> true);

        ResponseCapture response = invokeGet(servlet, request("/current", "Bearer signed", null, ""));

        assertEquals(403, response.status);
        assertEquals(0, service.calls);
    }

    @Test void missingAndMalformedTokensReturn401() throws Exception {
        RecordingService service = new RecordingService();
        CodSettlementServlet servlet = servlet(service, token -> null, true);

        ResponseCapture missing = invokeGet(servlet, request("/current", null, null, ""));
        ResponseCapture malformed = invokeGet(servlet, request("/current", "Bearer broken", null, ""));

        assertEquals(401, missing.status);
        assertEquals(401, malformed.status);
        assertTrue(missing.body().contains("Missing token"));
        assertTrue(malformed.body().contains("Invalid token"));
        assertEquals(0, service.calls);
    }

    @Test void tokenReaderExceptionsFailClosedTo401() throws Exception {
        RecordingService service = new RecordingService();
        CodSettlementServlet servlet = servlet(service, token -> { throw new RuntimeException("JWT parser failure"); }, true);

        ResponseCapture response = invokeGet(servlet, request("/current", "Bearer broken", null, ""));

        assertEquals(401, response.status);
        assertTrue(response.body().contains("Invalid token"));
        assertEquals(0, service.calls);
    }

    @Test void authenticatedWrongRoleAndInactiveAccountReturn403() throws Exception {
        RecordingService service = new RecordingService();
        CodSettlementServlet wrongRole = servlet(service, token -> Map.of("userId", 7, "role", "ADMIN"), true);
        CodSettlementServlet inactive = servlet(service, token -> Map.of("userId", 8, "role", "SHIPPER"), false);

        assertEquals(403, invokeGet(wrongRole, request("/current", "Bearer valid", null, "")).status);
        assertEquals(403, invokeGet(inactive, request("/current", "Bearer valid", null, "")).status);
        assertEquals(0, service.calls);
    }

    @Test void getRoutesUseJwtIdentityAndAdminStatus() throws Exception {
        RecordingService service = new RecordingService();
        CodSettlementServlet shipper = servlet(service, token -> Map.of("userId", 41, "role", "SHIPPER"), true);
        CodSettlementServlet admin = servlet(service, token -> Map.of("userId", 9, "role", "ADMIN"), true);

        assertEquals(200, invokeGet(shipper, request("/current", "Bearer valid", null, "")).status);
        assertEquals("current:41", service.lastCall);
        assertEquals(200, invokeGet(shipper, request("/mine", "Bearer valid", null, "")).status);
        assertEquals("mine:41", service.lastCall);
        assertEquals(200, invokeGet(admin, request("/admin", "Bearer valid", "SUBMITTED", "")).status);
        assertEquals("admin:SUBMITTED", service.lastCall);
        assertEquals(404, invokeGet(shipper, request("/unknown", "Bearer valid", null, "")).status);
    }

    @Test void postUsesJwtShipperIdentityAndIgnoresBodyIdentity() throws Exception {
        RecordingService service = new RecordingService();
        CodSettlementServlet servlet = servlet(service, token -> Map.of("userId", 41, "role", "SHIPPER"), true);

        ResponseCapture response = invokePost(servlet, request(null, "Bearer valid", null,
                "{\"shipperId\":999,\"shiftId\":12,\"submittedAmount\":150000}"));

        assertEquals(200, response.status);
        assertEquals("submit:41:12:150000", service.lastCall);
    }

    @Test void invalidDecimal18Scale2SubmissionsReturn400() throws Exception {
        CodSettlementServlet servlet = servlet(new ValidatingService(), token -> Map.of("userId", 41, "role", "SHIPPER"), true);
        for (String amount : List.of("10000000000000000.00", "123456789012345678", "1e18", "1.001")) {
            ResponseCapture response = invokePost(servlet, request(null, "Bearer valid", null,
                    "{\"shiftId\":12,\"submittedAmount\":" + amount + "}"));
            assertEquals(400, response.status, amount);
        }
    }

    @Test void putUsesJwtAdminIdentityAndExactPathId() throws Exception {
        RecordingService service = new RecordingService();
        CodSettlementServlet servlet = servlet(service, token -> Map.of("userId", 9, "role", "ADMIN"), true);

        ResponseCapture response = invokePut(servlet, request("/27/verify", "Bearer valid", null,
                "{\"expectedStatus\":\"SUBMITTED\",\"status\":\"SETTLED\",\"verifiedAmount\":150000}"));

        assertEquals(200, response.status);
        assertEquals("verify:9:27:SUBMITTED:SETTLED:150000", service.lastCall);
    }

    @Test void verifyRejectsUnknownAndMissingFields() throws Exception {
        RecordingService service = new RecordingService();
        CodSettlementServlet admin = servlet(service, token -> Map.of("userId", 9, "role", "ADMIN"), true);
        ResponseCapture unknown = invokePut(admin, request("/27/verify", "Bearer valid", null,
                "{\"expectedStatus\":\"SUBMITTED\",\"status\":\"SETTLED\",\"verifiedAmount\":1,\"extra\":true}"));
        ResponseCapture missing = invokePut(admin, request("/27/verify", "Bearer valid", null,
                "{\"expectedStatus\":\"SUBMITTED\",\"status\":\"SETTLED\"}"));
        assertEquals(400, unknown.status);
        assertEquals(400, missing.status);
        assertEquals(0, service.calls);
    }

    @Test void malformedJsonAndWrongExpectedStatusReturnJson400() throws Exception {
        RecordingService service = new RecordingService();
        CodSettlementServlet shipper = servlet(service, token -> Map.of("userId", 41, "role", "SHIPPER"), true);
        CodSettlementServlet admin = servlet(service, token -> Map.of("userId", 9, "role", "ADMIN"), true);

        ResponseCapture malformed = invokePost(shipper, request(null, "Bearer valid", null, "{"));
        ResponseCapture expectedStatus = invokePut(admin, request("/27/verify", "Bearer valid", null,
                "{\"expectedStatus\":\"SETTLED\",\"status\":\"SETTLED\",\"verifiedAmount\":1}"));

        assertEquals(400, malformed.status);
        assertTrue(malformed.body().contains("Invalid JSON"));
        assertEquals(400, expectedStatus.status);
        assertTrue(expectedStatus.body().contains("Invalid expectedStatus"));
        assertEquals("application/json;charset=UTF-8", malformed.contentType);
    }

    @Test void mapsNotFoundConflictValidationSecurityAndUnexpectedFailures() throws Exception {
        assertEquals(404, invokePut(failingServlet(new SettlementNotFoundException("missing")), validVerify()).status);
        assertEquals(409, invokePut(failingServlet(new SettlementConflictException("changed")), validVerify()).status);
        assertEquals(400, invokePut(failingServlet(new IllegalArgumentException("bad")), validVerify()).status);
        assertEquals(403, invokePut(failingServlet(new SecurityException("denied")), validVerify()).status);
        ResponseCapture unexpected = invokePut(failingServlet(new RuntimeException("sql secret")), validVerify());
        assertEquals(500, unexpected.status);
        assertTrue(unexpected.body().contains("Internal server error"));
        assertTrue(!unexpected.body().contains("sql secret"));
    }

    private CodSettlementServlet failingServlet(RuntimeException failure) {
        RecordingService service = new RecordingService();
        service.failure = failure;
        return servlet(service, token -> Map.of("userId", 9, "role", "ADMIN"), true);
    }

    private HttpServletRequest validVerify() {
        return request("/27/verify", "Bearer valid", null,
                "{\"expectedStatus\":\"SUBMITTED\",\"status\":\"SETTLED\",\"verifiedAmount\":1}");
    }

    private CodSettlementServlet servlet(RecordingService service, CodSettlementServlet.TokenReader reader, boolean active) {
        return new CodSettlementServlet(service, reader, (userId, role) -> active);
    }

    private ResponseCapture invokeGet(CodSettlementServlet servlet, HttpServletRequest request) throws Exception {
        ResponseCapture capture = new ResponseCapture();
        servlet.doGet(request, response(capture));
        return capture;
    }

    private ResponseCapture invokePost(CodSettlementServlet servlet, HttpServletRequest request) throws Exception {
        ResponseCapture capture = new ResponseCapture();
        servlet.doPost(request, response(capture));
        return capture;
    }

    private ResponseCapture invokePut(CodSettlementServlet servlet, HttpServletRequest request) throws Exception {
        ResponseCapture capture = new ResponseCapture();
        servlet.doPut(request, response(capture));
        return capture;
    }

    private HttpServletRequest request(String path, String authorization, String status, String body) {
        return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getPathInfo" -> path;
                    case "getHeader" -> "Authorization".equals(args[0]) ? authorization : null;
                    case "getParameter" -> "status".equals(args[0]) ? status : null;
                    case "getReader" -> new BufferedReader(new StringReader(body));
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

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        return 0;
    }

    private static class ValidatingService extends RecordingService {
        @Override public Map<String, Object> submit(int shipperId, int shiftId, BigDecimal submittedAmount) {
            CodSettlementService.validateSubmission(submittedAmount);
            return super.submit(shipperId, shiftId, submittedAmount);
        }
    }

    private static class RecordingService extends CodSettlementService {
        private int calls;
        private String lastCall;
        private RuntimeException failure;

        @Override public Map<String, Object> getShipperCurrent(int shipperId) {
            record("current:" + shipperId);
            return Map.of();
        }

        @Override public List<Map<String, Object>> listForShipper(int shipperId) {
            record("mine:" + shipperId);
            return List.of();
        }

        @Override public List<Map<String, Object>> listForAdmin(String status) {
            record("admin:" + status);
            return List.of();
        }

        @Override public Map<String, Object> submit(int shipperId, int shiftId, BigDecimal submittedAmount) {
            record("submit:" + shipperId + ":" + shiftId + ":" + submittedAmount);
            return Map.of();
        }

        @Override public Map<String, Object> verify(int adminId, int settlementId, String expectedStatus, String status,
                BigDecimal verifiedAmount, String reason) {
            record("verify:" + adminId + ":" + settlementId + ":" + expectedStatus + ":" + status + ":" + verifiedAmount);
            return Map.of();
        }

        private void record(String call) {
            calls++;
            lastCall = call;
            if (failure != null) throw failure;
        }
    }

    private static class ResponseCapture {
        private int status = 200;
        private String contentType;
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body);

        private String body() {
            writer.flush();
            return body.toString();
        }
    }
}
