USE FastGuyDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
BEGIN TRY
    BEGIN TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '046_cod_shift_settlement') THROW 51461, '046 migration history missing.', 1;
    IF OBJECT_ID(N'dbo.CodSettlement', N'U') IS NULL THROW 51462, 'CodSettlement table missing.', 1;
    IF OBJECT_ID(N'dbo.UQ_CodSettlement_ShipperShift', N'UQ') IS NULL THROW 51463, 'Shipper/shift unique constraint missing.', 1;
    IF OBJECT_ID(N'dbo.CK_CodSettlement_Status', N'C') IS NULL OR OBJECT_ID(N'dbo.CK_CodSettlement_Amounts', N'C') IS NULL OR OBJECT_ID(N'dbo.CK_CodSettlement_Verification', N'C') IS NULL THROW 51464, 'COD settlement checks missing.', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes i
        JOIN sys.index_columns ic1 ON ic1.object_id = i.object_id AND ic1.index_id = i.index_id AND ic1.key_ordinal = 1
        JOIN sys.columns c1 ON c1.object_id = ic1.object_id AND c1.column_id = ic1.column_id AND c1.name = N'status'
        JOIN sys.index_columns ic2 ON ic2.object_id = i.object_id AND ic2.index_id = i.index_id AND ic2.key_ordinal = 2 AND ic2.is_descending_key = 1
        JOIN sys.columns c2 ON c2.object_id = ic2.object_id AND c2.column_id = ic2.column_id AND c2.name = N'submitted_at'
        WHERE i.object_id = OBJECT_ID(N'dbo.CodSettlement') AND i.name = N'IX_CodSettlement_StatusSubmittedAt' AND i.is_unique = 0 AND i.is_disabled = 0
    ) THROW 51465, 'Pending queue index definition invalid.', 1;
    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes i
        JOIN sys.index_columns ic1 ON ic1.object_id = i.object_id AND ic1.index_id = i.index_id AND ic1.key_ordinal = 1
        JOIN sys.columns c1 ON c1.object_id = ic1.object_id AND c1.column_id = ic1.column_id AND c1.name = N'shipper_id'
        JOIN sys.index_columns ic2 ON ic2.object_id = i.object_id AND ic2.index_id = i.index_id AND ic2.key_ordinal = 2 AND ic2.is_descending_key = 1
        JOIN sys.columns c2 ON c2.object_id = ic2.object_id AND c2.column_id = ic2.column_id AND c2.name = N'submitted_at'
        WHERE i.object_id = OBJECT_ID(N'dbo.CodSettlement') AND i.name = N'IX_CodSettlement_ShipperSubmittedAt' AND i.is_unique = 0 AND i.is_disabled = 0
    ) THROW 51468, 'Shipper history index definition invalid.', 1;
    IF EXISTS (
        SELECT required.name FROM (VALUES
            (N'settlement_id', N'int', 4, 10, 0, 0), (N'shipper_id', N'int', 4, 10, 0, 0), (N'shift_id', N'int', 4, 10, 0, 0),
            (N'received_by', N'int', 4, 10, 0, 1), (N'status', N'varchar', 20, 0, 0, 0),
            (N'expected_amount', N'decimal', 9, 18, 2, 0), (N'submitted_amount', N'decimal', 9, 18, 2, 0), (N'verified_amount', N'decimal', 9, 18, 2, 1),
            (N'reason', N'nvarchar', 1000, 0, 0, 1), (N'submitted_at', N'datetime2', 8, 0, 0, 0), (N'verified_at', N'datetime2', 8, 0, 0, 1),
            (N'created_at', N'datetime2', 8, 0, 0, 0), (N'updated_at', N'datetime2', 8, 0, 0, 0)
        ) required(name, type_name, max_length, precision_value, scale_value, nullable_value)
        LEFT JOIN sys.columns c ON c.object_id = OBJECT_ID(N'dbo.CodSettlement') AND c.name = required.name
        LEFT JOIN sys.types t ON t.user_type_id = c.user_type_id
        WHERE c.column_id IS NULL OR t.name <> required.type_name OR c.max_length <> required.max_length OR c.precision <> required.precision_value OR c.scale <> required.scale_value OR c.is_nullable <> required.nullable_value
    ) THROW 51469, 'COD settlement column definition invalid.', 1;
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CodSettlement') AND name IN (N'submitted_at', N'verified_at', N'created_at', N'updated_at') AND scale <> 0) THROW 51470, 'COD settlement datetime scale invalid.', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.default_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.CodSettlement') AND name = N'DF_CodSettlement_Status' AND definition LIKE N'%SUBMITTED%')
        OR NOT EXISTS (SELECT 1 FROM sys.default_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.CodSettlement') AND name = N'DF_CodSettlement_SubmittedAt' AND definition LIKE N'%SYSUTCDATETIME%')
        OR NOT EXISTS (SELECT 1 FROM sys.default_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.CodSettlement') AND name = N'DF_CodSettlement_CreatedAt' AND definition LIKE N'%SYSUTCDATETIME%')
        OR NOT EXISTS (SELECT 1 FROM sys.default_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.CodSettlement') AND name = N'DF_CodSettlement_UpdatedAt' AND definition LIKE N'%SYSUTCDATETIME%') THROW 51471, 'COD settlement default definition invalid.', 1;
    IF EXISTS (
        SELECT required.name FROM (VALUES
            (N'CK_CodSettlement_Status', N'%SUBMITTED%SETTLED%SHORT%OVER%'),
            (N'CK_CodSettlement_Amounts', N'%expected_amount%submitted_amount%verified_amount%'),
            (N'CK_CodSettlement_Verification', N'%received_by%verified_amount%submitted_amount%reason%verified_at%')
        ) required(name, pattern)
        LEFT JOIN sys.check_constraints cc ON cc.parent_object_id = OBJECT_ID(N'dbo.CodSettlement') AND cc.name = required.name
        WHERE cc.object_id IS NULL OR cc.is_disabled = 1 OR cc.is_not_trusted = 1 OR cc.definition NOT LIKE required.pattern
    ) THROW 51472, 'COD settlement constraint definition invalid.', 1;
    IF EXISTS (SELECT 1 FROM dbo.CodSettlement GROUP BY shipper_id, shift_id HAVING COUNT(*) > 1) THROW 51466, 'Duplicate shipper/shift settlement.', 1;
    IF EXISTS (SELECT 1 FROM dbo.CodSettlement cs JOIN dbo.WorkShift ws ON ws.shift_id = cs.shift_id WHERE ws.user_id <> cs.shipper_id) THROW 51467, 'Settlement shipper differs from shift owner.', 1;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
PRINT '046 COD settlement validation passed.';
