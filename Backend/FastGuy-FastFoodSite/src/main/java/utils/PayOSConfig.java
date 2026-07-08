package utils;

public class PayOSConfig {
    private static final String CLIENT_ID = System.getenv("PAYOS_CLIENT_ID");
    private static final String API_KEY = System.getenv("PAYOS_API_KEY");
    private static final String CHECKSUM_KEY = System.getenv("PAYOS_CHECKSUM_KEY");
    private static final String FRONTEND_URL;

    static {
        String url = System.getenv("FRONTEND_BASE_URL");
        if (url == null || url.isBlank()) {
            url = "http://localhost:5173";
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        FRONTEND_URL = url;
    }

    public static String getClientId() { return CLIENT_ID; }
    public static String getApiKey() { return API_KEY; }
    public static String getChecksumKey() { return CHECKSUM_KEY; }
    public static String getFrontendUrl() { return FRONTEND_URL; }

    public static boolean isConfigured() {
        return CLIENT_ID != null && !CLIENT_ID.isBlank()
            && API_KEY != null && !API_KEY.isBlank()
            && CHECKSUM_KEY != null && !CHECKSUM_KEY.isBlank();
    }

    public static String getReturnUrl() {
        return FRONTEND_URL + "/payment-return";
    }
}
