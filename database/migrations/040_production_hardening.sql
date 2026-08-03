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
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51410, 'Run 000_preflight_history.sql first.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '039_inventory_waste_state') THROW 51411, 'Run 039_inventory_waste_state.sql first.', 1;
IF EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '040_production_hardening')
BEGIN
    PRINT '040_production_hardening already applied; skipped.';
    RETURN;
END;
BEGIN TRY
    BEGIN TRANSACTION;

    INSERT dbo.OrderStatusHistory(order_id, actor_user_id, actor_role, from_status, to_status, note, created_at)
    SELECT o.order_id, NULL, 'SYSTEM', NULL, o.order_status, N'SYSTEM snapshot: pre-migration order had no status history.', COALESCE(o.updated_at, o.created_at, GETDATE())
    FROM dbo.Orders o
    WHERE NOT EXISTS (SELECT 1 FROM dbo.OrderStatusHistory h WHERE h.order_id=o.order_id);

    INSERT dbo.OrderStatusHistory(order_id, actor_user_id, actor_role, from_status, to_status, note, created_at)
    SELECT o.order_id, NULL, 'SYSTEM', latest.to_status, o.order_status, N'SYSTEM reconciliation: current order status differed from latest legacy history.',
           CASE WHEN latest.created_at>=GETDATE() THEN DATEADD(second,1,latest.created_at) ELSE GETDATE() END
    FROM dbo.Orders o
    CROSS APPLY (SELECT TOP (1) h.to_status,h.created_at FROM dbo.OrderStatusHistory h WHERE h.order_id=o.order_id ORDER BY h.created_at DESC,h.history_id DESC) latest
    WHERE latest.to_status<>o.order_status;

    UPDATE o
    SET internal_note = CASE WHEN NULLIF(LTRIM(RTRIM(o.internal_note)), N'') IS NULL THEN N'[SYSTEM:ITEMLESS-ORDER] Legacy order has no OrderItem rows.' WHEN o.internal_note NOT LIKE N'%[[]SYSTEM:ITEMLESS-ORDER]%' THEN CONCAT(o.internal_note, NCHAR(13), NCHAR(10), N'[SYSTEM:ITEMLESS-ORDER] Legacy order has no OrderItem rows.') ELSE o.internal_note END,
        updated_at = CASE WHEN o.internal_note NOT LIKE N'%[[]SYSTEM:ITEMLESS-ORDER]%' OR o.internal_note IS NULL THEN GETDATE() ELSE o.updated_at END
    FROM dbo.Orders o
    WHERE NOT EXISTS (SELECT 1 FROM dbo.OrderItem oi WHERE oi.order_id=o.order_id);

    INSERT dbo.OrderStatusHistory(order_id, actor_user_id, actor_role, from_status, to_status, note, created_at)
    SELECT o.order_id, NULL, 'SYSTEM', o.order_status, o.order_status, CONCAT(N'Invalid staff assignment cleared; user ', o.staff_id, N' role was ', COALESCE(u.role_name, N'missing'), N'.'), GETDATE()
    FROM dbo.Orders o LEFT JOIN dbo.Users u ON u.user_id=o.staff_id
    WHERE o.staff_id IS NOT NULL AND (u.user_id IS NULL OR u.role_name NOT IN ('STAFF','ADMIN'));
    UPDATE o SET staff_id=NULL, updated_at=GETDATE()
    FROM dbo.Orders o LEFT JOIN dbo.Users u ON u.user_id=o.staff_id
    WHERE o.staff_id IS NOT NULL AND (u.user_id IS NULL OR u.role_name NOT IN ('STAFF','ADMIN'));

    INSERT dbo.OrderStatusHistory(order_id, actor_user_id, actor_role, from_status, to_status, note, created_at)
    SELECT o.order_id, NULL, 'SYSTEM', o.order_status, o.order_status, CONCAT(N'Invalid shipper assignment cleared; user ', o.shipper_id, N' role was ', COALESCE(u.role_name, N'missing'), N'.'), GETDATE()
    FROM dbo.Orders o LEFT JOIN dbo.Users u ON u.user_id=o.shipper_id
    WHERE o.shipper_id IS NOT NULL AND (u.user_id IS NULL OR u.role_name<>'SHIPPER');
    UPDATE o SET shipper_id=NULL, assigned_at=NULL, updated_at=GETDATE()
    FROM dbo.Orders o LEFT JOIN dbo.Users u ON u.user_id=o.shipper_id
    WHERE o.shipper_id IS NOT NULL AND (u.user_id IS NULL OR u.role_name<>'SHIPPER');

    UPDATE dbo.Orders
    SET assigned_at=COALESCE(picked_up_at,ready_at,confirmed_at,created_at,GETDATE()), updated_at=GETDATE()
    WHERE shipper_id IS NOT NULL AND assigned_at IS NULL;

    IF EXISTS (SELECT 1 FROM dbo.OrderItem WHERE total_price<>unit_price*quantity) THROW 51412, 'OrderItem amount mismatch requires manual correction.', 1;
    IF EXISTS (SELECT 1 FROM dbo.Orders WHERE final_amount<>total_amount+shipping_fee+service_fee-discount_amount) THROW 51419, 'Order amount mismatch requires manual correction.', 1;
    IF OBJECT_ID(N'dbo.CK_OrderItem_Total', N'C') IS NULL ALTER TABLE dbo.OrderItem WITH CHECK ADD CONSTRAINT CK_OrderItem_Total CHECK (total_price=unit_price*quantity);
    IF OBJECT_ID(N'dbo.CK_Orders_FinalAmount', N'C') IS NULL ALTER TABLE dbo.Orders WITH CHECK ADD CONSTRAINT CK_Orders_FinalAmount CHECK (final_amount=total_amount+shipping_fee+service_fee-discount_amount);
    IF OBJECT_ID(N'dbo.CK_Orders_AssignmentTimes', N'C') IS NULL ALTER TABLE dbo.Orders WITH CHECK ADD CONSTRAINT CK_Orders_AssignmentTimes CHECK ((shipper_id IS NULL) OR assigned_at IS NOT NULL);

    IF NOT EXISTS (SELECT 1 FROM sys.indexes i JOIN sys.index_columns ic ON ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE i.object_id=OBJECT_ID(N'dbo.OrderItem') AND c.name=N'order_id') CREATE INDEX IX_OrderItem_Order ON dbo.OrderItem(order_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes i JOIN sys.index_columns ic ON ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE i.object_id=OBJECT_ID(N'dbo.OrderStatusHistory') AND c.name=N'order_id') CREATE INDEX IX_OrderStatusHistory_Order_Created ON dbo.OrderStatusHistory(order_id,created_at,history_id);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes i JOIN sys.index_columns ic ON ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE i.object_id=OBJECT_ID(N'dbo.WorkShift') AND c.name=N'user_id') CREATE INDEX IX_WorkShift_User_Date ON dbo.WorkShift(user_id,shift_date,start_time,end_time);

    EXEC(N'CREATE OR ALTER TRIGGER dbo.TR_Orders_AssignmentRoleGuard ON dbo.Orders AFTER INSERT, UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i LEFT JOIN dbo.Users u ON u.user_id=i.staff_id WHERE i.staff_id IS NOT NULL AND (u.user_id IS NULL OR u.role_name NOT IN (''STAFF'',''ADMIN''))) THROW 51413, ''Orders.staff_id must reference STAFF or ADMIN.'', 1;
    IF EXISTS (SELECT 1 FROM inserted i LEFT JOIN dbo.Users u ON u.user_id=i.shipper_id WHERE i.shipper_id IS NOT NULL AND (u.user_id IS NULL OR u.role_name<>''SHIPPER'')) THROW 51414, ''Orders.shipper_id must reference SHIPPER.'', 1;
END');
    EXEC(N'CREATE OR ALTER TRIGGER dbo.TR_WorkShift_RoleGuard ON dbo.WorkShift AFTER INSERT, UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i LEFT JOIN dbo.Users u ON u.user_id=i.user_id WHERE u.user_id IS NULL OR u.role_name NOT IN (''STAFF'',''SHIPPER'')) THROW 51415, ''WorkShift.user_id must reference STAFF or SHIPPER.'', 1;
END');
    EXEC(N'CREATE OR ALTER TRIGGER dbo.TR_Users_OperationalRoleGuard ON dbo.Users AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF NOT UPDATE(role_name) RETURN;
    IF EXISTS (SELECT 1 FROM inserted i WHERE i.role_name NOT IN (''STAFF'',''ADMIN'') AND EXISTS (SELECT 1 FROM dbo.Orders o WHERE o.staff_id=i.user_id)) THROW 51416, ''Cannot change role while user has staff assignments.'', 1;
    IF EXISTS (SELECT 1 FROM inserted i WHERE i.role_name<>''SHIPPER'' AND EXISTS (SELECT 1 FROM dbo.Orders o WHERE o.shipper_id=i.user_id)) THROW 51417, ''Cannot change role while user has shipper assignments.'', 1;
    IF EXISTS (SELECT 1 FROM inserted i WHERE i.role_name NOT IN (''STAFF'',''SHIPPER'') AND EXISTS (SELECT 1 FROM dbo.WorkShift w WHERE w.user_id=i.user_id)) THROW 51418, ''Cannot change role while user has work shifts.'', 1;
END');

    INSERT dbo.SchemaMigrationHistory(migration_id,details) VALUES ('040_production_hardening',N'Order snapshots and itemless markers applied; invalid assignments cleared with history; role guards, checks and canonical indexes enforced');
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
