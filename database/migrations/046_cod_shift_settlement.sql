USE FastGuyDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51460, 'Run 000_preflight_history.sql first.', 1;
IF EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '046_cod_shift_settlement')
    PRINT '046_cod_shift_settlement already applied.';
ELSE
BEGIN
    BEGIN TRY
        BEGIN TRANSACTION;
        CREATE TABLE dbo.CodSettlement (
            settlement_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_CodSettlement PRIMARY KEY,
            shipper_id int NOT NULL,
            shift_id int NOT NULL,
            received_by int NULL,
            status varchar(20) NOT NULL CONSTRAINT DF_CodSettlement_Status DEFAULT 'SUBMITTED',
            expected_amount decimal(18,2) NOT NULL,
            submitted_amount decimal(18,2) NOT NULL,
            verified_amount decimal(18,2) NULL,
            reason nvarchar(500) NULL,
            submitted_at datetime2(0) NOT NULL CONSTRAINT DF_CodSettlement_SubmittedAt DEFAULT SYSUTCDATETIME(),
            verified_at datetime2(0) NULL,
            created_at datetime2(0) NOT NULL CONSTRAINT DF_CodSettlement_CreatedAt DEFAULT SYSUTCDATETIME(),
            updated_at datetime2(0) NOT NULL CONSTRAINT DF_CodSettlement_UpdatedAt DEFAULT SYSUTCDATETIME(),
            CONSTRAINT FK_CodSettlement_Shipper FOREIGN KEY (shipper_id) REFERENCES dbo.Users(user_id),
            CONSTRAINT FK_CodSettlement_Shift FOREIGN KEY (shift_id) REFERENCES dbo.WorkShift(shift_id),
            CONSTRAINT FK_CodSettlement_ReceivedBy FOREIGN KEY (received_by) REFERENCES dbo.Users(user_id),
            CONSTRAINT UQ_CodSettlement_ShipperShift UNIQUE (shipper_id, shift_id),
            CONSTRAINT CK_CodSettlement_Status CHECK (status IN ('SUBMITTED','SETTLED','SHORT','OVER')),
            CONSTRAINT CK_CodSettlement_Amounts CHECK (expected_amount >= 0 AND submitted_amount >= 0 AND (verified_amount IS NULL OR verified_amount >= 0)),
            CONSTRAINT CK_CodSettlement_Verification CHECK (
                (status = 'SUBMITTED' AND received_by IS NULL AND verified_amount IS NULL AND verified_at IS NULL)
                OR (status = 'SETTLED' AND received_by IS NOT NULL AND verified_amount = submitted_amount AND verified_at IS NOT NULL)
                OR (status = 'SHORT' AND received_by IS NOT NULL AND verified_amount < submitted_amount AND NULLIF(LTRIM(RTRIM(reason)), N'') IS NOT NULL AND verified_at IS NOT NULL)
                OR (status = 'OVER' AND received_by IS NOT NULL AND verified_amount > submitted_amount AND NULLIF(LTRIM(RTRIM(reason)), N'') IS NOT NULL AND verified_at IS NOT NULL)
            )
        );
        CREATE INDEX IX_CodSettlement_StatusSubmittedAt ON dbo.CodSettlement(status, submitted_at DESC);
        CREATE INDEX IX_CodSettlement_ShipperSubmittedAt ON dbo.CodSettlement(shipper_id, submitted_at DESC);
        INSERT dbo.SchemaMigrationHistory(migration_id, details)
        VALUES ('046_cod_shift_settlement', N'Added shift-scoped COD settlement with immutable submission and Admin verification constraints');
        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
