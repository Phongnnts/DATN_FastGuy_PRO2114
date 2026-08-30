package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.WorkShiftService;
import utils.JsonUtil;

class ShiftServletBehaviorTest {
    @Test
    void checkoutOwnershipConflictReturnsContract409() throws Exception {
        ShiftServlet servlet = new AuthorizedShiftServlet(new ConflictWorkShiftService());
        HttpServletRequest request = request("/11/check-out");
        ResponseCapture capture = new ResponseCapture();

        servlet.doPost(request, response(capture));
        capture.writer.flush();

        Map<?, ?> body = JsonUtil.fromJson(capture.body.toString(), Map.class);
        assertEquals(409, capture.status);
        assertEquals("error", body.get("status"));
        assertEquals("Active order ownership must be handed over before check-out", body.get("message"));
        assertEquals(3L, ((Number) ((Map<?, ?>) body.get("data")).get("activeOwnershipCount")).longValue());
    }

    private HttpServletRequest request(String path) {
        return (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {HttpServletRequest.class},
                (proxy, method, args) -> "getPathInfo".equals(method.getName()) ? path : defaultValue(method.getReturnType()));
    }

    private HttpServletResponse response(ResponseCapture capture) {
        return (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {HttpServletResponse.class},
                (proxy, method, args) -> {
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

    private static class AuthorizedShiftServlet extends ShiftServlet {
        private AuthorizedShiftServlet(WorkShiftService service) {
            super(service);
        }

        @Override
        protected int worker(HttpServletRequest req, HttpServletResponse resp) {
            return 7;
        }
    }

    private static class ConflictWorkShiftService extends WorkShiftService {
        @Override
        public Map<String, Object> check(int shiftId, int userId, boolean checkIn) {
            assertEquals(11, shiftId);
            assertEquals(7, userId);
            assertEquals(false, checkIn);
            throw new ActiveOwnershipConflict(3);
        }
    }

    private static class ResponseCapture {
        private int status = 200;
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body);
    }
}
