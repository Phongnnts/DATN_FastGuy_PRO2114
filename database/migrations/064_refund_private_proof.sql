SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF DB_NAME() <> N'FastGuyDB' THROW 51000, '064 target must be FastGuyDB', 1;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory',N'U') IS NULL OR OBJECT_ID(N'dbo.Orders',N'U') IS NULL THROW 51000, '064 prerequisites missing', 1;
IF EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='064_refund_private_proof')
BEGIN
 IF COL_LENGTH(N'dbo.Orders',N'refund_proof_public_id') IS NULL OR COL_LENGTH(N'dbo.Orders',N'refund_proof_content_type') IS NULL OR COL_LENGTH(N'dbo.Orders',N'refund_proof_uploaded_at') IS NULL THROW 51000, '064 history exists but columns are missing', 1;
 PRINT '064_refund_private_proof already applied';
END
ELSE
BEGIN
 BEGIN TRY
  BEGIN TRANSACTION;
  IF COL_LENGTH(N'dbo.Orders',N'refund_proof_public_id') IS NOT NULL OR COL_LENGTH(N'dbo.Orders',N'refund_proof_content_type') IS NOT NULL OR COL_LENGTH(N'dbo.Orders',N'refund_proof_uploaded_at') IS NOT NULL THROW 51000, '064 schema partially exists', 1;
  ALTER TABLE dbo.Orders ADD
   refund_proof_public_id nvarchar(255) NULL,
   refund_proof_content_type varchar(50) NULL,
   refund_proof_uploaded_at datetime2(0) NULL;
  INSERT dbo.SchemaMigrationHistory(migration_id,details) VALUES('064_refund_private_proof',N'Private external refund proof metadata');
  COMMIT;
 END TRY BEGIN CATCH IF XACT_STATE()<>0 ROLLBACK; THROW; END CATCH;
END;
GO
