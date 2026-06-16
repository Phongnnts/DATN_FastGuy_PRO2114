package controller;

import dto.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.CartService;
import utils.JsonUtil;
import utils.RoleRequired;

import java.io.IOException;

@WebServlet("/api/cart/*")
public class  CartController extends HttpServlet {
    private final CartService cartService = new CartService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "USER");
        Long userId = (Long) req.getAttribute("userId");
        resp.setContentType("application/json; charset=UTF-8");
        writeJson(resp, ApiResponse.ok(cartService.getCart(userId)));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "USER");
        Long userId = (Long) req.getAttribute("userId");
        resp.setContentType("application/json; charset=UTF-8");
        resp.setStatus(201);

        String path = req.getPathInfo();
        if (path != null && path.equals("/migrate")) {
            MigrateCartRequest migrateReq = JsonUtil.fromJson(req.getReader(), MigrateCartRequest.class);
            writeJson(resp, ApiResponse.ok(cartService.migrateCart(userId, migrateReq), "Đồng bộ giỏ hàng thành công"));
        } else {
            AddToCartRequest addReq = JsonUtil.fromJson(req.getReader(), AddToCartRequest.class);
            writeJson(resp, ApiResponse.ok(cartService.addItem(userId, addReq), "Thêm vào giỏ hàng thành công"));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "USER");
        Long userId = (Long) req.getAttribute("userId");
        Long cartItemId = parseIdFromPath(req.getPathInfo());
        resp.setContentType("application/json; charset=UTF-8");

        UpdateCartItemRequest updateReq = JsonUtil.fromJson(req.getReader(), UpdateCartItemRequest.class);
        writeJson(resp, ApiResponse.ok(
                cartService.updateItemQuantity(userId, cartItemId, updateReq.getQuantity()),
                "Cập nhật số lượng thành công"));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "USER");
        Long userId = (Long) req.getAttribute("userId");
        Long cartItemId = parseIdFromPath(req.getPathInfo());
        resp.setContentType("application/json; charset=UTF-8");

        writeJson(resp, ApiResponse.ok(
                cartService.removeItem(userId, cartItemId),
                "Xóa sản phẩm khỏi giỏ hàng thành công"));
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
