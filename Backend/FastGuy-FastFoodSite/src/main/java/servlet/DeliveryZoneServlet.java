package servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dao.DeliveryZoneDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;

@WebServlet("/api/delivery-zones")
public class DeliveryZoneServlet extends HttpServlet {
    private DeliveryZoneDAO deliveryZoneDAO = new DeliveryZoneDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        List<Map<String, Object>> zones = deliveryZoneDAO.findAllActive()
                .stream()
                .map(z -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("zoneId", z.getZoneId());
                    m.put("districtName", z.getDistrictName());
                    m.put("shippingFee", z.getShippingFee());
                    return m;
                })
                .collect(Collectors.toList());
        ApiResponse.ok(resp, zones);
    }
}
