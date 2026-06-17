package utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class DatabaseUtil {
    private static EntityManagerFactory factory;

    public static EntityManager getEntityManager() {
        if (factory == null) {
            factory = Persistence.createEntityManagerFactory("FastGuyPU");
        }
        return factory.createEntityManager();
    }

    public static void close() {
        if (factory != null) {
            factory.close();
        }
    }
}
