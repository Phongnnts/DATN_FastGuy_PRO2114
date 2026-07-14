package service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dao.NotificationDAO;
import entity.Notification;

public class NotificationService {
    private NotificationDAO notificationDAO = new NotificationDAO();

    public void notifyUser(Integer userId, String title, String message, String type, String targetUrl) {
        create(userId, null, title, message, type, targetUrl);
    }

    public void notifyRole(String roleName, String title, String message, String type, String targetUrl) {
        create(null, roleName, title, message, type, targetUrl);
    }

    public Map<String, Object> getForUser(int userId, String roleName) {
        List<Map<String, Object>> items = notificationDAO.findForUser(userId, roleName, 20).stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("unreadCount", notificationDAO.countUnread(userId, roleName));
        return result;
    }

    public boolean markRead(int id, int userId, String roleName) {
        return notificationDAO.markRead(id, userId, roleName);
    }

    public void markAllRead(int userId, String roleName) {
        notificationDAO.markAllRead(userId, roleName);
    }

    private void create(Integer userId, String roleName, String title, String message, String type, String targetUrl) {
        try {
            Notification n = new Notification();
            n.setUserId(userId);
            n.setRoleName(roleName);
            n.setTitle(title);
            n.setMessage(message);
            n.setType(type);
            n.setTargetUrl(targetUrl);
            n.setIsRead(false);
            n.setCreatedAt(LocalDateTime.now());
            notificationDAO.save(n);
        } catch (RuntimeException e) {
            // Notifications are secondary to the already-committed business action.
            System.err.println("Unable to create notification: " + e.getMessage());
        }
    }

    private Map<String, Object> toMap(Notification n) {
        Map<String, Object> m = new HashMap<>();
        m.put("notificationId", n.getNotificationId());
        m.put("title", n.getTitle());
        m.put("message", n.getMessage());
        m.put("type", n.getType());
        m.put("targetUrl", n.getTargetUrl());
        m.put("isRead", Boolean.TRUE.equals(n.getIsRead()));
        m.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
        return m;
    }
}
