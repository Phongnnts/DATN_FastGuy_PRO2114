package utils;

import java.util.Map;
import java.util.regex.Pattern;

public class AddressValidator {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)(3|5|7|8|9)[0-9]{8}$");

    public static String validate(Map<String, Object> body) {
        String recipientName = body.get("recipientName") instanceof String ? ((String) body.get("recipientName")).trim() : "";
        String phone = body.get("phone") instanceof String ? ((String) body.get("phone")).trim() : "";
        String street = body.get("street") instanceof String ? ((String) body.get("street")).trim() : "";
        String wardName = body.get("wardName") instanceof String ? ((String) body.get("wardName")).trim() : "";
        String districtName = body.get("districtName") instanceof String ? ((String) body.get("districtName")).trim() : "";
        String provinceName = body.get("provinceName") instanceof String ? ((String) body.get("provinceName")).trim() : "";

        if (recipientName.length() < 2 || recipientName.length() > 100) {
            return "Ten nguoi nhan phai tu 2 den 100 ky tu";
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return "So dien khong khong hop le";
        }
        if (street.length() < 5 || street.length() > 255) {
            return "So nha/ten duong phai tu 5 den 255 ky tu";
        }
        if (wardName.isEmpty()) {
            return "Phuong/xa khong duoc de trong";
        }
        if (districtName.isEmpty()) {
            return "Quan/huyen khong duoc de trong";
        }
        if (provinceName.isEmpty()) {
            return "Tinh/thanh pho khong duoc de trong";
        }
        if (!(body.get("ghnProvinceId") instanceof Number) || ((Number) body.get("ghnProvinceId")).intValue() <= 0) {
            return "Tinh/thanh pho GHN khong hop le";
        }
        if (!(body.get("ghnDistrictId") instanceof Number) || ((Number) body.get("ghnDistrictId")).intValue() <= 0) {
            return "Quan/huyen GHN khong hop le";
        }
        String wardCode = body.get("ghnWardCode") instanceof String ? ((String) body.get("ghnWardCode")).trim() : "";
        if (wardCode.isEmpty() || wardCode.length() > 20) {
            return "Phuong/xa GHN khong hop le";
        }
        return null;
    }
}
