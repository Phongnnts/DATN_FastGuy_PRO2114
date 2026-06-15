package controller;

import dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.ShipperService;
import utils.JsonUtil;
import utils.RoleRequired;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/shipper/*")
public class ShipperController extends HttpServlet {
    private final ShipperService shipperService = new ShipperService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "SHIPPER", "ADMIN");
        Long userId = (Long) req.getAttribute("userId");
        resp.setContentType("application/json; charset=UTF-8");
        writeJson(resp, ApiResponse.ok(shipperService.getDeliveries(userId)));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "SHIPPER", "ADMIN");
        Long userId = (Long) req.getAttribute("userId");
        resp.setContentType("application/json; charset=UTF-8");

        String path = req.getPathInfo();
        if (path == null) return;

        if (path.matches("/orders/\\d+/pick-up")) {
            Long orderId = parseIdAt(path, 2);
            writeJson(resp, ApiResponse.ok(shipperService.pickUp(orderId, userId), "Nhận đơn giao hàng thành công"));
        } else if (path.matches("/orders/\\d+/deliver")) {
            Long orderId = parseIdAt(path, 2);
            writeJson(resp, ApiResponse.ok(shipperService.deliver(orderId), "Xác nhận giao hàng thành công"));
        } else if (path.matches("/orders/\\d+/fail")) {
            Long orderId = parseIdAt(path, 2);
            Map<String, String> body = JsonUtil.fromJson(req.getReader(), Map.class);
            writeJson(resp, ApiResponse.ok(shipperService.failDelivery(orderId, body.get("reason")), "Cập nhật thất bại"));
        } else if (path.matches("/orders/\\d+/collect")) {
            Long orderId = parseIdAt(path, 2);
            writeJson(resp, ApiResponse.ok(shipperService.collectPayment(orderId, userId), "Thu tiền thành công"));
        }
    }

    private Long parseIdAt(String path, int index) {
        String[] parts = path.split("/");
        return Long.parseLong(parts[index]);
    }

    private void writeJson(HttpServletResponse resp, Object data) throws IOException {
        resp.getWriter().write(JsonUtil.toJson(data));
    }
}
