SET XACT_ABORT ON;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.NotificationReadReceipt', N'U') IS NOT NULL
        DROP TABLE dbo.NotificationReadReceipt;

    IF OBJECT_ID(N'dbo.Notification', N'U') IS NOT NULL
        DROP TABLE dbo.Notification;

    IF OBJECT_ID(N'dbo.SupportTicket', N'U') IS NOT NULL
        DROP TABLE dbo.SupportTicket;

    IF OBJECT_ID(N'dbo.ProductComboItem', N'U') IS NOT NULL
        DROP TABLE dbo.ProductComboItem;

    IF OBJECT_ID(N'dbo.ProductCombo', N'U') IS NOT NULL
        DROP TABLE dbo.ProductCombo;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
