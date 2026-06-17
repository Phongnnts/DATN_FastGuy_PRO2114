package utils;

import java.util.HashMap;
import java.util.Map;

public class ApiResponse {

    public static Map<String, Object> ok(Object data) {
        Map<String, Object> res = new HashMap<>();
        res.put("status", "success");
        res.put("data", data);
        return res;
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
}
