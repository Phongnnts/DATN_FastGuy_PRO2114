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
IF EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '039_inventory_waste_state')
    PRINT '039_inventory_waste_state already applied.';
ELSE
BEGIN
    BEGIN TRY
        BEGIN TRANSACTION;
        IF EXISTS (SELECT 1 FROM dbo.InventoryReservation WHERE status NOT IN ('RESERVED', 'CONSUMED', 'RELEASED', 'WASTED')) THROW 51390, 'Unknown inventory reservation status.', 1;
        IF EXISTS (SELECT 1 FROM dbo.InventoryTransaction WHERE transaction_type NOT IN ('RESERVE', 'RELEASE', 'CONSUME', 'WASTE', 'RETURN', 'ADJUSTMENT')) THROW 51391, 'Unknown inventory transaction type.', 1;
        IF OBJECT_ID(N'dbo.CK_InventoryReservation_Status', N'C') IS NOT NULL ALTER TABLE dbo.InventoryReservation DROP CONSTRAINT CK_InventoryReservation_Status;
        ALTER TABLE dbo.InventoryReservation WITH CHECK ADD CONSTRAINT CK_InventoryReservation_Status CHECK (status IN ('RESERVED', 'CONSUMED', 'RELEASED', 'WASTED'));
        IF OBJECT_ID(N'dbo.CK_InventoryTransaction_Type', N'C') IS NOT NULL ALTER TABLE dbo.InventoryTransaction DROP CONSTRAINT CK_InventoryTransaction_Type;
        ALTER TABLE dbo.InventoryTransaction WITH CHECK ADD CONSTRAINT CK_InventoryTransaction_Type CHECK (transaction_type IN ('RESERVE', 'RELEASE', 'CONSUME', 'WASTE', 'RETURN', 'ADJUSTMENT'));
        INSERT dbo.SchemaMigrationHistory(migration_id, details) VALUES ('039_inventory_waste_state', N'Added terminal WASTED reservation state and WASTE transaction type');
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
