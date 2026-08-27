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
IF DB_NAME() NOT IN (N'FastGuyDB',N'FastGuyDB_Inventory054_Test',N'FastGuyDB_Ownership058_Test') THROW 51000, '058 target database is not approved', 1;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory',N'U') IS NULL THROW 51000, 'Run 000_preflight_history.sql first', 1;
IF EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='058_staff_shift_ownership')
BEGIN
    IF NOT EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id WHERE c.object_id=OBJECT_ID(N'dbo.Orders') AND c.name=N'staff_shift_id' AND t.name=N'int' AND c.max_length=4 AND c.precision=10 AND c.scale=0 AND c.is_nullable=1) THROW 51000, '058 history exists but staff_shift_id is incomplete', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID(N'dbo.Orders') AND referenced_object_id=OBJECT_ID(N'dbo.WorkShift') AND name=N'FK_Orders_StaffShift' AND is_disabled=0 AND is_not_trusted=0) THROW 51000, '058 history exists but FK is incomplete', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_key_columns fkc JOIN sys.columns pc ON pc.object_id=fkc.parent_object_id AND pc.column_id=fkc.parent_column_id JOIN sys.columns rc ON rc.object_id=fkc.referenced_object_id AND rc.column_id=fkc.referenced_column_id WHERE fkc.constraint_object_id=OBJECT_ID(N'dbo.FK_Orders_StaffShift') AND pc.name=N'staff_shift_id' AND rc.name=N'shift_id') THROW 51000, '058 history exists but FK columns are incorrect', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.Orders') AND i.name=N'IX_Orders_StaffShift_Status' AND i.is_disabled=0) THROW 51000, '058 history exists but index is incomplete', 1;
    IF (SELECT COUNT(*) FROM sys.index_columns ic JOIN sys.indexes i ON i.object_id=ic.object_id AND i.index_id=ic.index_id JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE i.object_id=OBJECT_ID(N'dbo.Orders') AND i.name=N'IX_Orders_StaffShift_Status' AND ((ic.key_ordinal=1 AND c.name=N'staff_shift_id') OR (ic.key_ordinal=2 AND c.name=N'order_status')))<>2 THROW 51000, '058 history exists but index keys are incorrect', 1;
    IF EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.indexes i ON i.object_id=ic.object_id AND i.index_id=ic.index_id WHERE i.object_id=OBJECT_ID(N'dbo.Orders') AND i.name=N'IX_Orders_StaffShift_Status' AND ic.key_ordinal>2) THROW 51000, '058 history exists but index has extra keys', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.triggers WHERE parent_id=OBJECT_ID(N'dbo.Orders') AND name=N'TR_Orders_AssignmentRoleGuard' AND is_disabled=0) THROW 51000, '058 history exists but trigger is incomplete', 1;
    IF CHARINDEX(N'Orders.staff_shift_id must reference a STAFF shift.',OBJECT_DEFINITION(OBJECT_ID(N'dbo.TR_Orders_AssignmentRoleGuard'))) = 0 THROW 51000, '058 history exists but trigger ownership clause is incomplete', 1;
    PRINT '058_staff_shift_ownership already applied';
END
ELSE
BEGIN
    IF OBJECT_ID(N'dbo.Orders',N'U') IS NULL OR OBJECT_ID(N'dbo.WorkShift',N'U') IS NULL THROW 51000, 'Orders or WorkShift table missing', 1;
    IF COL_LENGTH(N'dbo.Orders',N'staff_shift_id') IS NOT NULL THROW 51000, '058 schema partially exists', 1;
    BEGIN TRY
        BEGIN TRANSACTION;
        DECLARE @app_lock_result int;
        EXEC @app_lock_result=sys.sp_getapplock @Resource=N'FastGuyDB:058_staff_shift_ownership',@LockMode=N'Exclusive',@LockOwner=N'Transaction',@LockTimeout=0;
        IF @app_lock_result<0 THROW 51000,'058 migration lock unavailable',1;
        DECLARE @order_count_before bigint=(SELECT COUNT_BIG(*) FROM dbo.Orders);
        ALTER TABLE dbo.Orders ADD staff_shift_id int NULL;
        ALTER TABLE dbo.Orders WITH CHECK ADD CONSTRAINT FK_Orders_StaffShift FOREIGN KEY(staff_shift_id) REFERENCES dbo.WorkShift(shift_id);
        CREATE INDEX IX_Orders_StaffShift_Status ON dbo.Orders(staff_shift_id,order_status);
        EXEC(N'CREATE OR ALTER TRIGGER dbo.TR_Orders_AssignmentRoleGuard ON dbo.Orders AFTER INSERT, UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted i LEFT JOIN dbo.Users u ON u.user_id=i.staff_id WHERE i.staff_id IS NOT NULL AND (u.user_id IS NULL OR u.role_name NOT IN (''STAFF'',''ADMIN''))) THROW 51413, ''Orders.staff_id must reference STAFF or ADMIN.'', 1;
    IF EXISTS (SELECT 1 FROM inserted i LEFT JOIN dbo.Users u ON u.user_id=i.shipper_id WHERE i.shipper_id IS NOT NULL AND (u.user_id IS NULL OR u.role_name<>''SHIPPER'')) THROW 51414, ''Orders.shipper_id must reference SHIPPER.'', 1;
    IF EXISTS (SELECT 1 FROM inserted i LEFT JOIN dbo.WorkShift ws ON ws.shift_id=i.staff_shift_id LEFT JOIN dbo.Users u ON u.user_id=ws.user_id WHERE i.staff_shift_id IS NOT NULL AND (ws.shift_id IS NULL OR u.user_id IS NULL OR u.role_name<>''STAFF'')) THROW 51420, ''Orders.staff_shift_id must reference a STAFF shift.'', 1;
END');
        IF (SELECT COUNT_BIG(*) FROM dbo.Orders)<>@order_count_before THROW 51000,'058 changed Orders row count',1;
        EXEC sys.sp_executesql N'IF EXISTS(SELECT 1 FROM dbo.Orders WHERE staff_shift_id IS NOT NULL) THROW 51000,''058 must not backfill ownership'',1;';
        INSERT dbo.SchemaMigrationHistory(migration_id,details) VALUES('058_staff_shift_ownership',N'Nullable current Staff shift ownership for explicit order handover');
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
