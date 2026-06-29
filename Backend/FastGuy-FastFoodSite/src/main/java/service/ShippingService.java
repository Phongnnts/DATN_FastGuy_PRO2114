package service;

import dao.DeliveryZoneDAO;
import dao.OrdersDAO;
import entity.DeliveryZone;
import entity.Orders;
import utils.GhnClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShippingService {
    private GhnClient ghnClient = new GhnClient();
    private DeliveryZoneDAO deliveryZoneDAO = new DeliveryZoneDAO();
    private OrdersDAO ordersDAO = new OrdersDAO();

    public List<Map<String, Object>> getProvinces() {
        return ghnClient.getProvinces();
    }

    public List<Map<String, Object>> getDistricts(int provinceId) {
        return ghnClient.getDistricts(provinceId);
    }

    public List<Map<String, Object>> getWards(int districtId) {
        return ghnClient.getWards(districtId);
    }

    public List<Map<String, Object>> getServices(int toDistrictId) {
        return ghnClient.getServices(toDistrictId);
    }

    public Map<String, Object> calculateFee(int toDistrictId, String toWardCode, int weight, int length, int width, int height) {
        try {
            Map<String, Object> result = ghnClient.calculateFee(toDistrictId, toWardCode, weight, length, width, height);
            if (result != null && !result.containsKey("error")) {
                result.put("shippingProvider", "GHN");
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fallbackFee();
    }

    public Map<String, Object> createGHNOrder(int orderId) {
        Orders order = ordersDAO.findById(orderId);
        if (order == null) return Map.of("error", "Order not found");

        Map<String, Object> result = ghnClient.createOrder(
                order.getGhnDistrictId() != null ? order.getGhnDistrictId() : 1442,
                order.getGhnWardCode() != null ? order.getGhnWardCode() : "",
                order.getCustomerAddress() != null ? order.getCustomerAddress() : "",
                order.getCustomerPhone() != null ? order.getCustomerPhone() : "",
                order.getCustomerName() != null ? order.getCustomerName() : "Khach hang",
                500, 20, 20, 10, 2, 0, order.getDeliveryNote());

        if (result.containsKey("orderCode")) {
            order.setGhnOrderCode((String) result.get("orderCode"));
            order.setGhnStatus("created");
            ordersDAO.save(order);
            result.put("trackingUrl", "https://donhang.ghn.vn/?order_code=" + result.get("orderCode"));
        }
        return result;
    }

    public Map<String, Object> trackGHNOrder(String ghnCode) {
        return ghnClient.getOrderStatus(ghnCode);
    }

    public boolean cancelGHNOrder(String ghnOrderCode) {
        return ghnClient.cancelOrder(ghnOrderCode);
    }

    private Map<String, Object> fallbackFee() {
        Map<String, Object> fallback = new HashMap<>();
        try {
            List<DeliveryZone> zones = deliveryZoneDAO.findAllActive();
            if (zones != null && !zones.isEmpty()) {
                DeliveryZone zone = zones.get(0);
                fallback.put("shippingFee", zone.getShippingFee() != null
                        ? zone.getShippingFee().doubleValue() : 15000);
            } else {
                fallback.put("shippingFee", 15000);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fallback.put("shippingFee", 15000);
        }
        fallback.put("shippingProvider", "FALLBACK_ZONE");
        fallback.put("expectedDeliveryTime", null);
        return fallback;
    }
}
