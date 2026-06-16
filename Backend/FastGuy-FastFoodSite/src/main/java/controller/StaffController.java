package controller;

import dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.ScheduleService;
import service.StaffService;
import utils.JsonUtil;
import utils.RoleRequired;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

@WebServlet("/api/staff/*")
public class StaffController extends HttpServlet {
    private final StaffService staffService = new StaffService();
    private final ScheduleService scheduleService = new ScheduleService();

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "STAFF", "ADMIN");
        Long userId = (Long) req.getAttribute("userId");
        resp.setContentType("application/json; charset=UTF-8");

        String path = req.getPathInfo();
        if (path != null && path.equals("/ingredients")) {
            writeJson(resp, ApiResponse.ok(staffService.getAllIngredients()));
        } else if (path != null && path.equals("/ingredients/low-stock")) {
            writeJson(resp, ApiResponse.ok(staffService.getLowStockIngredients()));
        } else if (path != null && path.equals("/orders/history")) {
            writeJson(resp, ApiResponse.ok(staffService.getOrderHistory(userId)));
        } else if (path != null && path.equals("/shifts/today")) {
            writeJson(resp, ApiResponse.ok(scheduleService.getTodaySchedule(userId)));
        } else {
            writeJson(resp, ApiResponse.ok(staffService.getAllOrders()));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        RoleRequired.require(req, "STAFF", "ADMIN");
        Long userId = (Long) req.getAttribute("userId");
        resp.setContentType("application/json; charset=UTF-8");

        String path = req.getPathInfo();
        if (path == null) return;

        if (path.matches("/orders/\\d+/confirm")) {
            Long orderId = parseIdAt(path, 2);
            writeJson(resp, ApiResponse.ok(staffService.confirmOrder(orderId, userId), "Xác nhận đơn hàng thành công"));
        } else if (path.matches("/orders/\\d+/ready")) {
            Long orderId = parseIdAt(path, 2);
            writeJson(resp, ApiResponse.ok(staffService.markReady(orderId), "Đánh dấu sẵn sàng thành công"));
        } else if (path.matches("/orders/\\d+/cancel")) {
            Long orderId = parseIdAt(path, 2);
            Map<String, String> body = JsonUtil.fromJson(req.getReader(), Map.class);
            staffService.cancelOrder(orderId, body.get("reason"));
            writeJson(resp, ApiResponse.ok(null, "Hủy đơn hàng thành công"));
        } else if (path.equals("/shifts/check-in")) {
            writeJson(resp, ApiResponse.ok(scheduleService.checkIn(userId), "Check-in thành công"));
        } else if (path.equals("/shifts/check-out")) {
            writeJson(resp, ApiResponse.ok(scheduleService.checkOut(userId), "Check-out thành công"));
        } else if (path.matches("/orders/\\d+/note")) {
            Long orderId = parseIdAt(path, 2);
            Map<String, String> body = JsonUtil.fromJson(req.getReader(), Map.class);
            writeJson(resp, ApiResponse.ok(staffService.updateInternalNote(orderId, body.get("note")), "Cập nhật ghi chú thành công"));
        } else if (path.matches("/ingredients/\\d+/stock-in")) {
            Long ingredientId = parseIdAt(path, 2);
            Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
            BigDecimal quantity = new BigDecimal(body.get("quantity").toString());
            staffService.stockIn(ingredientId, quantity, userId);
            writeJson(resp, ApiResponse.ok(null, "Nhập kho thành công"));
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
