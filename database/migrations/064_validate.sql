SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() <> N'FastGuyDB' THROW 51000, '064 validator target must be FastGuyDB', 1;
IF NOT EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='064_refund_private_proof') THROW 51000, '064 history missing', 1;
IF EXISTS(SELECT 1 FROM (VALUES
 (N'refund_proof_public_id',N'nvarchar',1,510,0),
 (N'refund_proof_content_type',N'varchar',1,50,0),
 (N'refund_proof_uploaded_at',N'datetime2',1,6,0)
) required(name,type_name,is_nullable,max_length,scale)
LEFT JOIN sys.columns c ON c.object_id=OBJECT_ID(N'dbo.Orders') AND c.name=required.name
LEFT JOIN sys.types t ON t.user_type_id=c.user_type_id
WHERE c.column_id IS NULL OR t.name<>required.type_name OR c.is_nullable<>required.is_nullable OR c.max_length<>required.max_length OR c.scale<>required.scale) THROW 51000, '064 column definition mismatch', 1;
PRINT '064 validation passed';
