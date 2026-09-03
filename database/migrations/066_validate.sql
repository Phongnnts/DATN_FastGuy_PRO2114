SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() NOT IN (N'FastGuyDB',N'DemoDatabase') AND DB_NAME() NOT LIKE N'FastGuyDB[_]%[_]Test' THROW 51000, '066 validator target is not approved', 1;
IF NOT EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='066_unified_staff_shipper_schedule') THROW 51000, '066 history missing', 1;
IF EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.WorkShift') AND name=N'UX_WorkShift_Staff_Date_Code') THROW 51000, '066 legacy shift index remains', 1;
IF NOT EXISTS(SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.WorkShift') AND i.name=N'UX_WorkShift_User_Date_Code' AND i.is_unique=1 AND i.has_filter=0 AND i.is_disabled=0) THROW 51000, '066 user shift uniqueness missing', 1;
IF EXISTS(
 SELECT expected.key_ordinal,expected.column_name
 FROM (VALUES(1,N'user_id'),(2,N'shift_date'),(3,N'shift_code'))expected(key_ordinal,column_name)
 LEFT JOIN sys.indexes i ON i.object_id=OBJECT_ID(N'dbo.WorkShift') AND i.name=N'UX_WorkShift_User_Date_Code'
 LEFT JOIN sys.index_columns ic ON ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=expected.key_ordinal
 LEFT JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id AND c.name=expected.column_name
 WHERE c.column_id IS NULL
) THROW 51000, '066 user shift key columns mismatch', 1;
IF (SELECT COUNT(*) FROM sys.indexes i JOIN sys.index_columns ic ON ic.object_id=i.object_id AND ic.index_id=i.index_id WHERE i.object_id=OBJECT_ID(N'dbo.WorkShift') AND i.name=N'UX_WorkShift_User_Date_Code' AND ic.key_ordinal>0)<>3 THROW 51000, '066 user shift key arity mismatch', 1;
PRINT '066 validation passed';
