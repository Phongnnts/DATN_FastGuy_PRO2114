USE FastGuyDB;
GO
SET NOCOUNT ON;
GO
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51493, 'SchemaMigrationHistory is missing.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '049_category_images') THROW 51494, '049_category_images is missing.', 1;
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Category') AND name = N'image_url'
      AND TYPE_NAME(system_type_id) = N'nvarchar' AND max_length = 2000 AND is_nullable = 1
      AND is_identity = 0 AND is_computed = 0
) THROW 51495, 'Category image_url definition invalid.', 1;
PRINT '049 category images validation passed.';
GO
