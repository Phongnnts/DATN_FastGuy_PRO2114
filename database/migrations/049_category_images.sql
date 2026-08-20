USE FastGuyDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51490, 'Run 000_preflight_history.sql first.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '048_homepage_merchandising') THROW 51491, 'Run 048_homepage_merchandising.sql first.', 1;
IF EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '049_category_images')
    PRINT '049_category_images already applied.';
ELSE
BEGIN
    BEGIN TRY
        BEGIN TRANSACTION;
        IF COL_LENGTH(N'dbo.Category', N'image_url') IS NULL
            ALTER TABLE dbo.Category ADD image_url nvarchar(1000) NULL;
        IF EXISTS (
            SELECT 1 FROM sys.columns
            WHERE object_id = OBJECT_ID(N'dbo.Category') AND name = N'image_url'
              AND (TYPE_NAME(system_type_id) <> N'nvarchar' OR max_length <> 2000 OR is_nullable <> 1 OR is_identity <> 0 OR is_computed <> 0)
        ) THROW 51492, 'Category image_url definition invalid.', 1;
        INSERT dbo.SchemaMigrationHistory(migration_id, details)
        VALUES ('049_category_images', N'Added optional managed image URL to menu categories');
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
