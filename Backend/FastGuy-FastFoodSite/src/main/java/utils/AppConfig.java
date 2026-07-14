package utils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class AppConfig {
    private static final Properties LOCAL_ENV = loadLocalEnv();
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

    public static String getPayosClientId() {
        return env("PAYOS_CLIENT_ID");
    }

    public static String getPayosApiKey() {
        return env("PAYOS_API_KEY");
    }

    public static String getPayosChecksumKey() {
        return env("PAYOS_CHECKSUM_KEY");
    }

    public static String getAppWebUrl() {
        String value = env("APP_WEB_URL");
        return value.isBlank() ? "http://localhost:5173" : value.replaceAll("/+$", "");
    }

    private static String env(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) value = LOCAL_ENV.getProperty(name);
        return value != null ? value.trim() : "";
    }

    private static Properties loadLocalEnv() {
        Properties properties = new Properties();
        Path path = Path.of(System.getProperty("user.dir"), ".env");
        if (!Files.isRegularFile(path)) {
            try {
                path = Path.of(AppConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                        .getParent().getParent().resolve(".env");
            } catch (Exception ignored) {
                return properties;
            }
        }
        if (!Files.isRegularFile(path)) return properties;
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        } catch (Exception ignored) {
        }
        return properties;
    }
}
