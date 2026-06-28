package service;

import dao.DeliveryZoneDAO;
import entity.DeliveryZone;
import utils.GhnClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShippingService {
    private GhnClient ghnClient = new GhnClient();
    private DeliveryZoneDAO deliveryZoneDAO = new DeliveryZoneDAO();

    public List<Map<String, Object>> getProvinces() {
        return ghnClient.getProvinces();
    }

    public List<Map<String, Object>> getDistricts(int provinceId) {
        return ghnClient.getDistricts(provinceId);
    }

    public List<Map<String, Object>> getWards(int districtId) {
        return ghnClient.getWards(districtId);
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
