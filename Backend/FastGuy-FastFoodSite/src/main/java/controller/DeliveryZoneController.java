package controller;

import dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.DeliveryZoneService;
import utils.JsonUtil;

import java.io.IOException;

@WebServlet("/api/delivery-zones")
public class DeliveryZoneController extends HttpServlet {
    private final DeliveryZoneService deliveryZoneService = new DeliveryZoneService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json; charset=UTF-8");
        writeJson(resp, ApiResponse.ok(deliveryZoneService.getAllActive()));
    }

    private void writeJson(HttpServletResponse resp, Object data) throws IOException {
        resp.getWriter().write(JsonUtil.toJson(data));
    }
}
