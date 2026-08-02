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
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51350, 'Run 000_preflight_history.sql first.', 1;
IF EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '035_backend_hardening')
BEGIN
    PRINT '035_backend_hardening already applied; skipped.';
    RETURN;
END;
BEGIN TRY
    BEGIN TRANSACTION;
    ;WITH RankedDefaults AS (
        SELECT address_id, ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at DESC, address_id DESC) rn
        FROM dbo.Address
        WHERE is_default = 1
    )
    UPDATE a SET is_default = 0 FROM dbo.Address a JOIN RankedDefaults r ON r.address_id=a.address_id WHERE r.rn>1;
    ;WITH MissingDefaults AS (
        SELECT address_id, ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at DESC, address_id DESC) rn
        FROM dbo.Address a
        WHERE NOT EXISTS (SELECT 1 FROM dbo.Address d WHERE d.user_id=a.user_id AND d.is_default=1)
    )
    UPDATE a SET is_default=1 FROM dbo.Address a JOIN MissingDefaults m ON m.address_id=a.address_id WHERE m.rn=1;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.Address') AND name=N'UX_Address_Default') CREATE UNIQUE INDEX UX_Address_Default ON dbo.Address(user_id) WHERE is_default=1;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.InventoryReservation') AND name=N'IX_InventoryReservation_Variant') CREATE INDEX IX_InventoryReservation_Variant ON dbo.InventoryReservation(variant_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.InventoryTransaction') AND name=N'IX_InventoryTransaction_Order') CREATE INDEX IX_InventoryTransaction_Order ON dbo.InventoryTransaction(order_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.InventoryTransaction') AND name=N'IX_InventoryTransaction_Variant') CREATE INDEX IX_InventoryTransaction_Variant ON dbo.InventoryTransaction(variant_id);
    INSERT dbo.SchemaMigrationHistory(migration_id,details) VALUES ('035_backend_hardening',N'Address defaults normalized; address and inventory indexes applied');
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
