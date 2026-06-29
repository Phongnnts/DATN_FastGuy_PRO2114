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
            if (body.containsKey("variantName")) v.setVariantName((String) body.get("variantName"));
            if (body.containsKey("price")) v.setPrice(BigDecimal.valueOf(((Number) body.get("price")).doubleValue()));
            if (body.containsKey("isDefault")) v.setIsDefault((Boolean) body.get("isDefault"));
            if (body.containsKey("status")) v.setStatus((String) body.get("status"));
            if (body.containsKey("originalPrice"))
                v.setOriginalPrice(BigDecimal.valueOf(((Number) body.get("originalPrice")).doubleValue()));
            if (body.containsKey("sku")) v.setSku((String) body.get("sku"));
            if (body.containsKey("quantityAvailable"))
                v.setQuantityAvailable(((Number) body.get("quantityAvailable")).intValue());
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
