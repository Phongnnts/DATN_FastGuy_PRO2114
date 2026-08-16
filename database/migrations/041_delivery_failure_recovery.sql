USE FastGuyDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51510, 'Run 000_preflight_history.sql first.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '040_production_hardening') THROW 51511, 'Run 040_production_hardening.sql first.', 1;
IF EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '041_delivery_failure_recovery')
BEGIN
    PRINT '041_delivery_failure_recovery already applied; skipped.';
    RETURN;
END;
BEGIN TRY
    BEGIN TRANSACTION;

    IF COL_LENGTH('dbo.Orders', 'delivery_attempt_count') IS NULL ALTER TABLE dbo.Orders ADD delivery_attempt_count int NOT NULL CONSTRAINT DF_Orders_DeliveryAttemptCount DEFAULT 0;
    IF COL_LENGTH('dbo.Orders', 'delivery_attempt_limit') IS NULL ALTER TABLE dbo.Orders ADD delivery_attempt_limit int NOT NULL CONSTRAINT DF_Orders_DeliveryAttemptLimit DEFAULT 2;
    IF COL_LENGTH('dbo.Orders', 'delivery_failure_code') IS NULL ALTER TABLE dbo.Orders ADD delivery_failure_code varchar(30) NULL;
    IF COL_LENGTH('dbo.Orders', 'delivery_failed_at') IS NULL ALTER TABLE dbo.Orders ADD delivery_failed_at datetime2(0) NULL;
    IF COL_LENGTH('dbo.Orders', 'retry_scheduled_at') IS NULL ALTER TABLE dbo.Orders ADD retry_scheduled_at datetime2(0) NULL;
    IF COL_LENGTH('dbo.Orders', 'returned_to_store_at') IS NULL ALTER TABLE dbo.Orders ADD returned_to_store_at datetime2(0) NULL;

    IF OBJECT_ID(N'dbo.CK_Orders_Status', N'C') IS NOT NULL ALTER TABLE dbo.Orders DROP CONSTRAINT CK_Orders_Status;
    ALTER TABLE dbo.Orders WITH CHECK ADD CONSTRAINT CK_Orders_Status CHECK (order_status IN ('PENDING','CONFIRMED','PREPARING','READY','ASSIGNED','PICKED_UP','DELIVERY_FAILED','RETURNED_TO_STORE','DELIVERED','CANCELLED'));

    IF OBJECT_ID(N'dbo.CK_OrderStatusHistory_From', N'C') IS NOT NULL ALTER TABLE dbo.OrderStatusHistory DROP CONSTRAINT CK_OrderStatusHistory_From;
    ALTER TABLE dbo.OrderStatusHistory WITH CHECK ADD CONSTRAINT CK_OrderStatusHistory_From CHECK (from_status IS NULL OR from_status IN ('PENDING','CONFIRMED','PREPARING','READY','ASSIGNED','PICKED_UP','DELIVERY_FAILED','RETURNED_TO_STORE','DELIVERED','CANCELLED'));
    IF OBJECT_ID(N'dbo.CK_OrderStatusHistory_To', N'C') IS NOT NULL ALTER TABLE dbo.OrderStatusHistory DROP CONSTRAINT CK_OrderStatusHistory_To;
    ALTER TABLE dbo.OrderStatusHistory WITH CHECK ADD CONSTRAINT CK_OrderStatusHistory_To CHECK (to_status IN ('PENDING','CONFIRMED','PREPARING','READY','ASSIGNED','PICKED_UP','DELIVERY_FAILED','RETURNED_TO_STORE','DELIVERED','CANCELLED'));

    IF OBJECT_ID(N'dbo.CK_Orders_DeliveryAttempts', N'C') IS NOT NULL ALTER TABLE dbo.Orders DROP CONSTRAINT CK_Orders_DeliveryAttempts;
    ALTER TABLE dbo.Orders WITH CHECK ADD CONSTRAINT CK_Orders_DeliveryAttempts CHECK (delivery_attempt_count >= 0 AND delivery_attempt_limit > 0 AND delivery_attempt_count <= delivery_attempt_limit);
    IF OBJECT_ID(N'dbo.CK_Orders_DeliveryFailureCode', N'C') IS NOT NULL ALTER TABLE dbo.Orders DROP CONSTRAINT CK_Orders_DeliveryFailureCode;
    ALTER TABLE dbo.Orders WITH CHECK ADD CONSTRAINT CK_Orders_DeliveryFailureCode CHECK (delivery_failure_code IS NULL OR delivery_failure_code IN ('CUSTOMER_UNREACHABLE','INVALID_ADDRESS','CUSTOMER_RESCHEDULED','CUSTOMER_REJECTED','SHIPPER_INCIDENT','PRODUCT_INCIDENT'));

    INSERT dbo.SchemaMigrationHistory(migration_id, details) VALUES ('041_delivery_failure_recovery', N'Delivery attempt metadata, failure codes, and recovery statuses added');
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
