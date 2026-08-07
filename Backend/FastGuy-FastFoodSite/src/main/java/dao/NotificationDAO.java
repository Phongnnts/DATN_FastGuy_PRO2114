package dao;

import java.util.List;

import entity.Notification;
import entity.NotificationReadReceipt;
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
            List<Object[]> rows = em.createQuery(
                    "SELECT n, CASE WHEN n.userId = :userId THEN n.isRead WHEN r.notificationId IS NOT NULL THEN true ELSE false END "
                            + "FROM Notification n LEFT JOIN NotificationReadReceipt r ON r.notificationId = n.notificationId AND r.userId = :userId "
                            + "WHERE n.userId = :userId OR n.roleName = :roleName ORDER BY n.createdAt DESC",
                    Object[].class)
                    .setParameter("userId", userId)
                    .setParameter("roleName", roleName)
                    .setMaxResults(limit)
                    .getResultList();
            return rows.stream().map(row -> {
                Notification notification = (Notification) row[0];
                notification.setReadForViewer(Boolean.TRUE.equals(row[1]));
                return notification;
            }).toList();
        } finally {
            em.close();
        }
    }

    public long countUnread(int userId, String roleName) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(n) FROM Notification n LEFT JOIN NotificationReadReceipt r ON r.notificationId = n.notificationId AND r.userId = :userId "
                            + "WHERE (n.userId = :userId AND n.isRead = false) OR (n.roleName = :roleName AND r.notificationId IS NULL)",
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
            Notification notification = em.find(Notification.class, id);
            if (notification == null || !canAccess(notification, userId, roleName)) {
                em.getTransaction().rollback();
                return false;
            }
            if (notification.getUserId() != null) {
                notification.setIsRead(true);
            } else if (em.find(NotificationReadReceipt.class, new NotificationReadReceipt.Key(id, userId)) == null) {
                em.persist(new NotificationReadReceipt(id, userId));
            }
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
            em.createQuery("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId")
                    .setParameter("userId", userId)
                    .executeUpdate();
            List<Notification> unreadRoleNotifications = em.createQuery(
                    "SELECT n FROM Notification n LEFT JOIN NotificationReadReceipt r ON r.notificationId = n.notificationId AND r.userId = :userId "
                            + "WHERE n.roleName = :roleName AND r.notificationId IS NULL",
                    Notification.class)
                    .setParameter("userId", userId)
                    .setParameter("roleName", roleName)
                    .getResultList();
            for (Notification notification : unreadRoleNotifications) {
                em.persist(new NotificationReadReceipt(notification.getNotificationId(), userId));
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException(e);
        } finally {
            em.close();
        }
    }

    private boolean canAccess(Notification notification, int userId, String roleName) {
        return (notification.getUserId() != null && notification.getUserId() == userId)
                || (notification.getRoleName() != null && notification.getRoleName().equals(roleName));
    }
}
