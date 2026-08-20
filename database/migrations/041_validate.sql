USE FastGuyDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
BEGIN TRY
    BEGIN TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '041_delivery_failure_recovery') THROW 51500, 'Migration 041 not applied.', 1;
    IF COL_LENGTH('dbo.Orders','delivery_attempt_count') IS NULL OR COL_LENGTH('dbo.Orders','delivery_attempt_limit') IS NULL OR COL_LENGTH('dbo.Orders','delivery_failure_code') IS NULL OR COL_LENGTH('dbo.Orders','delivery_failed_at') IS NULL OR COL_LENGTH('dbo.Orders','retry_scheduled_at') IS NULL OR COL_LENGTH('dbo.Orders','returned_to_store_at') IS NULL THROW 51501, 'Delivery failure recovery columns missing.', 1;
    IF OBJECT_ID(N'dbo.CK_Orders_Status',N'C') IS NULL OR OBJECT_ID(N'dbo.CK_OrderStatusHistory_From',N'C') IS NULL OR OBJECT_ID(N'dbo.CK_OrderStatusHistory_To',N'C') IS NULL OR OBJECT_ID(N'dbo.CK_Orders_DeliveryAttempts',N'C') IS NULL OR OBJECT_ID(N'dbo.CK_Orders_DeliveryFailureCode',N'C') IS NULL THROW 51502, 'Delivery failure recovery constraints missing.', 1;
    IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE object_id IN (OBJECT_ID(N'dbo.CK_Orders_Status'),OBJECT_ID(N'dbo.CK_OrderStatusHistory_From'),OBJECT_ID(N'dbo.CK_OrderStatusHistory_To'),OBJECT_ID(N'dbo.CK_Orders_DeliveryAttempts'),OBJECT_ID(N'dbo.CK_Orders_DeliveryFailureCode')) AND (is_disabled=1 OR is_not_trusted=1)) THROW 51503, 'Delivery failure recovery constraint disabled or untrusted.', 1;
    DECLARE @requiredStatuses TABLE(status varchar(30) PRIMARY KEY);
    INSERT @requiredStatuses(status) VALUES ('PENDING'),('CONFIRMED'),('PREPARING'),('READY'),('ASSIGNED'),('PICKED_UP'),('DELIVERY_FAILED'),('RETURNED_TO_STORE'),('DELIVERED'),('CANCELLED');
    DECLARE @orderStatusDefinition nvarchar(max)=(SELECT UPPER(REPLACE(REPLACE(REPLACE(REPLACE(definition,N'[',N''),N']',N''),N' ',N''),NCHAR(9),N'')) FROM sys.check_constraints WHERE object_id=OBJECT_ID(N'dbo.CK_Orders_Status'));
    DECLARE @historyFromDefinition nvarchar(max)=(SELECT UPPER(REPLACE(REPLACE(REPLACE(REPLACE(definition,N'[',N''),N']',N''),N' ',N''),NCHAR(9),N'')) FROM sys.check_constraints WHERE object_id=OBJECT_ID(N'dbo.CK_OrderStatusHistory_From'));
    DECLARE @historyToDefinition nvarchar(max)=(SELECT UPPER(REPLACE(REPLACE(REPLACE(REPLACE(definition,N'[',N''),N']',N''),N' ',N''),NCHAR(9),N'')) FROM sys.check_constraints WHERE object_id=OBJECT_ID(N'dbo.CK_OrderStatusHistory_To'));
    IF EXISTS (SELECT 1 FROM @requiredStatuses WHERE CHARINDEX(N''''+status+N'''',@orderStatusDefinition)=0 OR CHARINDEX(N''''+status+N'''',@historyFromDefinition)=0 OR CHARINDEX(N''''+status+N'''',@historyToDefinition)=0) OR CHARINDEX(N'FROM_STATUSISNULL',@historyFromDefinition)=0 THROW 51510, 'Order status constraint definition invalid.', 1;
    DECLARE @attemptDefinition nvarchar(max)=(SELECT UPPER(REPLACE(REPLACE(REPLACE(REPLACE(definition,N'[',N''),N']',N''),N' ',N''),NCHAR(9),N'')) FROM sys.check_constraints WHERE object_id=OBJECT_ID(N'dbo.CK_Orders_DeliveryAttempts'));
    IF @attemptDefinition NOT LIKE N'%DELIVERY_ATTEMPT_COUNT>=(0)%' OR @attemptDefinition NOT LIKE N'%DELIVERY_ATTEMPT_LIMIT>(0)%' OR @attemptDefinition NOT LIKE N'%DELIVERY_ATTEMPT_COUNT<=DELIVERY_ATTEMPT_LIMIT%' THROW 51504, 'Delivery attempt constraint definition invalid.', 1;
    DECLARE @reasonDefinition nvarchar(max)=(SELECT UPPER(REPLACE(REPLACE(REPLACE(REPLACE(definition,N'[',N''),N']',N''),N' ',N''),NCHAR(9),N'')) FROM sys.check_constraints WHERE object_id=OBJECT_ID(N'dbo.CK_Orders_DeliveryFailureCode'));
    DECLARE @requiredFailureCodes TABLE(code varchar(30) PRIMARY KEY);
    INSERT @requiredFailureCodes(code) VALUES ('CUSTOMER_UNREACHABLE'),('INVALID_ADDRESS'),('CUSTOMER_RESCHEDULED'),('CUSTOMER_REJECTED'),('SHIPPER_INCIDENT'),('PRODUCT_INCIDENT');
    IF @reasonDefinition NOT LIKE N'%DELIVERY_FAILURE_CODEISNULL%' OR EXISTS (SELECT 1 FROM @requiredFailureCodes WHERE CHARINDEX(N''''+code+N'''',@reasonDefinition)=0) THROW 51505, 'Delivery failure code constraint definition invalid.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.default_constraints dc JOIN sys.columns c ON c.object_id=dc.parent_object_id AND c.column_id=dc.parent_column_id WHERE dc.parent_object_id=OBJECT_ID(N'dbo.Orders') AND c.name=N'delivery_attempt_count' AND TRY_CONVERT(int,REPLACE(REPLACE(dc.definition,N'(',N''),N')',N''))=0) OR NOT EXISTS (SELECT 1 FROM sys.default_constraints dc JOIN sys.columns c ON c.object_id=dc.parent_object_id AND c.column_id=dc.parent_column_id WHERE dc.parent_object_id=OBJECT_ID(N'dbo.Orders') AND c.name=N'delivery_attempt_limit' AND TRY_CONVERT(int,REPLACE(REPLACE(dc.definition,N'(',N''),N')',N''))=2) THROW 51506, 'Delivery attempt defaults must be 0 and 2.', 1;
    IF EXISTS (SELECT 1 FROM dbo.Orders WHERE delivery_attempt_count < 0 OR delivery_attempt_limit <= 0 OR delivery_attempt_count > delivery_attempt_limit) THROW 51507, 'Invalid delivery attempt values.', 1;
    IF EXISTS (SELECT 1 FROM dbo.Orders WHERE delivery_failure_code IS NOT NULL AND delivery_failure_code NOT IN ('CUSTOMER_UNREACHABLE','INVALID_ADDRESS','CUSTOMER_RESCHEDULED','CUSTOMER_REJECTED','SHIPPER_INCIDENT','PRODUCT_INCIDENT')) THROW 51508, 'Invalid delivery failure code.', 1;
    IF EXISTS (SELECT 1 FROM dbo.Orders o CROSS APPLY (SELECT TOP (1) h.to_status FROM dbo.OrderStatusHistory h WHERE h.order_id=o.order_id ORDER BY h.created_at DESC,h.history_id DESC) latest WHERE latest.to_status<>o.order_status) THROW 51509, 'Latest order history status differs from Orders.order_status.', 1;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
PRINT '041 delivery failure recovery validation passed.';
