package servlet;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;
import utils.JwtUtil;

@WebServlet("/api/user/vouchers")
public class VoucherServlet extends HttpServlet {
    // ponytail: in-memory store, replace with Voucher entity + DAO when feature matures
    private static final CopyOnWriteArrayList<Map<String, Object>> vouchers = new CopyOnWriteArrayList<>();
    static {
        Map<String, Object> v1 = new ConcurrentHashMap<>();
        v1.put("id", 1);
        v1.put("code", "FASTGUY10");
        v1.put("discount", 10);
        v1.put("discountType", "PERCENT");
        v1.put("minOrder", 50000);
        v1.put("maxDiscount", 20000);
        v1.put("expiresAt", "2026-12-31");
        v1.put("description", "Giảm 10% cho đơn từ 50.000đ");
        vouchers.add(v1);

        Map<String, Object> v2 = new ConcurrentHashMap<>();
        v2.put("id", 2);
        v2.put("code", "WELCOME30");
        v2.put("discount", 30000);
        v2.put("discountType", "FIXED");
        v2.put("minOrder", 100000);
        v2.put("maxDiscount", 30000);
        v2.put("expiresAt", "2026-12-31");
        v2.put("description", "Giảm 30.000đ cho đơn từ 100.000đ");
        vouchers.add(v2);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return;
        }
        int userId = JwtUtil.getUserId(authHeader.substring(7));
        if (userId < 0) { ApiResponse.error(resp, "Invalid token", 401); return; }
        ApiResponse.ok(resp, new java.util.ArrayList<>(vouchers));
    }
}
