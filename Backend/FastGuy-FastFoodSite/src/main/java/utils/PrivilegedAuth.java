package utils;

import entity.User;
import jakarta.persistence.EntityManager;

public final class PrivilegedAuth {

    private PrivilegedAuth() {}

    public static boolean isActiveRole(int userId, String role) {
        if (userId < 0 || role == null) return false;
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            User user = em.find(User.class, userId);
            return (
                user != null &&
                role.equals(user.getRole()) &&
                "ACTIVE".equals(user.getStatus())
            );
        } finally {
            em.close();
        }
    }
}
