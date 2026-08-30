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
IF DB_NAME() NOT IN (N'FastGuyDB',N'FastGuyDB_PayRate062_Test') THROW 51000, '062 migration target database is not approved', 1;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory',N'U') IS NULL OR OBJECT_ID(N'dbo.WorkShift',N'U') IS NULL OR OBJECT_ID(N'dbo.Users',N'U') IS NULL THROW 51000, '062 prerequisites missing', 1;
IF EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='062_staff_pay_rate_snapshot')
BEGIN
 IF OBJECT_ID(N'dbo.StaffPayRate',N'U') IS NULL OR COL_LENGTH(N'dbo.WorkShift',N'total_pay_amount') IS NULL THROW 51000, '062 history exists but schema incomplete', 1;
 PRINT '062_staff_pay_rate_snapshot already applied';
END
ELSE
BEGIN
 BEGIN TRY
  BEGIN TRANSACTION;
  DECLARE @lock_result int;
  EXEC @lock_result=sys.sp_getapplock @Resource=N'FastGuyDB:062_staff_pay_rate_snapshot',@LockMode=N'Exclusive',@LockOwner=N'Transaction',@LockTimeout=15000;
  IF @lock_result<0 THROW 51000, '062 migration lock failed', 1;
  IF OBJECT_ID(N'dbo.StaffPayRate',N'U') IS NOT NULL OR COL_LENGTH(N'dbo.WorkShift',N'pay_snapshot_status') IS NOT NULL THROW 51000, '062 schema partially exists', 1;
  CREATE TABLE dbo.StaffPayRate(
   pay_rate_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_StaffPayRate PRIMARY KEY,
   user_id int NOT NULL,
   effective_from date NOT NULL,
   regular_hourly_rate decimal(18,2) NOT NULL,
   overtime_hourly_rate decimal(18,2) NOT NULL,
   created_by int NOT NULL,
   created_at datetime2(0) NOT NULL CONSTRAINT DF_StaffPayRate_CreatedAt DEFAULT SYSDATETIME(),
   CONSTRAINT FK_StaffPayRate_User FOREIGN KEY(user_id) REFERENCES dbo.Users(user_id),
   CONSTRAINT FK_StaffPayRate_CreatedBy FOREIGN KEY(created_by) REFERENCES dbo.Users(user_id),
   CONSTRAINT UQ_StaffPayRate_User_EffectiveFrom UNIQUE(user_id,effective_from),
   CONSTRAINT CK_StaffPayRate_Positive CHECK(regular_hourly_rate>0 AND overtime_hourly_rate>0)
  );
  CREATE INDEX IX_StaffPayRate_User_EffectiveFrom ON dbo.StaffPayRate(user_id,effective_from DESC) INCLUDE(regular_hourly_rate,overtime_hourly_rate);
  ALTER TABLE dbo.WorkShift ADD pay_snapshot_status varchar(30) NULL, regular_hourly_rate_snapshot decimal(18,2) NULL, overtime_hourly_rate_snapshot decimal(18,2) NULL, regular_pay_amount decimal(18,2) NULL, overtime_pay_amount decimal(18,2) NULL, total_pay_amount decimal(18,2) NULL;
  EXEC sys.sp_executesql N'UPDATE dbo.WorkShift SET pay_snapshot_status=''LEGACY_UNAVAILABLE'' WHERE attendance_status=''APPROVED'';';
  EXEC sys.sp_executesql N'ALTER TABLE dbo.WorkShift WITH CHECK ADD CONSTRAINT CK_WorkShift_PaySnapshot CHECK((pay_snapshot_status IS NULL AND regular_hourly_rate_snapshot IS NULL AND overtime_hourly_rate_snapshot IS NULL AND regular_pay_amount IS NULL AND overtime_pay_amount IS NULL AND total_pay_amount IS NULL) OR (pay_snapshot_status=''LEGACY_UNAVAILABLE'' AND regular_hourly_rate_snapshot IS NULL AND overtime_hourly_rate_snapshot IS NULL AND regular_pay_amount IS NULL AND overtime_pay_amount IS NULL AND total_pay_amount IS NULL) OR (pay_snapshot_status=''CALCULATED'' AND regular_hourly_rate_snapshot>0 AND overtime_hourly_rate_snapshot>0 AND regular_pay_amount>=0 AND overtime_pay_amount>=0 AND total_pay_amount=regular_pay_amount+overtime_pay_amount));';
  INSERT dbo.SchemaMigrationHistory(migration_id) VALUES('062_staff_pay_rate_snapshot');
  COMMIT;
 END TRY BEGIN CATCH IF XACT_STATE()<>0 ROLLBACK; THROW; END CATCH;
END;
