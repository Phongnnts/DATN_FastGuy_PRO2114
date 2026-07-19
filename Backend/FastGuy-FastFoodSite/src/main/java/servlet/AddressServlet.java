package servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import dao.AddressDAO;
import entity.Address;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AddressService;
import utils.AddressValidator;
import utils.ApiResponse;
import utils.JwtUtil;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/user/addresses/*")
public class AddressServlet extends HttpServlet {
    private AddressDAO addressDAO = new AddressDAO();
    private AddressService addressService = new AddressService();
    private ObjectMapper objectMapper = new ObjectMapper();

    private int getUserId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return -1;
        }
        int userId = JwtUtil.getUserId(authHeader.substring(7));
        if (userId < 0) ApiResponse.error(resp, "Invalid token", 401);
        return userId;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId < 0) return;
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            List<Map<String, Object>> result = addressDAO.findByUserId(userId).stream().map(this::toMap).collect(Collectors.toList());
            ApiResponse.ok(resp, result);
            return;
        }
        try {
            Address address = addressDAO.findById(Integer.parseInt(path.substring(1)));
            if (address == null || address.getUser().getUserId() != userId) {
                ApiResponse.error(resp, "Address not found", 404);
                return;
            }
            ApiResponse.ok(resp, toMap(address));
        } catch (NumberFormatException e) {
            resp.sendError(404);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId < 0) return;
        Map<String, Object> body = objectMapper.readValue(req.getReader(), Map.class);
        String validationError = AddressValidator.validate(body);
        if (validationError != null) {
            ApiResponse.error(resp, validationError, 400);
            return;
        }
        Address address = toAddress(body);
        address.setIsDefault(body.get("isDefault") instanceof Boolean && (Boolean) body.get("isDefault"));
        address.setCreatedAt(LocalDateTime.now());
        try {
            ApiResponse.ok(resp, toMap(addressService.create(userId, address)), "Address created");
        } catch (RuntimeException e) {
            ApiResponse.error(resp, "Could not save address", 500);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId < 0) return;
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            resp.sendError(404);
            return;
        }
        String[] parts = path.split("/");
        try {
            int addressId = Integer.parseInt(parts[1]);
            if (parts.length >= 3 && "default".equals(parts[2])) {
                try {
                    addressService.setDefault(userId, addressId);
                    ApiResponse.ok(resp, null, "Default address updated");
                } catch (IllegalArgumentException e) {
                    ApiResponse.error(resp, "Address not found", 404);
                } catch (RuntimeException e) {
                    ApiResponse.error(resp, "Could not update default address", 500);
                }
                return;
            }
            Map<String, Object> body = objectMapper.readValue(req.getReader(), Map.class);
            String validationError = AddressValidator.validate(body);
            if (validationError != null) {
                ApiResponse.error(resp, validationError, 400);
                return;
            }
            Boolean isDefault = body.containsKey("isDefault") && body.get("isDefault") instanceof Boolean ? (Boolean) body.get("isDefault") : null;
            if (body.containsKey("isDefault") && isDefault == null) {
                ApiResponse.error(resp, "Default address flag is invalid", 400);
                return;
            }
            try {
                ApiResponse.ok(resp, toMap(addressService.update(userId, addressId, toAddress(body), isDefault)), "Address updated");
            } catch (IllegalArgumentException e) {
                ApiResponse.error(resp, "Address not found", 404);
            } catch (RuntimeException e) {
                ApiResponse.error(resp, "Could not save address", 500);
            }
        } catch (NumberFormatException e) {
            resp.sendError(404);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId < 0) return;
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            resp.sendError(404);
            return;
        }
        try {
            int addressId = Integer.parseInt(path.substring(1));
            Address address = addressDAO.findById(addressId);
            if (address == null || address.getUser().getUserId() != userId) {
                ApiResponse.error(resp, "Address not found", 404);
                return;
            }
            addressDAO.delete(addressId);
            ApiResponse.ok(resp, null, "Address deleted");
        } catch (NumberFormatException e) {
            resp.sendError(404);
        }
    }

    private Address toAddress(Map<String, Object> body) {
        Address address = new Address();
        address.setRecipientName(((String) body.get("recipientName")).trim());
        address.setPhone(((String) body.get("phone")).trim());
        address.setStreet(((String) body.get("street")).trim());
        address.setWardName(((String) body.get("wardName")).trim());
        address.setDistrictName(((String) body.get("districtName")).trim());
        address.setProvinceName(((String) body.get("provinceName")).trim());
        address.setGhnProvinceId(((Number) body.get("ghnProvinceId")).intValue());
        address.setGhnDistrictId(((Number) body.get("ghnDistrictId")).intValue());
        address.setGhnWardCode(((String) body.get("ghnWardCode")).trim());
        address.setCity(body.get("city") instanceof String ? (String) body.get("city") : "Hồ Chí Minh");
        return address;
    }

    private Map<String, Object> toMap(Address address) {
        Map<String, Object> result = new HashMap<>();
        result.put("addressId", address.getAddressId());
        result.put("recipientName", address.getRecipientName());
        result.put("phone", address.getPhone());
        result.put("street", address.getStreet());
        result.put("wardName", address.getWardName());
        result.put("districtName", address.getDistrictName());
        result.put("provinceName", address.getProvinceName());
        result.put("ghnProvinceId", address.getGhnProvinceId());
        result.put("ghnDistrictId", address.getGhnDistrictId());
        result.put("ghnWardCode", address.getGhnWardCode());
        result.put("city", address.getCity());
        result.put("isDefault", address.getIsDefault());
        result.put("createdAt", address.getCreatedAt() != null ? address.getCreatedAt().toString() : null);
        return result;
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}
