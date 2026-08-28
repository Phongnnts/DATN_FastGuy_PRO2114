SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() NOT IN (N'FastGuyDB',N'FastGuyDB_Operations060_Test') THROW 51000, '060 migration target database is not approved', 1;
IF OBJECT_ID(N'dbo.SchemaMigrationHistory',N'U') IS NULL THROW 51000, 'Run 000_preflight_history.sql first', 1;

IF EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id='060_operating_finance')
BEGIN
    IF OBJECT_ID(N'dbo.OperatingExpense',N'U') IS NULL THROW 51000, '060 history exists but OperatingExpense is missing', 1;
    IF OBJECT_ID(N'dbo.FixedAsset',N'U') IS NULL THROW 51000, '060 history exists but FixedAsset is missing', 1;
    IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID(N'dbo.OperatingExpense') AND name=N'FK_OperatingExpense_CreatedBy' AND is_disabled=0 AND is_not_trusted=0) THROW 51000, '060 history exists but OperatingExpense FK is incomplete', 1;
    IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID(N'dbo.FixedAsset') AND name=N'FK_FixedAsset_CreatedBy' AND is_disabled=0 AND is_not_trusted=0) THROW 51000, '060 history exists but FixedAsset FK is incomplete', 1;
    IF (SELECT COUNT(*) FROM sys.check_constraints WHERE parent_object_id IN(OBJECT_ID(N'dbo.OperatingExpense'),OBJECT_ID(N'dbo.FixedAsset')) AND name IN(N'CK_OperatingExpense_Category',N'CK_OperatingExpense_Amount',N'CK_FixedAsset_Value',N'CK_FixedAsset_UsefulLife',N'CK_FixedAsset_Status',N'CK_FixedAsset_Retirement') AND is_disabled=0 AND is_not_trusted=0)<>6 THROW 51000, '060 history exists but checks are incomplete', 1;
    IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.OperatingExpense') AND name=N'IX_OperatingExpense_ExpenseDate' AND is_disabled=0) THROW 51000, '060 history exists but expense index is incomplete', 1;
    IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.FixedAsset') AND name=N'IX_FixedAsset_Status_DepreciationStartDate' AND is_disabled=0) THROW 51000, '060 history exists but asset index is incomplete', 1;
    PRINT '060_operating_finance already applied';
END
ELSE
BEGIN
    IF OBJECT_ID(N'dbo.Users',N'U') IS NULL THROW 51000, '060 requires dbo.Users', 1;
    IF OBJECT_ID(N'dbo.OperatingExpense',N'U') IS NOT NULL OR OBJECT_ID(N'dbo.FixedAsset',N'U') IS NOT NULL THROW 51000, '060 schema partially exists', 1;
    BEGIN TRY
        BEGIN TRANSACTION;
        DECLARE @lock_result int;
        EXEC @lock_result=sys.sp_getapplock @Resource=N'FastGuyDB:060_operating_finance',@LockMode=N'Exclusive',@LockOwner=N'Transaction',@LockTimeout=15000;
        IF @lock_result<0 THROW 51000, '060 migration lock failed', 1;
        CREATE TABLE dbo.OperatingExpense(
            expense_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_OperatingExpense PRIMARY KEY,
            expense_date date NOT NULL,
            category varchar(20) NOT NULL,
            description nvarchar(500) NOT NULL,
            amount decimal(18,2) NOT NULL,
            created_by int NOT NULL CONSTRAINT FK_OperatingExpense_CreatedBy REFERENCES dbo.Users(user_id),
            created_at datetime2(0) NOT NULL CONSTRAINT DF_OperatingExpense_CreatedAt DEFAULT SYSUTCDATETIME(),
            updated_at datetime2(0) NOT NULL CONSTRAINT DF_OperatingExpense_UpdatedAt DEFAULT SYSUTCDATETIME(),
            CONSTRAINT CK_OperatingExpense_Category CHECK(category IN('RENT','UTILITIES','SALARY','MARKETING','MAINTENANCE','OTHER')),
            CONSTRAINT CK_OperatingExpense_Amount CHECK(amount>0));
        CREATE TABLE dbo.FixedAsset(
            asset_id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_FixedAsset PRIMARY KEY,
            asset_name nvarchar(255) NOT NULL,
            acquisition_cost decimal(18,2) NOT NULL,
            salvage_value decimal(18,2) NOT NULL,
            depreciation_start_date date NOT NULL,
            useful_life_months int NOT NULL,
            status varchar(20) NOT NULL CONSTRAINT DF_FixedAsset_Status DEFAULT 'ACTIVE',
            retired_at datetime2(0) NULL,
            created_by int NOT NULL CONSTRAINT FK_FixedAsset_CreatedBy REFERENCES dbo.Users(user_id),
            created_at datetime2(0) NOT NULL CONSTRAINT DF_FixedAsset_CreatedAt DEFAULT SYSUTCDATETIME(),
            updated_at datetime2(0) NOT NULL CONSTRAINT DF_FixedAsset_UpdatedAt DEFAULT SYSUTCDATETIME(),
            CONSTRAINT CK_FixedAsset_Value CHECK(acquisition_cost>0 AND salvage_value>=0 AND salvage_value<acquisition_cost),
            CONSTRAINT CK_FixedAsset_UsefulLife CHECK(useful_life_months>0),
            CONSTRAINT CK_FixedAsset_Status CHECK(status IN('ACTIVE','RETIRED')),
            CONSTRAINT CK_FixedAsset_Retirement CHECK((status='ACTIVE' AND retired_at IS NULL) OR (status='RETIRED' AND retired_at IS NOT NULL)));
        CREATE INDEX IX_OperatingExpense_ExpenseDate ON dbo.OperatingExpense(expense_date,category);
        CREATE INDEX IX_FixedAsset_Status_DepreciationStartDate ON dbo.FixedAsset(status,depreciation_start_date);
        INSERT dbo.SchemaMigrationHistory(migration_id) VALUES('060_operating_finance');
        COMMIT;
    END TRY
    BEGIN CATCH
        IF XACT_STATE()<>0 ROLLBACK;
        THROW;
    END CATCH;
END;
