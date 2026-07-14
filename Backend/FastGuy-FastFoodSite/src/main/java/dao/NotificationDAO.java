package dao;

import java.util.List;

import entity.Notification;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

public class NotificationDAO {
    public void save(Notification notification) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(notification);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException(e);
        } finally {
            em.close();
        }
    }

    public List<Notification> findForUser(int userId, String roleName, int limit) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT n FROM Notification n WHERE n.userId = :userId OR n.roleName = :roleName ORDER BY n.createdAt DESC",
                    Notification.class)
                    .setParameter("userId", userId)
                    .setParameter("roleName", roleName)
                    .setMaxResults(limit)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public long countUnread(int userId, String roleName) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(n) FROM Notification n WHERE (n.userId = :userId OR n.roleName = :roleName) AND n.isRead = false",
                    Long.class)
                    .setParameter("userId", userId)
                    .setParameter("roleName", roleName)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public boolean markRead(int id, int userId, String roleName) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Notification n = em.find(Notification.class, id);
            if (n == null || !canAccess(n, userId, roleName)) {
                em.getTransaction().rollback();
                return false;
            }
            n.setIsRead(true);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException(e);
        } finally {
            em.close();
        }
    }

    public void markAllRead(int userId, String roleName) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId OR n.roleName = :roleName")
                    .setParameter("userId", userId)
                    .setParameter("roleName", roleName)
                    .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException(e);
        } finally {
            em.close();
        }
    }

    private boolean canAccess(Notification n, int userId, String roleName) {
        return (n.getUserId() != null && n.getUserId() == userId) || (n.getRoleName() != null && n.getRoleName().equals(roleName));
    }
}
