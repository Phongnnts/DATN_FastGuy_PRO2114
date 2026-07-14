package service;

import utils.GhnClient;

import java.util.List;
import java.util.Map;

public class ShippingService {
    private GhnClient ghnClient = new GhnClient();

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
            if (result != null && result.get("fee") instanceof Number) return result;
            if (result != null && result.containsKey("error")) return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Map.of("error", "Không thể tính phí giao hàng từ GHN");
    }


}
