package utils;

public class AppConfig {
    private static final String GHN_TOKEN;
    private static final String GHN_SHOP_ID;
    private static final String GHN_HOST;

    static {
        GHN_TOKEN = System.getenv("GHN_TOKEN") != null ? System.getenv("GHN_TOKEN") : "";
        GHN_SHOP_ID = System.getenv("GHN_SHOP_ID") != null ? System.getenv("GHN_SHOP_ID") : "";
        GHN_HOST = "https://dev-online-gateway.ghn.vn";
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
}
