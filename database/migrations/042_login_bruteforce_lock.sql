USE FastGuyDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO
IF EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '042_login_bruteforce_lock')
    PRINT '042_login_bruteforce_lock already applied.';
ELSE
BEGIN
    BEGIN TRY
        BEGIN TRANSACTION;
        IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51420, 'Run 000_preflight_history.sql first.', 1;
        IF COL_LENGTH('dbo.Users', 'failed_login_attempts') IS NULL
            EXEC(N'ALTER TABLE dbo.Users ADD failed_login_attempts int NOT NULL CONSTRAINT DF_Users_FailedLoginAttempts DEFAULT 0');
        IF COL_LENGTH('dbo.Users', 'locked_until') IS NULL
            EXEC(N'ALTER TABLE dbo.Users ADD locked_until datetime2(0) NULL');
        IF OBJECT_ID(N'dbo.CK_Users_FailedLoginAttempts', N'C') IS NOT NULL
            EXEC(N'ALTER TABLE dbo.Users DROP CONSTRAINT CK_Users_FailedLoginAttempts');
        EXEC(N'ALTER TABLE dbo.Users WITH CHECK ADD CONSTRAINT CK_Users_FailedLoginAttempts CHECK (failed_login_attempts >= 0)');
        INSERT dbo.SchemaMigrationHistory(migration_id, details)
        VALUES ('042_login_bruteforce_lock', N'Added failed_login_attempts and locked_until to Users for login brute-force protection');
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
