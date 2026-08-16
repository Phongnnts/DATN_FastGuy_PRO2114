USE FastGuyDB;
GO
SET NOCOUNT ON;
GO
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51472, 'SchemaMigrationHistory is missing.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '046_cod_shift_settlement') THROW 51473, '046_cod_shift_settlement is missing.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '047_low_stock_threshold') THROW 51474, '047_low_stock_threshold is missing.', 1;
IF EXISTS (SELECT 1 FROM dbo.ShippingConfig WHERE config_key = 'low_stock_threshold' HAVING COUNT(*) <> 1) OR NOT EXISTS (SELECT 1 FROM dbo.ShippingConfig WHERE config_key = 'low_stock_threshold') THROW 51475, 'low_stock_threshold must exist exactly once.', 1;
IF EXISTS (
    SELECT 1 FROM dbo.ShippingConfig
    WHERE config_key = 'low_stock_threshold'
      AND (TRY_CONVERT(int, config_value) IS NULL OR TRY_CONVERT(int, config_value) NOT BETWEEN 1 AND 1000)
) THROW 51476, 'low_stock_threshold must be an integer between 1 and 1000.', 1;
PRINT '047 low-stock threshold validation passed.';
GO
