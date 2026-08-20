USE FastGuyDB;
GO
SET NOCOUNT ON;
GO
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51482, 'SchemaMigrationHistory is missing.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '047_low_stock_threshold') THROW 51483, '047_low_stock_threshold is missing.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '048_homepage_merchandising') THROW 51484, '048_homepage_merchandising is missing.', 1;

IF EXISTS (
    SELECT required.table_name, required.column_name
    FROM (VALUES
        (N'Product', N'is_new', N'bit', 1, 0),
        (N'Product', N'spice_level', N'tinyint', 1, 0),
        (N'ProductCombo', N'homepage_occasion', N'varchar', 24, 1),
        (N'ProductCombo', N'homepage_sort_order', N'int', 4, 0),
        (N'Review', N'is_featured', N'bit', 1, 0),
        (N'Review', N'homepage_consent', N'bit', 1, 0)
    ) required(table_name, column_name, type_name, max_length, nullable_value)
    LEFT JOIN sys.columns c ON c.object_id = OBJECT_ID(N'dbo.' + required.table_name) AND c.name = required.column_name
    WHERE c.column_id IS NULL OR TYPE_NAME(c.system_type_id) <> required.type_name OR c.user_type_id <> c.system_type_id
       OR c.max_length <> required.max_length OR c.is_nullable <> required.nullable_value OR c.is_identity <> 0
       OR c.is_computed <> 0 OR c.is_rowguidcol <> 0 OR c.is_sparse <> 0
) THROW 51485, 'Homepage merchandising column definition invalid.', 1;

IF EXISTS (
    SELECT required.constraint_name
    FROM (VALUES
        (N'Product', N'is_new', N'DF_Product_IsNew'),
        (N'Product', N'spice_level', N'DF_Product_SpiceLevel'),
        (N'ProductCombo', N'homepage_sort_order', N'DF_ProductCombo_HomepageSortOrder'),
        (N'Review', N'is_featured', N'DF_Review_IsFeatured'),
        (N'Review', N'homepage_consent', N'DF_Review_HomepageConsent')
    ) required(table_name, column_name, constraint_name)
    LEFT JOIN sys.columns c ON c.object_id = OBJECT_ID(N'dbo.' + required.table_name) AND c.name = required.column_name
    LEFT JOIN sys.default_constraints dc ON dc.object_id = c.default_object_id AND dc.name = required.constraint_name
    WHERE dc.object_id IS NULL OR REPLACE(REPLACE(REPLACE(dc.definition, N'(', N''), N')', N''), N' ', N'') <> N'0'
) THROW 51486, 'Homepage merchandising default definition invalid.', 1;

DECLARE @spice_check nvarchar(max) = (
    SELECT LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(definition, N'[', N''), N']', N''), N'(', N''), N')', N''), N' ', N''), CHAR(13), N''), CHAR(10), N''))
    FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.Product') AND name = N'CK_Product_SpiceLevel'
);
IF @spice_check IS NULL OR @spice_check NOT IN (N'spice_levelbetween0and3', N'spice_level>=0andspice_level<=3')
   OR EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.Product') AND name = N'CK_Product_SpiceLevel' AND (is_disabled = 1 OR is_not_trusted = 1))
    THROW 51487, 'CK_Product_SpiceLevel definition invalid.', 1;

DECLARE @occasion_check nvarchar(max) = (
    SELECT LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(definition, N'[', N''), N']', N''), N'(', N''), N')', N''), N' ', N''), CHAR(13), N''), CHAR(10), N''))
    FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.ProductCombo') AND name = N'CK_ProductCombo_HomepageOccasion'
);
IF @occasion_check IS NULL OR @occasion_check NOT IN (
    N'homepage_occasionisnullorhomepage_occasionin''quick_break'',''office_lunch'',''student'',''group''',
    N'homepage_occasionisnullorhomepage_occasion=''quick_break''orhomepage_occasion=''office_lunch''orhomepage_occasion=''student''orhomepage_occasion=''group''',
    N'homepage_occasionisnullorhomepage_occasion=''group''orhomepage_occasion=''student''orhomepage_occasion=''office_lunch''orhomepage_occasion=''quick_break'''
) OR EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.ProductCombo') AND name = N'CK_ProductCombo_HomepageOccasion' AND (is_disabled = 1 OR is_not_trusted = 1))
    THROW 51488, 'CK_ProductCombo_HomepageOccasion definition invalid.', 1;

DECLARE @featured_consent_check nvarchar(max) = (
    SELECT LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(definition, N'[', N''), N']', N''), N'(', N''), N')', N''), N' ', N''), CHAR(13), N''), CHAR(10), N''))
    FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.Review') AND name = N'CK_Review_FeaturedConsent'
);
IF @featured_consent_check <> N'is_featured=0orhomepage_consent=1'
   OR EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.Review') AND name = N'CK_Review_FeaturedConsent' AND (is_disabled = 1 OR is_not_trusted = 1))
    THROW 51489, 'CK_Review_FeaturedConsent definition invalid.', 1;

DECLARE @occasion_filter nvarchar(max) = (
    SELECT LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(filter_definition, N'[', N''), N']', N''), N'(', N''), N')', N''), N' ', N''), CHAR(13), N''), CHAR(10), N''))
    FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.ProductCombo') AND name = N'IX_ProductCombo_HomepageOccasion'
);
IF @occasion_filter <> N'homepage_occasionisnotnullandis_active=1' OR NOT EXISTS (
    SELECT 1 FROM sys.indexes i
    JOIN sys.index_columns ic1 ON ic1.object_id = i.object_id AND ic1.index_id = i.index_id AND ic1.key_ordinal = 1 AND ic1.is_descending_key = 0
    JOIN sys.columns c1 ON c1.object_id = ic1.object_id AND c1.column_id = ic1.column_id AND c1.name = N'homepage_occasion'
    JOIN sys.index_columns ic2 ON ic2.object_id = i.object_id AND ic2.index_id = i.index_id AND ic2.key_ordinal = 2 AND ic2.is_descending_key = 0
    JOIN sys.columns c2 ON c2.object_id = ic2.object_id AND c2.column_id = ic2.column_id AND c2.name = N'homepage_sort_order'
    WHERE i.object_id = OBJECT_ID(N'dbo.ProductCombo') AND i.name = N'IX_ProductCombo_HomepageOccasion'
      AND i.type = 2 AND i.is_unique = 0 AND i.is_disabled = 0 AND i.has_filter = 1
      AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id AND ic.key_ordinal > 0) = 2
      AND NOT EXISTS (SELECT 1 FROM sys.index_columns ic WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id AND ic.is_included_column = 1)
) THROW 51489, 'IX_ProductCombo_HomepageOccasion definition invalid.', 1;
DECLARE @featured_filter nvarchar(max) = (
    SELECT LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(filter_definition, N'[', N''), N']', N''), N'(', N''), N')', N''), N' ', N''), CHAR(13), N''), CHAR(10), N''))
    FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Review') AND name = N'IX_Review_FeaturedCreatedAt'
);
IF @featured_filter <> N'is_featured=1' OR NOT EXISTS (
    SELECT 1 FROM sys.indexes i
    JOIN sys.index_columns ic1 ON ic1.object_id = i.object_id AND ic1.index_id = i.index_id AND ic1.key_ordinal = 1 AND ic1.is_descending_key = 0
    JOIN sys.columns c1 ON c1.object_id = ic1.object_id AND c1.column_id = ic1.column_id AND c1.name = N'is_featured'
    JOIN sys.index_columns ic2 ON ic2.object_id = i.object_id AND ic2.index_id = i.index_id AND ic2.key_ordinal = 2 AND ic2.is_descending_key = 1
    JOIN sys.columns c2 ON c2.object_id = ic2.object_id AND c2.column_id = ic2.column_id AND c2.name = N'created_at'
    WHERE i.object_id = OBJECT_ID(N'dbo.Review') AND i.name = N'IX_Review_FeaturedCreatedAt'
      AND i.type = 2 AND i.is_unique = 0 AND i.is_disabled = 0 AND i.has_filter = 1
      AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id AND ic.key_ordinal > 0) = 2
      AND NOT EXISTS (SELECT 1 FROM sys.index_columns ic WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id AND ic.is_included_column = 1)
) THROW 51490, 'IX_Review_FeaturedCreatedAt definition invalid.', 1;

IF EXISTS (SELECT 1 FROM dbo.Product WHERE spice_level NOT BETWEEN 0 AND 3) THROW 51491, 'Product spice_level data is outside 0..3.', 1;
IF EXISTS (SELECT 1 FROM dbo.ProductCombo WHERE homepage_occasion IS NOT NULL AND homepage_occasion NOT IN ('QUICK_BREAK', 'OFFICE_LUNCH', 'STUDENT', 'GROUP')) THROW 51492, 'ProductCombo homepage_occasion data is invalid.', 1;
IF EXISTS (SELECT 1 FROM dbo.Review WHERE is_featured = 1 AND homepage_consent = 0) THROW 51493, 'Review featured consent data invariant is violated.', 1;
PRINT '048 homepage merchandising validation passed.';
GO
