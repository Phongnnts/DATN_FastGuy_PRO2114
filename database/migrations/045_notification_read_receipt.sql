USE FastGuyDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51450, 'Run 000_preflight_history.sql first.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '044_manual_refund_audit') THROW 51451, 'Run 044_manual_refund_audit.sql first.', 1;
IF EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '045_notification_read_receipt')
    PRINT '045_notification_read_receipt already applied.';
ELSE
BEGIN
    BEGIN TRY
        BEGIN TRANSACTION;
        IF OBJECT_ID(N'dbo.NotificationReadReceipt', N'U') IS NULL
            CREATE TABLE dbo.NotificationReadReceipt (
                notification_id int NOT NULL,
                user_id int NOT NULL,
                read_at datetime2(0) NOT NULL CONSTRAINT DF_NotificationReadReceipt_ReadAt DEFAULT GETDATE(),
                CONSTRAINT PK_NotificationReadReceipt PRIMARY KEY (notification_id, user_id),
                CONSTRAINT FK_NotificationReadReceipt_Notification FOREIGN KEY (notification_id) REFERENCES dbo.Notification(notification_id) ON DELETE CASCADE,
                CONSTRAINT FK_NotificationReadReceipt_User FOREIGN KEY (user_id) REFERENCES dbo.Users(user_id)
            );
        INSERT dbo.SchemaMigrationHistory(migration_id, details)
        VALUES ('045_notification_read_receipt', N'Added viewer-specific read receipts for role notifications');
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
