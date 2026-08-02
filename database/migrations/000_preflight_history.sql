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
IF DB_NAME() <> N'FastGuyDB' THROW 51000, 'Wrong database. Expected FastGuyDB.', 1;
IF CAST(SERVERPROPERTY('ProductMajorVersion') AS int) < 13 THROW 51001, 'SQL Server 2016 or newer is required for JSON functions.', 1;
IF NOT EXISTS (SELECT 1 FROM sys.databases WHERE database_id = DB_ID() AND state_desc = 'ONLINE') THROW 51002, 'Database must be online.', 1;
IF OBJECT_ID(N'dbo.Users', N'U') IS NULL OR OBJECT_ID(N'dbo.Product', N'U') IS NULL THROW 51003, 'Unsupported source schema: Users or Product is missing.', 1;
IF NOT EXISTS (SELECT 1 FROM msdb.dbo.backupset WHERE database_name = DB_NAME() AND type = 'D' AND backup_finish_date >= DATEADD(day, -7, GETDATE())) THROW 51004, 'A successful full backup from the last 7 days is required.', 1;
GO
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL
        CREATE TABLE dbo.SchemaMigrationHistory (
            migration_id varchar(100) NOT NULL CONSTRAINT PK_SchemaMigrationHistory PRIMARY KEY,
            applied_at datetime2(0) NOT NULL CONSTRAINT DF_SchemaMigrationHistory_AppliedAt DEFAULT SYSUTCDATETIME(),
            applied_by sysname NOT NULL CONSTRAINT DF_SchemaMigrationHistory_AppliedBy DEFAULT ORIGINAL_LOGIN(),
            details nvarchar(1000) NULL
        );
    IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '000_preflight_history')
        INSERT dbo.SchemaMigrationHistory(migration_id, details) VALUES ('000_preflight_history', N'Backup verified; migration history initialized');
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
PRINT 'Preflight passed. Keep the verified backup until post-migration acceptance is complete.';
