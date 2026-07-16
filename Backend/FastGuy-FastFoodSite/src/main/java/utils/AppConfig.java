package utils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class AppConfig {
    private static final Properties LOCAL_ENV = loadLocalEnv();
    private static final boolean PROD = "true".equalsIgnoreCase(System.getProperty("prod"));

    private static final String JWT_SECRET;
    private static final String GHN_TOKEN;
    private static final String GHN_SHOP_ID;
    private static final String GHN_HOST;

    static {
        JWT_SECRET = requireEnv("JWT_SECRET");
        GHN_TOKEN = env("GHN_TOKEN", "");
        GHN_SHOP_ID = env("GHN_SHOP_ID", "");
        GHN_HOST = env("GHN_HOST", "https://online-gateway.ghn.vn");
    }

    public static String getJwtSecret() {
        return JWT_SECRET;
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
        return env("GHN_FROM_DISTRICT_ID", "1442");
    }

    public static String getGhnFromWardCode() {
        return env("GHN_FROM_WARD_CODE", "20107");
    }

    public static int getDefaultServiceTypeId() {
        try {
            return Integer.parseInt(env("GHN_DEFAULT_SERVICE_TYPE", "2"));
        } catch (NumberFormatException e) {
            return 2;
        }
    }

    public static String getPayosClientId() {
        return env("PAYOS_CLIENT_ID", "");
    }

    public static String getPayosApiKey() {
        return env("PAYOS_API_KEY", "");
    }

    public static String getPayosChecksumKey() {
        return env("PAYOS_CHECKSUM_KEY", "");
    }

    public static String getAppWebUrl() {
        String value = env("APP_WEB_URL", "http://localhost:5173");
        return value.replaceAll("/+$", "");
    }

    public static String getDbUrl() {
        return env("DB_URL", "jdbc:sqlserver://localhost:1433;databaseName=FastGuyDB;encrypt=false;sendStringParametersAsUnicode=true");
    }

    public static String getDbUser() {
        return env("DB_USER", "JavaDuAn");
    }

    public static String getDbPassword() {
        return env("DB_PASSWORD", "");
    }

    public static boolean isProduction() {
        return PROD;
    }

    private static String requireEnv(String name) {
        String value = env(name, "");
        if (value.isEmpty()) {
            if (PROD) {
                throw new IllegalStateException("Required env var " + name + " is not set. Cannot start in production.");
            }
            return "";
        }
        return value;
    }

    private static String env(String name, String defaultVal) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) value = System.getProperty(name);
        if (value == null || value.isBlank()) value = LOCAL_ENV.getProperty(name);
        return (value != null && !value.isBlank()) ? value.trim() : defaultVal;
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
