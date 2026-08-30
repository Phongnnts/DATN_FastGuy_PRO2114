SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() NOT IN (N'FastGuyDB',N'FastGuyDB_ActivityLog063_Test',N'FastGuyDB_ActivityLog063_RestoreTest') THROW 51000, '063 validator target database is not approved', 1;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory',N'U') IS NULL OR NOT EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='063_activity_log') THROW 51000, '063 history missing', 1;
IF OBJECT_ID(N'dbo.ActivityLog',N'U') IS NULL THROW 51000, 'ActivityLog missing', 1;
IF (SELECT COUNT(*) FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.ActivityLog') AND name IN(N'activity_log_id',N'actor_user_id',N'action_type',N'target_type',N'target_id',N'summary',N'metadata_json',N'created_at'))<>8 THROW 51000, 'ActivityLog columns missing', 1;
IF EXISTS(SELECT 1 FROM (VALUES
 (N'activity_log_id',N'bigint',0,8,19,0,1),
 (N'actor_user_id',N'int',0,4,10,0,0),
 (N'action_type',N'varchar',0,100,0,0,0),
 (N'target_type',N'varchar',0,100,0,0,0),
 (N'target_id',N'nvarchar',1,510,0,0,0),
 (N'summary',N'nvarchar',0,1000,0,0,0),
 (N'metadata_json',N'nvarchar',1,-1,0,0,0),
 (N'created_at',N'datetime2',0,6,0,0,0)
) required(name,type_name,is_nullable,max_length,precision,scale,is_identity)
LEFT JOIN sys.columns c ON c.object_id=OBJECT_ID(N'dbo.ActivityLog') AND c.name=required.name
LEFT JOIN sys.types t ON t.user_type_id=c.user_type_id
WHERE c.column_id IS NULL OR t.name<>required.type_name OR c.is_nullable<>required.is_nullable OR c.max_length<>required.max_length OR c.is_identity<>required.is_identity) THROW 51000, 'ActivityLog column definition mismatch', 1;
IF NOT EXISTS(SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.ActivityLog') AND name=N'PK_ActivityLog' AND type='PK') THROW 51000, 'ActivityLog primary key missing', 1;
IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys fk JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id=fk.object_id WHERE fk.parent_object_id=OBJECT_ID(N'dbo.ActivityLog') AND fk.name=N'FK_ActivityLog_ActorUser' AND fkc.parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.ActivityLog'),N'actor_user_id','ColumnId') AND fk.referenced_object_id=OBJECT_ID(N'dbo.Users') AND fkc.referenced_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.Users'),N'user_id','ColumnId') AND fk.is_disabled=0 AND fk.is_not_trusted=0) THROW 51000, 'ActivityLog actor FK mismatch', 1;
IF (SELECT COUNT(*) FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.ActivityLog') AND name IN(N'IX_ActivityLog_CreatedAt',N'IX_ActivityLog_ActionType_CreatedAt',N'IX_ActivityLog_ActorUser_CreatedAt') AND is_disabled=0)=3
BEGIN
 IF EXISTS(SELECT 1 FROM (VALUES(N'IX_ActivityLog_CreatedAt',N'created_at,activity_log_id'),(N'IX_ActivityLog_ActionType_CreatedAt',N'action_type,created_at,activity_log_id'),(N'IX_ActivityLog_ActorUser_CreatedAt',N'actor_user_id,created_at,activity_log_id')) required(index_name,column_names) OUTER APPLY(SELECT STRING_AGG(c.name,N',') WITHIN GROUP(ORDER BY ic.key_ordinal) column_names FROM sys.indexes i JOIN sys.index_columns ic ON ic.object_id=i.object_id AND ic.index_id=i.index_id JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE i.object_id=OBJECT_ID(N'dbo.ActivityLog') AND i.name=required.index_name AND ic.is_included_column=0) actual WHERE actual.column_names<>required.column_names OR actual.column_names IS NULL) THROW 51000, 'ActivityLog index definition mismatch', 1;
END
ELSE THROW 51000, 'ActivityLog indexes missing', 1;
IF (SELECT COUNT(*) FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.ActivityLog') AND name IN(N'CK_ActivityLog_ActionType',N'CK_ActivityLog_TargetType',N'CK_ActivityLog_TargetId',N'CK_ActivityLog_Summary',N'CK_ActivityLog_MetadataJson') AND is_disabled=0 AND is_not_trusted=0)<>5 THROW 51000, 'ActivityLog checks missing or untrusted', 1;
IF EXISTS(SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.ActivityLog') AND name=N'CK_ActivityLog_MetadataJson' AND CHARINDEX(N'ISJSON',UPPER(definition))=0) THROW 51000, 'ActivityLog JSON check mismatch', 1;
PRINT '063 validation passed';
