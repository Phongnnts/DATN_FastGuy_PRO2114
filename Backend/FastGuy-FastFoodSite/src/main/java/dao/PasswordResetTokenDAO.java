package dao;

import java.time.LocalDateTime;

import entity.PasswordResetToken;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

public class PasswordResetTokenDAO {
    public void replaceForUser(PasswordResetToken token) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM PasswordResetToken t WHERE t.user.userId = :userId")
                    .setParameter("userId", token.getUser().getUserId())
                    .executeUpdate();
            em.persist(token);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public PasswordResetToken findUsableByHash(String tokenHash) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT t FROM PasswordResetToken t WHERE t.tokenHash = :tokenHash AND t.usedAt IS NULL AND t.expiresAt > :now", PasswordResetToken.class)
                    .setParameter("tokenHash", tokenHash)
                    .setParameter("now", LocalDateTime.now())
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        } finally {
            em.close();
        }
    }

    public void consumeAndUpdatePassword(int tokenId, String passwordHash) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            PasswordResetToken token = em.find(PasswordResetToken.class, tokenId);
            if (token == null || token.getUsedAt() != null || !token.getExpiresAt().isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
            }
            token.getUser().setPasswordHash(passwordHash);
            token.setUsedAt(LocalDateTime.now());
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
