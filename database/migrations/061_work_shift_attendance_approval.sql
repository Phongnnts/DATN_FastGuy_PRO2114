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
IF DB_NAME() NOT IN (N'FastGuyDB',N'FastGuyDB_Attendance061_Test') THROW 51000, '061 migration target database is not approved', 1;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory',N'U') IS NULL THROW 51000, 'Run 000_preflight_history.sql first', 1;
IF OBJECT_ID(N'dbo.WorkShift',N'U') IS NULL OR OBJECT_ID(N'dbo.Users',N'U') IS NULL THROW 51000, '061 requires dbo.WorkShift and dbo.Users', 1;

IF EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='061_work_shift_attendance_approval')
BEGIN
    IF (SELECT COUNT(*) FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.WorkShift') AND name IN(N'attendance_status',N'approved_minutes',N'approved_overtime_minutes',N'attendance_note',N'approved_by',N'approved_at'))<>6 THROW 51000, '061 history exists but columns are incomplete', 1;
    IF (SELECT COUNT(*) FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.WorkShift') AND name IN(N'CK_WorkShift_AttendanceStatus',N'CK_WorkShift_ApprovedMinutes',N'CK_WorkShift_AttendanceApproval') AND is_disabled=0 AND is_not_trusted=0)<>3 THROW 51000, '061 history exists but checks are incomplete', 1;
    IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID(N'dbo.WorkShift') AND name=N'FK_WorkShift_ApprovedBy' AND is_disabled=0 AND is_not_trusted=0) THROW 51000, '061 history exists but FK is incomplete', 1;
    IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.WorkShift') AND name=N'IX_WorkShift_AttendanceReview' AND is_disabled=0) THROW 51000, '061 history exists but index is incomplete', 1;
    PRINT '061_work_shift_attendance_approval already applied';
END
ELSE
BEGIN
    IF EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.WorkShift') AND name IN(N'attendance_status',N'approved_minutes',N'approved_overtime_minutes',N'attendance_note',N'approved_by',N'approved_at')) THROW 51000, '061 schema partially exists', 1;
    BEGIN TRY
        BEGIN TRANSACTION;
        DECLARE @lock_result int;
        EXEC @lock_result=sys.sp_getapplock @Resource=N'FastGuyDB:061_work_shift_attendance_approval',@LockMode=N'Exclusive',@LockOwner=N'Transaction',@LockTimeout=15000;
        IF @lock_result<0 THROW 51000, '061 migration lock failed', 1;
        IF EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='061_work_shift_attendance_approval')
        BEGIN
            IF (SELECT COUNT(*) FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.WorkShift') AND name IN(N'attendance_status',N'approved_minutes',N'approved_overtime_minutes',N'attendance_note',N'approved_by',N'approved_at'))<>6 THROW 51000, '061 history exists but columns are incomplete', 1;
            IF (SELECT COUNT(*) FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.WorkShift') AND name IN(N'CK_WorkShift_AttendanceStatus',N'CK_WorkShift_ApprovedMinutes',N'CK_WorkShift_AttendanceApproval') AND is_disabled=0 AND is_not_trusted=0)<>3 THROW 51000, '061 history exists but checks are incomplete', 1;
            IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID(N'dbo.WorkShift') AND name=N'FK_WorkShift_ApprovedBy' AND is_disabled=0 AND is_not_trusted=0) THROW 51000, '061 history exists but FK is incomplete', 1;
            IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.WorkShift') AND name=N'IX_WorkShift_AttendanceReview' AND is_disabled=0) THROW 51000, '061 history exists but index is incomplete', 1;
            PRINT '061_work_shift_attendance_approval already applied after lock';
        END
        ELSE
        BEGIN
            IF EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.WorkShift') AND name IN(N'attendance_status',N'approved_minutes',N'approved_overtime_minutes',N'attendance_note',N'approved_by',N'approved_at')) THROW 51000, '061 schema partially exists', 1;
            ALTER TABLE dbo.WorkShift ADD attendance_status varchar(20) NULL, approved_minutes int NULL, approved_overtime_minutes int NULL, attendance_note nvarchar(500) NULL, approved_by int NULL, approved_at datetime2(0) NULL;
            EXEC sys.sp_executesql N'ALTER TABLE dbo.WorkShift WITH CHECK ADD CONSTRAINT CK_WorkShift_AttendanceStatus CHECK(attendance_status IS NULL OR attendance_status IN(''PENDING'',''APPROVED'')), CONSTRAINT CK_WorkShift_ApprovedMinutes CHECK((approved_minutes IS NULL OR approved_minutes>=0) AND (approved_overtime_minutes IS NULL OR approved_overtime_minutes>=0)), CONSTRAINT CK_WorkShift_AttendanceApproval CHECK((attendance_status IS NULL AND approved_minutes IS NULL AND approved_overtime_minutes IS NULL AND attendance_note IS NULL AND approved_by IS NULL AND approved_at IS NULL) OR (attendance_status=''PENDING'' AND approved_minutes IS NULL AND approved_overtime_minutes IS NULL AND approved_by IS NULL AND approved_at IS NULL) OR (attendance_status=''APPROVED'' AND approved_minutes IS NOT NULL AND approved_overtime_minutes IS NOT NULL AND approved_by IS NOT NULL AND approved_at IS NOT NULL)), CONSTRAINT FK_WorkShift_ApprovedBy FOREIGN KEY(approved_by) REFERENCES dbo.Users(user_id);';
            EXEC sys.sp_executesql N'CREATE INDEX IX_WorkShift_AttendanceReview ON dbo.WorkShift(attendance_status,shift_date,user_id) INCLUDE(updated_at,approved_minutes,approved_overtime_minutes);';
            INSERT dbo.SchemaMigrationHistory(migration_id) VALUES('061_work_shift_attendance_approval');
        END
        COMMIT;
    END TRY
    BEGIN CATCH
        IF XACT_STATE()<>0 ROLLBACK;
        THROW;
    END CATCH;
END;
