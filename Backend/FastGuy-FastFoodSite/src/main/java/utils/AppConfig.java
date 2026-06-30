package utils;

public class AppConfig {
    private static final String GHN_TOKEN;
    private static final String GHN_SHOP_ID;
    private static final String GHN_HOST;

    static {
        GHN_TOKEN = System.getenv("GHN_TOKEN") != null ? System.getenv("GHN_TOKEN") : "e956f31b-713a-11f1-bcbb-caab4368df61";
        GHN_SHOP_ID = System.getenv("GHN_SHOP_ID") != null ? System.getenv("GHN_SHOP_ID") : "6513527";
        GHN_HOST = "https://online-gateway.ghn.vn";
    }

    public static String getGhnToken() {
        return GHN_TOKEN;
    }

    public static String getGhnShopId() {
        return GHN_SHOP_ID;
    }

    public static String getGhnHost() {
        return GHN_HOST;
    }

    public static String getGhnFromDistrictId() {
        return System.getenv("GHN_FROM_DISTRICT_ID") != null ? System.getenv("GHN_FROM_DISTRICT_ID") : "1442";
    }

    public static String getGhnFromWardCode() {
        return System.getenv("GHN_FROM_WARD_CODE") != null ? System.getenv("GHN_FROM_WARD_CODE") : "20107";
    }

    public static int getDefaultServiceTypeId() {
        try {
            return Integer.parseInt(System.getenv("GHN_DEFAULT_SERVICE_TYPE"));
        } catch (Exception e) {
            return 2;
        }
    }
}
