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
IF DB_NAME() NOT IN (N'FastGuyDB',N'FastGuyDB_Operations060_Test') THROW 51000, '059 target database is not approved', 1;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory',N'U') IS NULL THROW 51000, 'Run 000_preflight_history.sql first', 1;
IF EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='059_shift_schedule_order_timeout')
BEGIN
    IF NOT EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.WorkShift') AND name=N'shift_code' AND is_nullable=0) THROW 51000, '059 history exists but shift_code is incomplete', 1;
    IF NOT EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.WorkShift') AND name=N'check_in_source' AND is_nullable=1) THROW 51000, '059 history exists but check_in_source is incomplete', 1;
    IF NOT EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.WorkShift') AND name=N'check_out_source' AND is_nullable=1) THROW 51000, '059 history exists but check_out_source is incomplete', 1;
    IF NOT EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.WorkShift') AND name=N'staff_role_snapshot' AND is_nullable=0) THROW 51000, '059 history exists but staff_role_snapshot is incomplete', 1;
    IF NOT EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.Orders') AND name=N'status_entered_at' AND is_nullable=0) THROW 51000, '059 history exists but status_entered_at is incomplete', 1;
    IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.WorkShift') AND name=N'UX_WorkShift_Staff_Date_Code' AND is_unique=1 AND is_disabled=0) THROW 51000, '059 history exists but shift uniqueness is incomplete', 1;
    IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.Orders') AND name=N'IX_Orders_Status_StatusEnteredAt' AND is_disabled=0) THROW 51000, '059 history exists but status timeout index is incomplete', 1;
    IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.Orders') AND name=N'IX_Orders_StaffShift_Status' AND is_disabled=0) THROW 51000, '059 history exists but staff shift index is incomplete', 1;
    IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.Orders') AND name=N'IX_Orders_PaymentStatus_OrderStatus_StatusEnteredAt' AND is_disabled=0) THROW 51000, '059 history exists but payment timeout index is incomplete', 1;
    BEGIN TRY
        BEGIN TRANSACTION;
        IF OBJECT_ID(N'dbo.TR_WorkShift_RoleGuard',N'TR') IS NOT NULL DROP TRIGGER dbo.TR_WorkShift_RoleGuard;
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
    PRINT '059_shift_schedule_order_timeout already applied';
END
ELSE
BEGIN
    IF OBJECT_ID(N'dbo.WorkShift',N'U') IS NULL OR OBJECT_ID(N'dbo.Orders',N'U') IS NULL OR OBJECT_ID(N'dbo.Users',N'U') IS NULL THROW 51000, '059 required table missing', 1;
    IF COL_LENGTH(N'dbo.WorkShift',N'shift_code') IS NOT NULL OR COL_LENGTH(N'dbo.Orders',N'status_entered_at') IS NOT NULL THROW 51000, '059 schema partially exists', 1;
    BEGIN TRY
        BEGIN TRANSACTION;
        DECLARE @app_lock_result int;
        EXEC @app_lock_result=sys.sp_getapplock @Resource=N'FastGuyDB:059_shift_schedule_order_timeout',@LockMode=N'Exclusive',@LockOwner=N'Transaction',@LockTimeout=0;
        IF @app_lock_result<0 THROW 51000,'059 migration lock unavailable',1;
        IF OBJECT_ID(N'dbo.TR_WorkShift_RoleGuard',N'TR') IS NOT NULL DROP TRIGGER dbo.TR_WorkShift_RoleGuard;
        ALTER TABLE dbo.WorkShift ADD shift_code varchar(10) NULL, check_in_source varchar(10) NULL, check_out_source varchar(10) NULL, staff_role_snapshot varchar(12) NOT NULL CONSTRAINT DF_WorkShift_StaffRoleSnapshot DEFAULT 'NON_STAFF';
        EXEC sys.sp_executesql N'UPDATE ws SET shift_code=CASE WHEN ws.start_time<''12:00'' THEN ''MORNING'' WHEN ws.start_time<''16:00'' THEN ''AFTERNOON'' ELSE ''EVENING'' END, staff_role_snapshot=CASE WHEN u.role_name=''STAFF'' AND ws.status=''SCHEDULED'' THEN ''STAFF'' WHEN u.role_name=''STAFF'' THEN ''LEGACY_STAFF'' ELSE ''NON_STAFF'' END FROM dbo.WorkShift ws JOIN dbo.Users u ON u.user_id=ws.user_id;';
        IF OBJECT_ID(N'dbo.CK_WorkShift_Time',N'C') IS NOT NULL ALTER TABLE dbo.WorkShift DROP CONSTRAINT CK_WorkShift_Time;
        EXEC sys.sp_executesql N'UPDATE ws SET start_time=CASE shift_code WHEN ''MORNING'' THEN ''08:00'' WHEN ''AFTERNOON'' THEN ''12:00'' ELSE ''16:00'' END, end_time=CASE shift_code WHEN ''MORNING'' THEN ''12:00'' WHEN ''AFTERNOON'' THEN ''16:00'' ELSE ''21:00'' END FROM dbo.WorkShift ws WHERE staff_role_snapshot=''STAFF'';';
        EXEC sys.sp_executesql N'IF EXISTS(SELECT 1 FROM dbo.WorkShift WHERE shift_code IS NULL) THROW 51000,''059 cannot backfill shift_code'',1;';
        EXEC sys.sp_executesql N'IF EXISTS(SELECT shift_date,shift_code FROM dbo.WorkShift WHERE staff_role_snapshot=''STAFF'' GROUP BY shift_date,shift_code HAVING COUNT_BIG(*)>1) THROW 51000,''059 duplicate STAFF shift date/code'',1;';
        EXEC sys.sp_executesql N'ALTER TABLE dbo.WorkShift ALTER COLUMN shift_code varchar(10) NOT NULL;';
        EXEC sys.sp_executesql N'ALTER TABLE dbo.WorkShift ADD CONSTRAINT CK_WorkShift_Time CHECK (start_time<end_time), CONSTRAINT CK_WorkShift_ShiftCode CHECK (shift_code IN (''MORNING'',''AFTERNOON'',''EVENING'')), CONSTRAINT CK_WorkShift_CheckInSource CHECK (check_in_source IS NULL OR check_in_source IN (''MANUAL'',''AUTO'')), CONSTRAINT CK_WorkShift_CheckOutSource CHECK (check_out_source IS NULL OR check_out_source IN (''MANUAL'',''AUTO'')), CONSTRAINT CK_WorkShift_StaffRoleSnapshot CHECK (staff_role_snapshot IN (''STAFF'',''LEGACY_STAFF'',''NON_STAFF'')), CONSTRAINT CK_WorkShift_StaffFixedTimes CHECK (staff_role_snapshot<>''STAFF'' OR (shift_code=''MORNING'' AND start_time=''08:00'' AND end_time=''12:00'') OR (shift_code=''AFTERNOON'' AND start_time=''12:00'' AND end_time=''16:00'') OR (shift_code=''EVENING'' AND start_time=''16:00'' AND end_time=''21:00''));';
        EXEC sys.sp_executesql N'CREATE UNIQUE INDEX UX_WorkShift_Staff_Date_Code ON dbo.WorkShift(shift_date,shift_code) WHERE staff_role_snapshot=''STAFF'';';
        ALTER TABLE dbo.Orders ADD status_entered_at datetime2(0) NULL;
        EXEC sys.sp_executesql N'UPDATE dbo.Orders SET status_entered_at=COALESCE(updated_at,created_at,SYSDATETIME()) WHERE status_entered_at IS NULL;';
        EXEC sys.sp_executesql N'ALTER TABLE dbo.Orders ALTER COLUMN status_entered_at datetime2(0) NOT NULL;';
        EXEC sys.sp_executesql N'ALTER TABLE dbo.Orders ADD CONSTRAINT DF_Orders_StatusEnteredAt DEFAULT SYSDATETIME() FOR status_entered_at;';
        EXEC sys.sp_executesql N'CREATE INDEX IX_Orders_Status_StatusEnteredAt ON dbo.Orders(order_status,status_entered_at);';
        IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.Orders') AND name=N'IX_Orders_StaffShift_Status') EXEC sys.sp_executesql N'CREATE INDEX IX_Orders_StaffShift_Status ON dbo.Orders(staff_shift_id,order_status);';
        EXEC sys.sp_executesql N'CREATE INDEX IX_Orders_PaymentStatus_OrderStatus_StatusEnteredAt ON dbo.Orders(payment_status,order_status,status_entered_at);';
        INSERT dbo.SchemaMigrationHistory(migration_id,details) VALUES('059_shift_schedule_order_timeout',N'Fixed STAFF shifts and order status timeout timestamp');
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
