SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF DB_NAME() NOT IN (N'FastGuyDB',N'DemoDatabase') AND DB_NAME() NOT LIKE N'FastGuyDB[_]%[_]Test' THROW 51000, '066 target is not approved', 1;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory',N'U') IS NULL OR OBJECT_ID(N'dbo.WorkShift',N'U') IS NULL THROW 51000, '066 prerequisites missing', 1;
IF EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='066_unified_staff_shipper_schedule')
BEGIN
 IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.WorkShift') AND name=N'UX_WorkShift_User_Date_Code' AND is_unique=1 AND is_disabled=0) THROW 51000, '066 history exists but schema is incomplete', 1;
 IF EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.WorkShift') AND name=N'UX_WorkShift_Staff_Date_Code') THROW 51000, '066 history exists but legacy index remains', 1;
 PRINT '066_unified_staff_shipper_schedule already applied';
END
ELSE
BEGIN
 BEGIN TRY
  BEGIN TRANSACTION;
  IF EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.WorkShift') AND name=N'UX_WorkShift_User_Date_Code') THROW 51000, '066 schema partially exists', 1;
  IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.WorkShift') AND name=N'UX_WorkShift_Staff_Date_Code' AND is_unique=1) THROW 51000, '066 legacy shift index missing', 1;
  DROP INDEX UX_WorkShift_Staff_Date_Code ON dbo.WorkShift;
  CREATE UNIQUE INDEX UX_WorkShift_User_Date_Code ON dbo.WorkShift(user_id,shift_date,shift_code);
  INSERT dbo.SchemaMigrationHistory(migration_id,details) VALUES('066_unified_staff_shipper_schedule',N'Allow multiple STAFF and SHIPPER users per weekly shift slot');
  COMMIT;
 END TRY BEGIN CATCH IF XACT_STATE()<>0 ROLLBACK; THROW; END CATCH;
END;
GO
