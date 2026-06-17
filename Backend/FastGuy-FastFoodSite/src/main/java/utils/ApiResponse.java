package utils;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ApiResponse {

    public static Map<String, Object> ok(Object data) {
        Map<String, Object> res = new HashMap<>();
        res.put("status", "success");
        res.put("data", data);
        return res;
    }

    public static void ok(HttpServletResponse resp, Object data) throws IOException {
        JsonUtil.write(resp, ok(data));
    }

    public static void ok(HttpServletResponse resp, Object data, String message) throws IOException {
        Map<String, Object> res = ok(data);
        res.put("message", message);
        JsonUtil.write(resp, res);
    }

    public static Map<String, Object> ok(Object data, String message) {
        Map<String, Object> res = ok(data);
        res.put("message", message);
        return res;
    }

    public static Map<String, Object> error(String message) {
        Map<String, Object> res = new HashMap<>();
        res.put("status", "error");
        res.put("message", message);
        return res;
    }

    public static void error(HttpServletResponse resp, String message, int statusCode) throws IOException {
        resp.setStatus(statusCode);
        JsonUtil.write(resp, error(message));
    }
}
