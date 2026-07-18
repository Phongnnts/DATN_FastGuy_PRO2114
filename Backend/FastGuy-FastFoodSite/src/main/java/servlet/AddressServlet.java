package servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import dao.AddressDAO;
import dao.UserDAO;
import entity.Address;
import entity.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private UserDAO userDAO = new UserDAO();
    private ObjectMapper objectMapper = new ObjectMapper();

    private int getUserId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return -1;
        }
        String token = authHeader.substring(7);
        int userId = JwtUtil.getUserId(token);
        if (userId < 0) {
            ApiResponse.error(resp, "Invalid token", 401);
        }
        return userId;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId < 0) return;

        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            List<Address> addresses = addressDAO.findByUserId(userId);
            List<Map<String, Object>> result = addresses.stream().map(this::toMap).collect(Collectors.toList());
            ApiResponse.ok(resp, result);
        } else {
            try {
                int addressId = Integer.parseInt(path.substring(1));
                Address address = addressDAO.findById(addressId);
                if (address == null || address.getUser().getUserId() != userId) {
                    ApiResponse.error(resp, "Address not found", 404);
                    return;
                }
                ApiResponse.ok(resp, toMap(address));
            } catch (NumberFormatException e) {
                resp.sendError(404);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req, resp);
        if (userId < 0) return;

        Map<String, Object> body = objectMapper.readValue(req.getReader(), Map.class);
        if (body == null) {
            ApiResponse.error(resp, "Invalid data", 400);
            return;
        }

        User user = userDAO.findById(userId);
        if (user == null) {
            ApiResponse.error(resp, "User not found", 404);
            return;
        }

        Address address = new Address();
        address.setUser(user);
        address.setRecipientName((String) body.get("recipientName"));
        address.setPhone((String) body.get("phone"));
        address.setStreet((String) body.get("street"));
        address.setWardName((String) body.get("wardName"));
        address.setDistrictName((String) body.get("districtName"));
        address.setProvinceName((String) body.get("provinceName"));
        if (body.containsKey("ghnProvinceId")) address.setGhnProvinceId(((Number) body.get("ghnProvinceId")).intValue());
        if (body.containsKey("ghnDistrictId")) address.setGhnDistrictId(((Number) body.get("ghnDistrictId")).intValue());
        if (body.containsKey("ghnWardCode")) address.setGhnWardCode((String) body.get("ghnWardCode"));
        address.setCity((String) body.getOrDefault("city", "Hồ Chí Minh"));

        boolean isDefault = body.get("isDefault") instanceof Boolean && (Boolean) body.get("isDefault");
        if (isDefault) {
            addressDAO.resetDefaultForUser(userId);
        }
        address.setIsDefault(isDefault);
        address.setCreatedAt(LocalDateTime.now());

        addressDAO.save(address);

        Map<String, Object> result = toMap(address);
        ApiResponse.ok(resp, result, "Address created");
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
                Address address = addressDAO.findById(addressId);
                if (address == null || address.getUser().getUserId() != userId) {
                    ApiResponse.error(resp, "Address not found", 404);
                    return;
                }
                addressDAO.resetDefaultForUser(userId);
                address.setIsDefault(true);
                addressDAO.save(address);
                ApiResponse.ok(resp, null, "Default address updated");
                return;
            }

            Map<String, Object> body = objectMapper.readValue(req.getReader(), Map.class);
            if (body == null) {
                ApiResponse.error(resp, "Invalid data", 400);
                return;
            }

            Address address = addressDAO.findById(addressId);
            if (address == null || address.getUser().getUserId() != userId) {
                ApiResponse.error(resp, "Address not found", 404);
                return;
            }

            if (body.containsKey("recipientName")) address.setRecipientName((String) body.get("recipientName"));
            if (body.containsKey("phone")) address.setPhone((String) body.get("phone"));
            if (body.containsKey("street")) address.setStreet((String) body.get("street"));
            if (body.containsKey("wardName")) address.setWardName((String) body.get("wardName"));
            if (body.containsKey("districtName")) address.setDistrictName((String) body.get("districtName"));
            if (body.containsKey("provinceName")) address.setProvinceName((String) body.get("provinceName"));
            if (body.containsKey("ghnProvinceId")) address.setGhnProvinceId(((Number) body.get("ghnProvinceId")).intValue());
            if (body.containsKey("ghnDistrictId")) address.setGhnDistrictId(((Number) body.get("ghnDistrictId")).intValue());
            if (body.containsKey("ghnWardCode")) address.setGhnWardCode((String) body.get("ghnWardCode"));
            if (body.containsKey("city")) address.setCity((String) body.get("city"));

            if (body.containsKey("isDefault")) {
                boolean isDefault = (Boolean) body.get("isDefault");
                if (isDefault) {
                    addressDAO.resetDefaultForUser(userId);
                }
                address.setIsDefault(isDefault);
            }

            addressDAO.save(address);
            ApiResponse.ok(resp, toMap(address), "Address updated");
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

    private Map<String, Object> toMap(Address a) {
        Map<String, Object> m = new HashMap<>();
        m.put("addressId", a.getAddressId());
        m.put("recipientName", a.getRecipientName());
        m.put("phone", a.getPhone());
        m.put("street", a.getStreet());
        m.put("wardName", a.getWardName());
        m.put("districtName", a.getDistrictName());
        m.put("provinceName", a.getProvinceName());
        m.put("ghnProvinceId", a.getGhnProvinceId());
        m.put("ghnDistrictId", a.getGhnDistrictId());
        m.put("ghnWardCode", a.getGhnWardCode());
        m.put("city", a.getCity());
        m.put("isDefault", a.getIsDefault());
        m.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
        return m;
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}
