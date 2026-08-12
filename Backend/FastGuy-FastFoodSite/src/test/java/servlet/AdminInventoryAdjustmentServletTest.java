package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.junit.jupiter.api.Test;

import exception.InventoryConflictException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.InventoryAdjustmentService;

class AdminInventoryAdjustmentServletTest {

    @Test
    void staleExpectedQuantityReturns409WithRuntimeData() throws Exception {
        InventoryAdjustmentService service = new ConflictService();
        AdminInventoryAdjustmentServlet servlet = new TestServlet(service);
        ResponseCapture capture = new ResponseCapture();
        HttpServletRequest request = request("""
                {"variantId":12,"operation":"INCREASE","quantity":3,"expectedQuantity":26,"reasonCode":"STOCK_COUNT","note":null}
                """);
        HttpServletResponse response = response(capture);

        servlet.doPost(request, response);

        capture.writer.flush();
        assertEquals(409, capture.status);
        assertTrue(capture.body.toString().contains("\"variantId\":12"));
        assertTrue(capture.body.toString().contains("\"currentQuantity\":27"));
    }

    private HttpServletRequest request(String body) {
        return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {HttpServletRequest.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getPathInfo" -> "/adjustments";
                    case "getReader" -> new BufferedReader(new StringReader(body));
                    default -> defaultValue(method.getReturnType());
                });
    }

    private HttpServletResponse response(ResponseCapture capture) {
        return (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {HttpServletResponse.class}, (proxy, method, args) -> {
                    if ("setStatus".equals(method.getName())) capture.status = (int) args[0];
                    if ("getWriter".equals(method.getName())) return capture.writer;
                    return defaultValue(method.getReturnType());
                });
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        return 0;
    }

    private static class TestServlet extends AdminInventoryAdjustmentServlet {
        private TestServlet(InventoryAdjustmentService service) {
            super(service);
        }

        @Override
        protected int requireAdmin(HttpServletRequest req, HttpServletResponse resp) {
            return 1;
        }
    }

    private static class ConflictService extends InventoryAdjustmentService {
        @Override
        public Map<String, Object> adjust(int variantId, String operation, int quantity, Integer expectedQuantity,
                String reasonCode, String note, int adminId) {
            throw new InventoryConflictException(variantId, 27);
        }
    }

    private static class ResponseCapture {
        private int status;
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body);
    }
}
