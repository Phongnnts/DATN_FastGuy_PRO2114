package servlet;

import service.CouponService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;
import utils.JwtUtil;
import utils.JsonUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

@WebServlet("/api/coupons/*")
public class CouponServlet extends HttpServlet {
    private CouponService couponService = new CouponService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String path = req.getPathInfo();

        if ("/public".equals(path)) {
            Integer userId = null;
            String authHeader = req.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try { userId = JwtUtil.getUserId(authHeader.substring(7)); } catch (Exception e) {}
            }
            ApiResponse.ok(resp, couponService.getPublicCoupons(userId));
            return;
        }

        if ("/claimed".equals(path)) {
            String auth = req.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) { ApiResponse.error(resp, "Unauthorized", 401); return; }
            int userId;
            try { userId = JwtUtil.getUserId(auth.substring(7)); } catch (Exception e) { ApiResponse.error(resp, "Unauthorized", 401); return; }
            ApiResponse.ok(resp, couponService.getClaimedCoupons(userId));
            return;
        }

        ApiResponse.error(resp, "Not found", 404);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String path = req.getPathInfo();

        if ("/verify".equals(path)) {
            handleVerify(req, resp);
        } else if ("/claim".equals(path)) {
            handleClaim(req, resp);
        } else {
            ApiResponse.error(resp, "Not found", 404);
        }
    }

    private void handleVerify(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }

        String code = (String) body.get("code");
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal shippingFee = BigDecimal.ZERO;
        if (body.containsKey("totalAmount")) {
            totalAmount = BigDecimal.valueOf(((Number) body.get("totalAmount")).doubleValue());
        }
        if (body.containsKey("shippingFee")) {
            shippingFee = BigDecimal.valueOf(((Number) body.get("shippingFee")).doubleValue());
        }

        Integer userId = null;
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try { userId = JwtUtil.getUserId(authHeader.substring(7)); } catch (Exception e) {}
        }

        Map<String, Object> result = couponService.verify(code, totalAmount, shippingFee, userId);
        ApiResponse.ok(resp, result);
    }

    private void handleClaim(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) { ApiResponse.error(resp, "Unauthorized", 401); return; }
        int userId;
        try { userId = JwtUtil.getUserId(auth.substring(7)); } catch (Exception e) { ApiResponse.error(resp, "Unauthorized", 401); return; }

        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }

        int couponId = ((Number) body.get("couponId")).intValue();
        String err = couponService.claim(couponId, userId);
        if (err != null) {
            ApiResponse.error(resp, err, 400);
        } else {
            ApiResponse.ok(resp, null, "Claimed");
        }
    }
}
