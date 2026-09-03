package utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DatabaseUtil {

    private static EntityManagerFactory factory;

    public static synchronized EntityManager getEntityManager() {
        if (factory == null) {
            Map<String, String> overrides = connectionOverrides(
                AppConfig.getDbUrl(),
                AppConfig::getDbUser,
                AppConfig::getDbPassword
            );
            factory = Persistence.createEntityManagerFactory(
                "FastGuyPU",
                overrides.isEmpty() ? null : overrides
            );
        }
        return factory.createEntityManager();
    }

    static Map<String, String> connectionOverrides(
        String configuredUrl,
        Supplier<String> user,
        Supplier<String> password
    ) {
        Map<String, String> overrides = new HashMap<>();
        String url = sqlServerTimeUrl(configuredUrl);
        overrides.put("jakarta.persistence.jdbc.url", url);
        if (
            !configuredUrl
                .toLowerCase(java.util.Locale.ROOT)
                .contains("integratedsecurity=true")
        ) {
            overrides.put("jakarta.persistence.jdbc.user", user.get());
            overrides.put("jakarta.persistence.jdbc.password", password.get());
        }
        return overrides;
    }

    static String sqlServerTimeUrl(String url) {
        if (url.toLowerCase().contains("sendtimeasdatetime=")) {
            return url.replaceAll(
                "(?i)sendTimeAsDatetime=[^;]*",
                "sendTimeAsDatetime=false"
            );
        }
        return (
            url + (url.endsWith(";") ? "" : ";") + "sendTimeAsDatetime=false"
        );
    }

    public static void close() {
        if (factory != null) {
            factory.close();
        }
    }
}
