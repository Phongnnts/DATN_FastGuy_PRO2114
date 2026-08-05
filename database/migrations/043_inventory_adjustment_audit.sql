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
IF EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '043_inventory_adjustment_audit')
    PRINT '043_inventory_adjustment_audit already applied.';
ELSE
BEGIN
    BEGIN TRY
        BEGIN TRANSACTION;
        IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51430, 'Run 000_preflight_history.sql first.', 1;

        -- Make order_id nullable so manual adjustments/waste are not tied to an order.
        DECLARE @fk sysname = (
            SELECT TOP 1 fk.name
            FROM sys.foreign_keys fk
            WHERE fk.parent_object_id = OBJECT_ID(N'dbo.InventoryTransaction')
              AND fk.referenced_object_id = OBJECT_ID(N'dbo.Orders')
        );
        IF @fk IS NOT NULL
        BEGIN
            DECLARE @sql nvarchar(500) = N'ALTER TABLE dbo.InventoryTransaction DROP CONSTRAINT ' + QUOTENAME(@fk);
            EXEC(@sql);
        END
        EXEC(N'ALTER TABLE dbo.InventoryTransaction ALTER COLUMN order_id int NULL');
        IF OBJECT_ID(N'dbo.FK_InventoryTransaction_Order', N'F') IS NULL
            ALTER TABLE dbo.InventoryTransaction WITH CHECK
            ADD CONSTRAINT FK_InventoryTransaction_Order
            FOREIGN KEY (order_id) REFERENCES dbo.Orders(order_id);

        -- Audit fields for manual adjustments / waste.
        IF COL_LENGTH('dbo.InventoryTransaction', 'created_by') IS NULL
            EXEC(N'ALTER TABLE dbo.InventoryTransaction ADD created_by int NULL');
        IF COL_LENGTH('dbo.InventoryTransaction', 'reason_code') IS NULL
            EXEC(N'ALTER TABLE dbo.InventoryTransaction ADD reason_code varchar(50) NULL');
        IF COL_LENGTH('dbo.InventoryTransaction', 'note') IS NULL
            EXEC(N'ALTER TABLE dbo.InventoryTransaction ADD note nvarchar(500) NULL');
        IF COL_LENGTH('dbo.InventoryTransaction', 'quantity_before') IS NULL
            EXEC(N'ALTER TABLE dbo.InventoryTransaction ADD quantity_before int NULL');
        IF COL_LENGTH('dbo.InventoryTransaction', 'quantity_after') IS NULL
            EXEC(N'ALTER TABLE dbo.InventoryTransaction ADD quantity_after int NULL');

        IF OBJECT_ID(N'dbo.FK_InventoryTransaction_CreatedBy', N'F') IS NULL
            ALTER TABLE dbo.InventoryTransaction WITH CHECK
            ADD CONSTRAINT FK_InventoryTransaction_CreatedBy
            FOREIGN KEY (created_by) REFERENCES dbo.Users(user_id);

        IF NOT EXISTS (
            SELECT 1 FROM sys.indexes
            WHERE object_id = OBJECT_ID(N'dbo.InventoryTransaction') AND name = N'IX_InventoryTransaction_CreatedBy'
        )
            CREATE INDEX IX_InventoryTransaction_CreatedBy ON dbo.InventoryTransaction(created_by);

        INSERT dbo.SchemaMigrationHistory(migration_id, details)
        VALUES ('043_inventory_adjustment_audit', N'InventoryTransaction order_id nullable; added created_by, reason_code, note, quantity_before, quantity_after for manual adjustments and waste');
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
