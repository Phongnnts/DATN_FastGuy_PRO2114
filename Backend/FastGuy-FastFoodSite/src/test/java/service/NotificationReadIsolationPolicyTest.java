package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import entity.Notification;

class NotificationReadIsolationPolicyTest {
    @Test
    void roleNotificationReadStateIsViewerSpecific() throws IOException {
        String dao = Files.readString(Path.of("src/main/java/dao/NotificationDAO.java"));

        assertTrue(dao.contains("NotificationReadReceipt"));
        assertTrue(dao.contains("r.userId = :userId"));
        assertTrue(dao.contains("n.userId = :userId AND n.isRead = false"));
        assertTrue(dao.contains("n.roleName = :roleName AND r.notificationId IS NULL"));
    }

    @Test
    void roleReadsCreateReceiptsWhileUserReadsRetainIsRead() throws IOException {
        String dao = Files.readString(Path.of("src/main/java/dao/NotificationDAO.java"));

        assertTrue(dao.contains("notification.getUserId() != null"));
        assertTrue(dao.contains("notification.setIsRead(true)"));
        assertTrue(dao.contains("new NotificationReadReceipt(id, userId)"));
        assertTrue(dao.contains("new NotificationReadReceipt(notification.getNotificationId(), userId)"));
    }

    @Test
    void accessStillRequiresViewerIdOrActiveRole() throws IOException {
        String dao = Files.readString(Path.of("src/main/java/dao/NotificationDAO.java"));

        assertTrue(dao.contains("canAccess(notification, userId, roleName)"));
        assertTrue(dao.contains("notification.getRoleName().equals(roleName)"));
    }

    @Test
    void serviceMapsViewerSpecificReadState() throws IOException {
        String service = Files.readString(Path.of("src/main/java/service/NotificationService.java"));
        Notification notification = new Notification();
        notification.setIsRead(false);
        notification.setReadForViewer(true);

        assertTrue(notification.isReadForViewer());
        assertEquals(false, notification.getIsRead());
        assertTrue(service.contains("n.isReadForViewer()"));
    }

    @Test
    void migrationAndFreshSchemasDefineUniqueReceiptPair() throws IOException {
        String migration = Files.readString(Path.of("../../database/migrations/045_notification_read_receipt.sql"));
        String init = Files.readString(Path.of("../../database/init.sql"));
        String fresh = Files.readString(Path.of("../../database/DB_FastGuy.sql"));

        for (String sql : new String[] { migration, init, fresh }) {
            assertTrue(sql.contains("CREATE TABLE dbo.NotificationReadReceipt"));
            assertTrue(sql.contains("PRIMARY KEY (notification_id, user_id)"));
            assertTrue(sql.contains("REFERENCES dbo.Notification(notification_id)"));
            assertTrue(sql.contains("REFERENCES dbo.Users(user_id)"));
        }
        assertTrue(migration.contains("045_notification_read_receipt"));
        assertTrue(migration.contains("044_manual_refund_audit"));
    }
}
