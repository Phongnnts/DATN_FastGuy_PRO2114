package controller;

import dto.AddressDTO;
import dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AddressService;
import utils.JsonUtil;
import utils.RoleRequired;

import java.io.IOException;

@WebServlet("/api/addresses/*")
public class AddressController extends HttpServlet {
    private final AddressService addressService = new AddressService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "USER");
        Long userId = (Long) req.getAttribute("userId");
        resp.setContentType("application/json; charset=UTF-8");
        writeJson(resp, ApiResponse.ok(addressService.getByUserId(userId)));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "USER");
        Long userId = (Long) req.getAttribute("userId");
        resp.setContentType("application/json; charset=UTF-8");

        AddressDTO dto = JsonUtil.fromJson(req.getReader(), AddressDTO.class);
        AddressDTO created = addressService.create(userId, dto);
        resp.setStatus(201);
        writeJson(resp, ApiResponse.ok(created, "Thêm địa chỉ thành công"));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "USER");
        Long userId = (Long) req.getAttribute("userId");
        Long addressId = parseIdFromPath(req.getPathInfo());
        resp.setContentType("application/json; charset=UTF-8");

        AddressDTO dto = JsonUtil.fromJson(req.getReader(), AddressDTO.class);
        AddressDTO updated = addressService.update(userId, addressId, dto);
        writeJson(resp, ApiResponse.ok(updated, "Cập nhật địa chỉ thành công"));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "USER");
        Long userId = (Long) req.getAttribute("userId");
        Long addressId = parseIdFromPath(req.getPathInfo());
        resp.setContentType("application/json; charset=UTF-8");

        addressService.delete(userId, addressId);
        writeJson(resp, ApiResponse.ok(null, "Xóa địa chỉ thành công"));
    }

    private Long parseIdFromPath(String pathInfo) {
        if (pathInfo == null || pathInfo.equals("/")) return null;
        String[] parts = pathInfo.split("/");
        if (parts.length < 2) return null;
        return Long.parseLong(parts[1]);
    }

    private void writeJson(HttpServletResponse resp, Object data) throws IOException {
        resp.getWriter().write(JsonUtil.toJson(data));
    }
}
