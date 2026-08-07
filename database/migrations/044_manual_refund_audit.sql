USE FastGuyDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51440, 'Run 000_preflight_history.sql first.', 1;
IF EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '044_manual_refund_audit')
    PRINT '044_manual_refund_audit already applied.';
ELSE
BEGIN
    BEGIN TRY
        BEGIN TRANSACTION;
        IF COL_LENGTH('dbo.Orders', 'refund_processed_by') IS NULL
            ALTER TABLE dbo.Orders ADD refund_processed_by int NULL;
        IF COL_LENGTH('dbo.Orders', 'refund_reference') IS NULL
            ALTER TABLE dbo.Orders ADD refund_reference nvarchar(200) NULL;
        IF OBJECT_ID(N'dbo.FK_Orders_RefundProcessedBy', N'F') IS NULL
            ALTER TABLE dbo.Orders WITH CHECK ADD CONSTRAINT FK_Orders_RefundProcessedBy FOREIGN KEY (refund_processed_by) REFERENCES dbo.Users(user_id);
        INSERT dbo.SchemaMigrationHistory(migration_id, details)
        VALUES ('044_manual_refund_audit', N'Added processing admin and manual refund reference audit columns to Orders');
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
