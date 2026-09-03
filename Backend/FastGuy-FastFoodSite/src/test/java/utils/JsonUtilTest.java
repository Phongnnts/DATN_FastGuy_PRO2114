package utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;

class JsonUtilTest {
    @Test
    void writeDeclaresUtf8JsonContentType() throws Exception {
        StringWriter body = new StringWriter();
        String[] contentType = new String[1];
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[] { HttpServletResponse.class },
            (proxy, method, args) -> switch (method.getName()) {
                case "setContentType" -> {
                    contentType[0] = (String) args[0];
                    yield null;
                }
                case "getWriter" -> new PrintWriter(body);
                default -> null;
            }
        );

        JsonUtil.write(response, ApiResponse.error("Denied"));

        assertEquals("application/json;charset=UTF-8", contentType[0]);
        assertEquals("{\"message\":\"Denied\",\"status\":\"error\"}", body.toString());
    }
}
