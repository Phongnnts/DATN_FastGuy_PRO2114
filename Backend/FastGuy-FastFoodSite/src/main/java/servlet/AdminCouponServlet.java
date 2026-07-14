package servlet;

import dao.CouponDAO;
import entity.Coupon;
import jakarta.persistence.PersistenceException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;
import utils.JsonUtil;
import utils.JwtUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@WebServlet("/api/admin/coupons/*")
public class AdminCouponServlet extends HttpServlet {
    private static final Set<String> TYPES = Set.of("PERCENT", "FIXED", "FREE_SHIPPING");
    private final CouponDAO couponDAO = new CouponDAO();

    private boolean checkAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return false;
        }
        if (!"ADMIN".equals(JwtUtil.getRole(authHeader.substring(7)))) {
            ApiResponse.error(resp, "Forbidden", 403);
            return false;
        }
        return true;
    }

    private Map<String, Object> toMap(Coupon c) {
        Map<String, Object> m = new HashMap<>();
        m.put("couponId", c.getCouponId());
        m.put("code", c.getCode());
        m.put("type", c.getType());
        m.put("value", c.getValue());
        m.put("minOrder", c.getMinOrder() != null ? c.getMinOrder() : BigDecimal.ZERO);
        m.put("maxDiscount", c.getMaxDiscount());
        m.put("maxUses", c.getMaxUses());
        m.put("usedCount", c.getUsedCount());
        m.put("expiresAt", c.getExpiresAt() != null ? c.getExpiresAt().toString() : null);
        m.put("isActive", Boolean.TRUE.equals(c.getIsActive()));
        m.put("isPublic", Boolean.TRUE.equals(c.getIsPublic()));
        m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
        m.put("canDelete", !couponDAO.hasReferences(c.getCouponId()));
        return m;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            List<Map<String, Object>> list = couponDAO.findAll().stream().map(this::toMap).collect(Collectors.toList());
            ApiResponse.ok(resp, list);
            return;
        }
        try {
            Coupon coupon = couponDAO.findById(Integer.parseInt(path.substring(1)));
            if (coupon == null) {
                ApiResponse.error(resp, "Not found", 404);
                return;
            }
            ApiResponse.ok(resp, toMap(coupon));
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid ID", 400);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;
        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) {
            ApiResponse.error(resp, "Invalid data", 400);
            return;
        }
        try {
            Coupon coupon = new Coupon();
            apply(body, coupon, true);
            if (couponDAO.findByCode(coupon.getCode()) != null) {
                ApiResponse.error(resp, "Mã giảm giá đã tồn tại", 409);
                return;
            }
            coupon.setIsActive(true);
            coupon.setUsedCount(0);
            coupon.setCreatedAt(LocalDateTime.now());
            couponDAO.save(coupon);
            resp.setStatus(201);
            ApiResponse.ok(resp, toMap(coupon), "Created");
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        } catch (PersistenceException e) {
            ApiResponse.error(resp, "Không thể lưu mã giảm giá", 409);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            ApiResponse.error(resp, "Missing ID", 400);
            return;
        }
        try {
            Coupon coupon = couponDAO.findById(Integer.parseInt(path.substring(1)));
            if (coupon == null) {
                ApiResponse.error(resp, "Not found", 404);
                return;
            }
            Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
            if (body == null) {
                ApiResponse.error(resp, "Invalid data", 400);
                return;
            }
            String oldCode = coupon.getCode();
            apply(body, coupon, false);
            if (!oldCode.equals(coupon.getCode())) {
                Coupon existing = couponDAO.findByCode(coupon.getCode());
                if (existing != null && existing.getCouponId() != coupon.getCouponId()) {
                    ApiResponse.error(resp, "Mã giảm giá đã tồn tại", 409);
                    return;
                }
            }
            couponDAO.save(coupon);
            ApiResponse.ok(resp, toMap(coupon), "Updated");
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid ID", 400);
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        } catch (PersistenceException e) {
            ApiResponse.error(resp, "Không thể lưu mã giảm giá", 409);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            ApiResponse.error(resp, "Missing ID", 400);
            return;
        }
        try {
            int id = Integer.parseInt(path.substring(1));
            if (couponDAO.findById(id) == null) {
                ApiResponse.error(resp, "Not found", 404);
                return;
            }
            if (couponDAO.hasReferences(id)) {
                ApiResponse.error(resp, "Không thể xóa mã đã có lịch sử nhận hoặc sử dụng", 409);
                return;
            }
            couponDAO.delete(id);
            ApiResponse.ok(resp, null, "Deleted");
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid ID", 400);
        }
    }

    private void apply(Map<String, Object> body, Coupon coupon, boolean creating) {
        if (creating || body.containsKey("code")) coupon.setCode(code(body.get("code")));
        if (creating || body.containsKey("type")) coupon.setType(type(body.get("type")));
        if (creating || body.containsKey("value")) coupon.setValue(amount(body.get("value"), "Giá trị"));
        if (creating || body.containsKey("minOrder")) coupon.setMinOrder(amountOrZero(body.get("minOrder"), "Đơn tối thiểu"));
        if (creating || body.containsKey("maxDiscount")) coupon.setMaxDiscount(optionalAmount(body.get("maxDiscount"), "Giảm tối đa"));
        if (creating || body.containsKey("maxUses")) coupon.setMaxUses(nonNegativeInt(body.get("maxUses"), "Số lần dùng tối đa"));
        if (creating || body.containsKey("expiresAt")) coupon.setExpiresAt(expiry(body.get("expiresAt")));
        if (body.containsKey("isActive")) coupon.setIsActive(bool(body.get("isActive"), "Trạng thái kích hoạt"));
        if (creating || body.containsKey("isPublic")) coupon.setIsPublic(body.containsKey("isPublic") ? bool(body.get("isPublic"), "Trạng thái công khai") : true);
        if (creating || body.containsKey("code") || body.containsKey("type") || body.containsKey("value") || body.containsKey("minOrder") || body.containsKey("maxDiscount") || body.containsKey("maxUses") || body.containsKey("expiresAt")) validate(coupon);
    }

    private String code(Object value) {
        if (!(value instanceof String code) || !code.trim().matches("[A-Za-z0-9_-]{3,50}")) throw new IllegalArgumentException("Mã chỉ gồm chữ, số, dấu gạch nối hoặc gạch dưới (3-50 ký tự)");
        return code.trim().toUpperCase();
    }

    private String type(Object value) {
        if (!(value instanceof String type) || !TYPES.contains(type)) throw new IllegalArgumentException("Loại mã giảm giá không hợp lệ");
        return type;
    }

    private BigDecimal amount(Object value, String label) {
        if (!(value instanceof Number number)) throw new IllegalArgumentException(label + " không hợp lệ");
        BigDecimal result = new BigDecimal(number.toString());
        if (result.signum() < 0) throw new IllegalArgumentException(label + " không được âm");
        return result;
    }

    private BigDecimal amountOrZero(Object value, String label) {
        return value == null ? BigDecimal.ZERO : amount(value, label);
    }

    private BigDecimal optionalAmount(Object value, String label) {
        return value == null ? null : amount(value, label);
    }

    private int nonNegativeInt(Object value, String label) {
        if (!(value instanceof Number number) || number.intValue() < 0 || number.doubleValue() != number.intValue()) throw new IllegalArgumentException(label + " không hợp lệ");
        return number.intValue();
    }

    private LocalDateTime expiry(Object value) {
        if (value == null || (value instanceof String text && text.isBlank())) return null;
        if (!(value instanceof String text)) throw new IllegalArgumentException("Thời hạn không hợp lệ");
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            throw new IllegalArgumentException("Thời hạn không hợp lệ");
        }
    }

    private boolean bool(Object value, String label) {
        if (!(value instanceof Boolean bool)) throw new IllegalArgumentException(label + " không hợp lệ");
        return bool;
    }

    private void validate(Coupon coupon) {
        if ("PERCENT".equals(coupon.getType()) && (coupon.getValue().signum() <= 0 || coupon.getValue().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("Phần trăm giảm phải từ 1 đến 100");
        }
        if ("FIXED".equals(coupon.getType()) && coupon.getValue().signum() <= 0) throw new IllegalArgumentException("Giá trị giảm phải lớn hơn 0");
        if ("FREE_SHIPPING".equals(coupon.getType())) {
            coupon.setValue(BigDecimal.ZERO);
            coupon.setMaxDiscount(null);
        }
        if (!"PERCENT".equals(coupon.getType())) coupon.setMaxDiscount(null);
    }
}
