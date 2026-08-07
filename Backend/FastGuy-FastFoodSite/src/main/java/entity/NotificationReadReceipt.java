package entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@IdClass(NotificationReadReceipt.Key.class)
@Table(name = "NotificationReadReceipt")
public class NotificationReadReceipt {
    @Id
    @Column(name = "notification_id")
    private int notificationId;

    @Id
    @Column(name = "user_id")
    private int userId;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public NotificationReadReceipt() {}

    public NotificationReadReceipt(int notificationId, int userId) {
        this.notificationId = notificationId;
        this.userId = userId;
    }

    @PrePersist
    void prePersist() {
        if (readAt == null) readAt = LocalDateTime.now();
    }

    public int getNotificationId() { return notificationId; }
    public int getUserId() { return userId; }
    public LocalDateTime getReadAt() { return readAt; }

    public static class Key implements Serializable {
        private int notificationId;
        private int userId;

        public Key() {}

        public Key(int notificationId, int userId) {
            this.notificationId = notificationId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object value) {
            if (this == value) return true;
            if (!(value instanceof Key other)) return false;
            return notificationId == other.notificationId && userId == other.userId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(notificationId, userId);
        }
    }
}
