USE FastGuyDB;
GO
SET NOCOUNT ON;
GO
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51520, 'SchemaMigrationHistory is missing.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '049_category_images') THROW 51521, '049_category_images is missing.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '050_product_scoped_reviews') THROW 51522, '050_product_scoped_reviews is missing.', 1;

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.Review') AND name = N'product_id'
      AND TYPE_NAME(system_type_id) = N'int' AND max_length = 4 AND is_nullable = 0
      AND is_identity = 0 AND is_computed = 0
) THROW 51523, 'Review product_id definition invalid.', 1;

IF EXISTS (
    SELECT expected.constraint_name
    FROM (VALUES
        (CAST(NULL AS sysname), N'user_id', N'Users', N'user_id'),
        (CAST(NULL AS sysname), N'order_id', N'Orders', N'order_id'),
        (N'FK_Review_Product', N'product_id', N'Product', N'product_id')
    ) expected(constraint_name, parent_column, referenced_table, referenced_column)
    LEFT JOIN sys.foreign_keys fk ON fk.parent_object_id = OBJECT_ID(N'dbo.Review')
      AND fk.referenced_object_id = OBJECT_ID(N'dbo.' + expected.referenced_table)
      AND (expected.constraint_name IS NULL OR fk.name = expected.constraint_name)
      AND EXISTS (
          SELECT 1 FROM sys.foreign_key_columns mapped
          JOIN sys.columns mapped_parent ON mapped_parent.object_id = mapped.parent_object_id AND mapped_parent.column_id = mapped.parent_column_id
          JOIN sys.columns mapped_reference ON mapped_reference.object_id = mapped.referenced_object_id AND mapped_reference.column_id = mapped.referenced_column_id
          WHERE mapped.constraint_object_id = fk.object_id AND mapped_parent.name = expected.parent_column AND mapped_reference.name = expected.referenced_column
      )
    LEFT JOIN sys.foreign_key_columns fkc ON fkc.constraint_object_id = fk.object_id
    LEFT JOIN sys.columns pc ON pc.object_id = fkc.parent_object_id AND pc.column_id = fkc.parent_column_id
    LEFT JOIN sys.columns rc ON rc.object_id = fkc.referenced_object_id AND rc.column_id = fkc.referenced_column_id
    WHERE fk.object_id IS NULL OR fk.referenced_object_id <> OBJECT_ID(N'dbo.' + expected.referenced_table)
       OR fk.is_disabled <> 0 OR fk.is_not_trusted <> 0 OR pc.name <> expected.parent_column OR rc.name <> expected.referenced_column
       OR (SELECT COUNT(*) FROM sys.foreign_key_columns x WHERE x.constraint_object_id = fk.object_id) <> 1
) OR (SELECT COUNT(*) FROM sys.foreign_keys fk WHERE fk.parent_object_id = OBJECT_ID(N'dbo.Review')) <> 3
    THROW 51524, 'Review foreign key definitions invalid.', 1;

IF EXISTS (
    SELECT 1 FROM sys.indexes i
    JOIN sys.index_columns ic1 ON ic1.object_id = i.object_id AND ic1.index_id = i.index_id AND ic1.key_ordinal = 1
    JOIN sys.columns c1 ON c1.object_id = ic1.object_id AND c1.column_id = ic1.column_id AND c1.name = N'user_id'
    JOIN sys.index_columns ic2 ON ic2.object_id = i.object_id AND ic2.index_id = i.index_id AND ic2.key_ordinal = 2
    JOIN sys.columns c2 ON c2.object_id = ic2.object_id AND c2.column_id = ic2.column_id AND c2.name = N'order_id'
    WHERE i.object_id = OBJECT_ID(N'dbo.Review') AND i.is_unique_constraint = 1
      AND (SELECT COUNT(*) FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.key_ordinal > 0) = 2
) THROW 51525, 'Legacy Review user/order unique still exists.', 1;
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes i
    JOIN sys.index_columns ic1 ON ic1.object_id = i.object_id AND ic1.index_id = i.index_id AND ic1.key_ordinal = 1 AND ic1.is_descending_key = 0
    JOIN sys.columns c1 ON c1.object_id = ic1.object_id AND c1.column_id = ic1.column_id AND c1.name = N'user_id'
    JOIN sys.index_columns ic2 ON ic2.object_id = i.object_id AND ic2.index_id = i.index_id AND ic2.key_ordinal = 2 AND ic2.is_descending_key = 0
    JOIN sys.columns c2 ON c2.object_id = ic2.object_id AND c2.column_id = ic2.column_id AND c2.name = N'order_id'
    JOIN sys.index_columns ic3 ON ic3.object_id = i.object_id AND ic3.index_id = i.index_id AND ic3.key_ordinal = 3 AND ic3.is_descending_key = 0
    JOIN sys.columns c3 ON c3.object_id = ic3.object_id AND c3.column_id = ic3.column_id AND c3.name = N'product_id'
    WHERE i.object_id = OBJECT_ID(N'dbo.Review') AND i.name = N'UQ_Review_UserOrderProduct'
      AND i.is_unique = 1 AND i.is_unique_constraint = 1 AND i.is_disabled = 0 AND i.has_filter = 0
      AND (SELECT COUNT(*) FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.key_ordinal > 0) = 3
      AND NOT EXISTS (SELECT 1 FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.is_included_column = 1)
) THROW 51526, 'UQ_Review_UserOrderProduct definition invalid.', 1;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes i
    JOIN sys.index_columns ic1 ON ic1.object_id = i.object_id AND ic1.index_id = i.index_id AND ic1.key_ordinal = 1 AND ic1.is_descending_key = 0
    JOIN sys.columns c1 ON c1.object_id = ic1.object_id AND c1.column_id = ic1.column_id AND c1.name = N'product_id'
    JOIN sys.index_columns ic2 ON ic2.object_id = i.object_id AND ic2.index_id = i.index_id AND ic2.key_ordinal = 2 AND ic2.is_descending_key = 1
    JOIN sys.columns c2 ON c2.object_id = ic2.object_id AND c2.column_id = ic2.column_id AND c2.name = N'created_at'
    JOIN sys.index_columns ic3 ON ic3.object_id = i.object_id AND ic3.index_id = i.index_id AND ic3.key_ordinal = 3 AND ic3.is_descending_key = 1
    JOIN sys.columns c3 ON c3.object_id = ic3.object_id AND c3.column_id = ic3.column_id AND c3.name = N'review_id'
    WHERE i.object_id = OBJECT_ID(N'dbo.Review') AND i.name = N'IX_Review_ProductCreatedAt'
      AND i.type = 2 AND i.is_unique = 0 AND i.is_unique_constraint = 0 AND i.is_disabled = 0 AND i.has_filter = 0
      AND (SELECT COUNT(*) FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.key_ordinal > 0) = 3
      AND NOT EXISTS (SELECT 1 FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.is_included_column = 1)
) THROW 51527, 'IX_Review_ProductCreatedAt definition invalid.', 1;

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
      AND i.type = 2 AND i.is_unique = 0 AND i.is_unique_constraint = 0 AND i.is_disabled = 0 AND i.has_filter = 1
      AND (SELECT COUNT(*) FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.key_ordinal > 0) = 2
      AND NOT EXISTS (SELECT 1 FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.is_included_column = 1)
) THROW 51533, 'IX_Review_FeaturedCreatedAt definition invalid.', 1;

IF (SELECT COUNT(*) FROM sys.indexes i WHERE i.object_id = OBJECT_ID(N'dbo.Review') AND i.index_id > 1) <> 3
    THROW 51534, 'Review has unexpected non-PK indexes.', 1;

IF EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'dbo.Review') AND name IN (N'CK_Review_Rating', N'CK_Review_FeaturedConsent')
      AND (is_disabled = 1 OR is_not_trusted = 1)
) OR (SELECT COUNT(*) FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.Review') AND name IN (N'CK_Review_Rating', N'CK_Review_FeaturedConsent')) <> 2
    THROW 51528, 'Review checks are missing, disabled, or untrusted.', 1;

IF EXISTS (SELECT 1 FROM dbo.Review WHERE product_id IS NULL) THROW 51529, 'Review contains null product_id.', 1;
IF EXISTS (SELECT 1 FROM dbo.Review r LEFT JOIN dbo.Product p ON p.product_id = r.product_id WHERE p.product_id IS NULL) THROW 51530, 'Review contains orphan product_id.', 1;
IF EXISTS (
    SELECT user_id, order_id, product_id
    FROM dbo.Review
    GROUP BY user_id, order_id, product_id
    HAVING COUNT(*) > 1
) THROW 51531, 'Review contains duplicate user/order/product triples.', 1;

SELECT COUNT_BIG(*) AS total_reviews,
       SUM(CASE WHEN r.product_id IS NULL THEN CONVERT(bigint, 1) ELSE CONVERT(bigint, 0) END) AS null_products,
       SUM(CASE WHEN p.product_id IS NULL THEN CONVERT(bigint, 1) ELSE CONVERT(bigint, 0) END) AS orphan_reviews,
       (SELECT COUNT_BIG(*) FROM (
           SELECT user_id, order_id, product_id
           FROM dbo.Review
           GROUP BY user_id, order_id, product_id
           HAVING COUNT(*) > 1
       ) duplicate_groups) AS duplicate_triples
FROM dbo.Review r
LEFT JOIN dbo.Product p ON p.product_id = r.product_id;
PRINT '050 product scoped reviews validation passed.';
GO
