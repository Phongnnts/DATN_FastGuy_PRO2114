package utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

public class DatabaseUtil {
    private static EntityManagerFactory factory;

    public static synchronized EntityManager getEntityManager() {
        if (factory == null) {
            Map<String, String> overrides = new HashMap<>();
            String url = AppConfig.getDbUrl();
            String user = AppConfig.getDbUser();
            String pass = AppConfig.getDbPassword();
            if (!url.isEmpty()) overrides.put("jakarta.persistence.jdbc.url", url);
            if (!user.isEmpty()) overrides.put("jakarta.persistence.jdbc.user", user);
            if (!pass.isEmpty()) overrides.put("jakarta.persistence.jdbc.password", pass);
            factory = Persistence.createEntityManagerFactory("FastGuyPU",
                    overrides.isEmpty() ? null : overrides);
        }
        return factory.createEntityManager();
    }

    public static void close() {
        if (factory != null) {
            factory.close();
        }
    }
}
