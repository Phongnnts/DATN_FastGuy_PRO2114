package controller;

import dto.ApiResponse;
import dto.CreateOrderRequest;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.OrderService;
import utils.JsonUtil;
import utils.RoleRequired;

import java.io.IOException;

@WebServlet("/api/orders/*")
public class OrderController extends HttpServlet {
    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");

        String pathInfo = req.getPathInfo();
        String trackCode = req.getParameter("orderCode");
        String trackPhone = req.getParameter("phone");

        if (pathInfo != null && pathInfo.equals("/track") && trackCode != null && trackPhone != null) {
            writeJson(resp, ApiResponse.ok(orderService.trackOrder(trackCode, trackPhone)));
            return;
        }

        RoleRequired.require(req, "USER");
        Long userId = (Long) req.getAttribute("userId");

        if (pathInfo == null || pathInfo.equals("/")) {
            writeJson(resp, ApiResponse.ok(orderService.getByUserId(userId)));
        } else {
            Long orderId = parseIdFromPath(pathInfo);
            writeJson(resp, ApiResponse.ok(orderService.getById(orderId)));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "USER");
        Long userId = (Long) req.getAttribute("userId");
        resp.setContentType("application/json; charset=UTF-8");
        resp.setStatus(201);

        CreateOrderRequest createReq = JsonUtil.fromJson(req.getReader(), CreateOrderRequest.class);
        writeJson(resp, ApiResponse.ok(orderService.createFromCart(userId, createReq), "Đặt hàng thành công"));
    }

    private Long parseIdFromPath(String pathInfo) {
        String[] parts = pathInfo.split("/");
        if (parts.length < 2) return null;
        return Long.parseLong(parts[1]);
    }

    private void writeJson(HttpServletResponse resp, Object data) throws IOException {
        resp.getWriter().write(JsonUtil.toJson(data));
    }
}
