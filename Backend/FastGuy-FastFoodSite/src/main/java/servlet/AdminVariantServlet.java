package servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.ProductDAO;
import entity.ProductVariant;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;
import utils.JwtUtil;

@WebServlet("/api/admin/variants/*")
public class AdminVariantServlet extends HttpServlet {
    private ProductDAO productDAO = new ProductDAO();
    private ObjectMapper mapper = new ObjectMapper();

    private boolean isAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return false;
        }
        String role = JwtUtil.getRole(authHeader.substring(7));
        if (!"ADMIN".equals(role)) { ApiResponse.error(resp, "Forbidden", 403); return false; }
        return true;
    }

    private BigDecimal readMoney(Map<String, Object> body, String key) {
        BigDecimal value = BigDecimal.valueOf(((Number) body.get(key)).doubleValue());
        if (value.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException(key + " must be >= 0");
        return value;
    }

    private Integer readStock(Map<String, Object> body) {
        if (body.get("quantityAvailable") == null) return null;
        int value = ((Number) body.get("quantityAvailable")).intValue();
        if (value < 0) throw new IllegalArgumentException("Tồn kho không được âm");
        return value;
    }

    private String readStatus(Map<String, Object> body) {
        String status = (String) body.get("status");
        if (!"AVAILABLE".equals(status) && !"UNAVAILABLE".equals(status)) throw new IllegalArgumentException("Trạng thái không hợp lệ");
        return status;
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!isAdmin(req, resp)) return;
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { ApiResponse.error(resp, "Missing variant ID", 400); return; }
        try {
            int id = Integer.parseInt(path.substring(1));
            ProductVariant v = productDAO.findVariantById(id);
            if (v == null) { ApiResponse.error(resp, "Variant not found", 404); return; }
            Map<String, Object> body = mapper.readValue(req.getReader(), new TypeReference<Map<String, Object>>() {});
            if (body.containsKey("variantName")) {
                String variantName = (String) body.get("variantName");
                if (variantName == null || variantName.trim().isEmpty()) { ApiResponse.error(resp, "Tên biến thể không được trống", 400); return; }
                v.setVariantName(variantName.trim());
            }
            try {
                if (body.containsKey("price")) v.setPrice(readMoney(body, "price"));
                if (body.containsKey("status")) v.setStatus(readStatus(body));
                if (body.containsKey("originalPrice")) v.setOriginalPrice(readMoney(body, "originalPrice"));
                if (body.containsKey("quantityAvailable")) v.setQuantityAvailable(readStock(body));
            } catch (IllegalArgumentException e) {
                ApiResponse.error(resp, e.getMessage(), 400);
                return;
            }
            if (body.containsKey("isDefault")) v.setIsDefault((Boolean) body.get("isDefault"));
            if (body.containsKey("sku")) v.setSku((String) body.get("sku"));
            if (body.containsKey("weight"))
                v.setWeight(BigDecimal.valueOf(((Number) body.get("weight")).doubleValue()));
            if (body.containsKey("length"))
                v.setLength(BigDecimal.valueOf(((Number) body.get("length")).doubleValue()));
            if (body.containsKey("width"))
                v.setWidth(BigDecimal.valueOf(((Number) body.get("width")).doubleValue()));
            if (body.containsKey("height"))
                v.setHeight(BigDecimal.valueOf(((Number) body.get("height")).doubleValue()));
            productDAO.saveVariant(v);
            ApiResponse.ok(resp, null, "Updated");
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid ID", 400);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!isAdmin(req, resp)) return;
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { ApiResponse.error(resp, "Missing variant ID", 400); return; }
        try {
            int id = Integer.parseInt(path.substring(1));
            productDAO.deleteVariant(id);
            ApiResponse.ok(resp, null, "Deleted");
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid ID", 400);
        }
    }
}
