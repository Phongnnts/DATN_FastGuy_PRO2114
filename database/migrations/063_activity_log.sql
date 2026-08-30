SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO
IF DB_NAME() NOT IN (N'FastGuyDB',N'FastGuyDB_ActivityLog063_Test',N'FastGuyDB_ActivityLog063_RestoreTest') THROW 51000, '063 migration target database is not approved', 1;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory',N'U') IS NULL OR OBJECT_ID(N'dbo.Users',N'U') IS NULL THROW 51000, '063 prerequisites missing', 1;
IF NOT EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='062_staff_pay_rate_snapshot') THROW 51000, 'Run 062_staff_pay_rate_snapshot.sql first', 1;
IF EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='063_activity_log')
BEGIN
 IF OBJECT_ID(N'dbo.ActivityLog',N'U') IS NULL THROW 51000, '063 history exists but schema incomplete', 1;
 PRINT '063_activity_log already applied';
END
ELSE
BEGIN
 BEGIN TRY
  BEGIN TRANSACTION;
  DECLARE @lock_result int;
  EXEC @lock_result=sys.sp_getapplock @Resource=N'FastGuyDB:063_activity_log',@LockMode=N'Exclusive',@LockOwner=N'Transaction',@LockTimeout=15000;
  IF @lock_result<0 THROW 51000, '063 migration lock failed', 1;
  IF OBJECT_ID(N'dbo.ActivityLog',N'U') IS NOT NULL THROW 51000, '063 schema partially exists', 1;
  CREATE TABLE dbo.ActivityLog(
   activity_log_id bigint IDENTITY(1,1) NOT NULL CONSTRAINT PK_ActivityLog PRIMARY KEY,
   actor_user_id int NOT NULL,
   action_type varchar(100) NOT NULL,
   target_type varchar(100) NOT NULL,
   target_id nvarchar(255) NULL,
   summary nvarchar(500) NOT NULL,
   metadata_json nvarchar(max) NULL,
   created_at datetime2(0) NOT NULL CONSTRAINT DF_ActivityLog_CreatedAt DEFAULT SYSUTCDATETIME(),
   CONSTRAINT FK_ActivityLog_ActorUser FOREIGN KEY(actor_user_id) REFERENCES dbo.Users(user_id),
   CONSTRAINT CK_ActivityLog_ActionType CHECK(LEN(LTRIM(RTRIM(action_type))) BETWEEN 1 AND 100),
   CONSTRAINT CK_ActivityLog_TargetType CHECK(LEN(LTRIM(RTRIM(target_type))) BETWEEN 1 AND 100),
   CONSTRAINT CK_ActivityLog_TargetId CHECK(target_id IS NULL OR LEN(LTRIM(RTRIM(target_id))) BETWEEN 1 AND 255),
   CONSTRAINT CK_ActivityLog_Summary CHECK(LEN(LTRIM(RTRIM(summary))) BETWEEN 1 AND 500),
   CONSTRAINT CK_ActivityLog_MetadataJson CHECK(metadata_json IS NULL OR ISJSON(metadata_json)=1)
  );
  CREATE INDEX IX_ActivityLog_CreatedAt ON dbo.ActivityLog(created_at DESC,activity_log_id DESC);
  CREATE INDEX IX_ActivityLog_ActionType_CreatedAt ON dbo.ActivityLog(action_type,created_at DESC,activity_log_id DESC);
  CREATE INDEX IX_ActivityLog_ActorUser_CreatedAt ON dbo.ActivityLog(actor_user_id,created_at DESC,activity_log_id DESC);
  INSERT dbo.SchemaMigrationHistory(migration_id,details) VALUES('063_activity_log',N'Append-only activity log schema');
  COMMIT;
 END TRY BEGIN CATCH IF XACT_STATE()<>0 ROLLBACK; THROW; END CATCH;
END;
