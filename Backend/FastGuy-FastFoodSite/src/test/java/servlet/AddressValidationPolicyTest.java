package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import entity.Address;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AddressService;
import utils.JwtUtil;

class AddressValidationPolicyTest {
    private static final String BODY = """
            {"recipientName":"Nguyen Van A","phone":"0901234567","street":"1 Main St","wardName":"Ward 1","districtName":"District 1","provinceName":"Ho Chi Minh","ghnProvinceId":202,"ghnDistrictId":"abc","ghnWardCode":"20101"}
            """;

    @Test
    void invalidDistrictReturns400BeforePostServiceCall() throws Exception {
        TestContext context = context(null);

        context.servlet.doPost(context.request, context.response);

        assertRejectedBeforeService(context);
    }

    @Test
    void invalidDistrictReturns400BeforePutServiceCall() throws Exception {
        TestContext context = context("/1");

        context.servlet.doPut(context.request, context.response);

        assertRejectedBeforeService(context);
    }

    private TestContext context(String pathInfo) throws Exception {
        AddressServlet servlet = new AddressServlet();
        CountingAddressService service = new CountingAddressService();
        Field field = AddressServlet.class.getDeclaredField("addressService");
        field.setAccessible(true);
        field.set(servlet, service);
        ResponseCapture capture = new ResponseCapture();
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getHeader" -> "Authorization".equals(args[0]) ? "Bearer " + JwtUtil.generate(1, "USER") : null;
                    case "getPathInfo" -> pathInfo;
                    case "getReader" -> new BufferedReader(new StringReader(BODY));
                    default -> defaultValue(method.getReturnType());
                });
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {HttpServletResponse.class},
                (proxy, method, args) -> {
                    if ("setStatus".equals(method.getName())) capture.status = (int) args[0];
                    if ("getWriter".equals(method.getName())) return capture.writer;
                    return defaultValue(method.getReturnType());
                });
        return new TestContext(servlet, service, request, response, capture);
    }

    private void assertRejectedBeforeService(TestContext context) {
        context.capture.writer.flush();
        assertEquals(400, context.capture.status);
        assertTrue(context.capture.body.toString().contains("Quan/huyen GHN khong hop le"));
        assertEquals(0, context.service.calls);
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        return 0;
    }

    private record TestContext(AddressServlet servlet, CountingAddressService service,
            HttpServletRequest request, HttpServletResponse response, ResponseCapture capture) {
    }

    private static class ResponseCapture {
        private int status;
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body);
    }

    private static class CountingAddressService extends AddressService {
        private int calls;

        @Override
        public Address create(int userId, Address address) {
            calls++;
            return address;
        }

        @Override
        public Address update(int userId, int addressId, Address address, Boolean isDefault) {
            calls++;
            return address;
        }
    }
}
