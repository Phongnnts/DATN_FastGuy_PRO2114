USE FastGuyDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51470, 'Run 000_preflight_history.sql first.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '046_cod_shift_settlement') THROW 51471, 'Run 046_cod_shift_settlement.sql first.', 1;
IF EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '047_low_stock_threshold')
    PRINT '047_low_stock_threshold already applied.';
ELSE
BEGIN
    BEGIN TRY
        BEGIN TRANSACTION;
        IF NOT EXISTS (SELECT 1 FROM dbo.ShippingConfig WHERE config_key = 'low_stock_threshold')
            INSERT dbo.ShippingConfig(config_key, config_value) VALUES ('low_stock_threshold', '5');
        INSERT dbo.SchemaMigrationHistory(migration_id, details)
        VALUES ('047_low_stock_threshold', N'Added persisted shared low-stock threshold default without overwriting existing configuration');
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
