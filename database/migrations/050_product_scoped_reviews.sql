USE FastGuyDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;
GO
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51500, 'Run 000_preflight_history.sql first.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '049_category_images') THROW 51501, 'Run 049_category_images.sql first.', 1;

IF EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '050_product_scoped_reviews')
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.Review') AND name = N'product_id'
          AND TYPE_NAME(system_type_id) = N'int' AND max_length = 4 AND is_nullable = 0
          AND is_identity = 0 AND is_computed = 0
    ) OR EXISTS (
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
      OR NOT EXISTS (
        SELECT 1 FROM sys.indexes i
        JOIN sys.index_columns ic1 ON ic1.object_id = i.object_id AND ic1.index_id = i.index_id AND ic1.key_ordinal = 1
        JOIN sys.columns c1 ON c1.object_id = ic1.object_id AND c1.column_id = ic1.column_id AND c1.name = N'user_id'
        JOIN sys.index_columns ic2 ON ic2.object_id = i.object_id AND ic2.index_id = i.index_id AND ic2.key_ordinal = 2
        JOIN sys.columns c2 ON c2.object_id = ic2.object_id AND c2.column_id = ic2.column_id AND c2.name = N'order_id'
        JOIN sys.index_columns ic3 ON ic3.object_id = i.object_id AND ic3.index_id = i.index_id AND ic3.key_ordinal = 3
        JOIN sys.columns c3 ON c3.object_id = ic3.object_id AND c3.column_id = ic3.column_id AND c3.name = N'product_id'
        WHERE i.object_id = OBJECT_ID(N'dbo.Review') AND i.name = N'UQ_Review_UserOrderProduct' AND i.type = 2
          AND i.is_unique = 1 AND i.is_unique_constraint = 1 AND i.is_disabled = 0 AND i.has_filter = 0
          AND ic1.is_descending_key = 0 AND ic2.is_descending_key = 0 AND ic3.is_descending_key = 0
          AND (SELECT COUNT(*) FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.key_ordinal > 0) = 3
          AND NOT EXISTS (SELECT 1 FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.is_included_column = 1)
    ) OR NOT EXISTS (
        SELECT 1 FROM sys.indexes i
        JOIN sys.index_columns ic1 ON ic1.object_id = i.object_id AND ic1.index_id = i.index_id AND ic1.key_ordinal = 1
        JOIN sys.columns c1 ON c1.object_id = ic1.object_id AND c1.column_id = ic1.column_id AND c1.name = N'product_id'
        JOIN sys.index_columns ic2 ON ic2.object_id = i.object_id AND ic2.index_id = i.index_id AND ic2.key_ordinal = 2
        JOIN sys.columns c2 ON c2.object_id = ic2.object_id AND c2.column_id = ic2.column_id AND c2.name = N'created_at'
        JOIN sys.index_columns ic3 ON ic3.object_id = i.object_id AND ic3.index_id = i.index_id AND ic3.key_ordinal = 3
        JOIN sys.columns c3 ON c3.object_id = ic3.object_id AND c3.column_id = ic3.column_id AND c3.name = N'review_id'
        WHERE i.object_id = OBJECT_ID(N'dbo.Review') AND i.name = N'IX_Review_ProductCreatedAt' AND i.type = 2
          AND i.is_unique = 0 AND i.is_unique_constraint = 0 AND i.is_disabled = 0 AND i.has_filter = 0
          AND ic1.is_descending_key = 0 AND ic2.is_descending_key = 1 AND ic3.is_descending_key = 1
          AND (SELECT COUNT(*) FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.key_ordinal > 0) = 3
          AND NOT EXISTS (SELECT 1 FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.is_included_column = 1)
    ) OR NOT EXISTS (
        SELECT 1 FROM sys.indexes i
        JOIN sys.index_columns ic1 ON ic1.object_id = i.object_id AND ic1.index_id = i.index_id AND ic1.key_ordinal = 1 AND ic1.is_descending_key = 0
        JOIN sys.columns c1 ON c1.object_id = ic1.object_id AND c1.column_id = ic1.column_id AND c1.name = N'is_featured'
        JOIN sys.index_columns ic2 ON ic2.object_id = i.object_id AND ic2.index_id = i.index_id AND ic2.key_ordinal = 2 AND ic2.is_descending_key = 1
        JOIN sys.columns c2 ON c2.object_id = ic2.object_id AND c2.column_id = ic2.column_id AND c2.name = N'created_at'
        WHERE i.object_id = OBJECT_ID(N'dbo.Review') AND i.name = N'IX_Review_FeaturedCreatedAt'
          AND i.type = 2 AND i.is_unique = 0 AND i.is_unique_constraint = 0 AND i.is_disabled = 0 AND i.has_filter = 1
          AND LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(i.filter_definition, N'[', N''), N']', N''), N'(', N''), N')', N''), N' ', N''), CHAR(13), N''), CHAR(10), N'')) = N'is_featured=1'
          AND (SELECT COUNT(*) FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.key_ordinal > 0) = 2
          AND NOT EXISTS (SELECT 1 FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.is_included_column = 1)
    ) OR (SELECT COUNT(*) FROM sys.indexes i WHERE i.object_id = OBJECT_ID(N'dbo.Review') AND i.index_id > 1) <> 3
      OR EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.Review') AND name IN (N'CK_Review_Rating', N'CK_Review_FeaturedConsent') AND (is_disabled = 1 OR is_not_trusted = 1))
      OR (SELECT COUNT(*) FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.Review') AND name IN (N'CK_Review_Rating', N'CK_Review_FeaturedConsent')) <> 2
        THROW 51502, '050 migration history exists but Review schema is incomplete.', 1;
    EXEC sys.sp_executesql N'IF EXISTS (SELECT 1 FROM dbo.Review r LEFT JOIN dbo.Product p ON p.product_id = r.product_id WHERE r.product_id IS NULL OR p.product_id IS NULL)
      OR EXISTS (SELECT 1 FROM dbo.Review GROUP BY user_id, order_id, product_id HAVING COUNT(*) > 1)
        THROW 51502, ''050 migration history exists but Review data is incomplete.'', 1;';
    PRINT '050_product_scoped_reviews already applied.';
END
ELSE
BEGIN
    IF OBJECT_ID(N'dbo.Review', N'U') IS NULL OR OBJECT_ID(N'dbo.Product', N'U') IS NULL OR OBJECT_ID(N'dbo.OrderItem', N'U') IS NULL
      OR COL_LENGTH(N'dbo.Review', N'product_id') IS NOT NULL
      OR OBJECT_ID(N'dbo.UQ_Review_UserOrderProduct', N'UQ') IS NOT NULL
      OR OBJECT_ID(N'dbo.FK_Review_Product', N'F') IS NOT NULL
      OR EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Review') AND name = N'IX_Review_ProductCreatedAt')
      OR NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.OrderItem') AND name = N'product_id' AND TYPE_NAME(system_type_id) = N'int')
        THROW 51503, 'Pre-migration Review schema shape invalid.', 1;

    IF EXISTS (
        SELECT expected.constraint_name
        FROM (VALUES
            (CAST(NULL AS sysname), N'user_id', N'Users', N'user_id'),
            (CAST(NULL AS sysname), N'order_id', N'Orders', N'order_id')
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
    ) OR (SELECT COUNT(*) FROM sys.foreign_keys fk WHERE fk.parent_object_id = OBJECT_ID(N'dbo.Review')) <> 2
        THROW 51504, 'Pre-migration Review foreign keys invalid.', 1;

    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes i
        JOIN sys.index_columns ic1 ON ic1.object_id = i.object_id AND ic1.index_id = i.index_id AND ic1.key_ordinal = 1 AND ic1.is_descending_key = 0
        JOIN sys.columns c1 ON c1.object_id = ic1.object_id AND c1.column_id = ic1.column_id AND c1.name = N'user_id'
        JOIN sys.index_columns ic2 ON ic2.object_id = i.object_id AND ic2.index_id = i.index_id AND ic2.key_ordinal = 2 AND ic2.is_descending_key = 0
        JOIN sys.columns c2 ON c2.object_id = ic2.object_id AND c2.column_id = ic2.column_id AND c2.name = N'order_id'
        WHERE i.object_id = OBJECT_ID(N'dbo.Review') AND i.type = 2
          AND i.is_unique = 1 AND i.is_unique_constraint = 1 AND i.is_disabled = 0 AND i.has_filter = 0
          AND (SELECT COUNT(*) FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.key_ordinal > 0) = 2
          AND NOT EXISTS (SELECT 1 FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.is_included_column = 1)
    ) THROW 51512, 'Pre-migration UQ_Review_UserOrder invalid.', 1;

    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes i
        JOIN sys.index_columns ic1 ON ic1.object_id = i.object_id AND ic1.index_id = i.index_id AND ic1.key_ordinal = 1 AND ic1.is_descending_key = 0
        JOIN sys.columns c1 ON c1.object_id = ic1.object_id AND c1.column_id = ic1.column_id AND c1.name = N'is_featured'
        JOIN sys.index_columns ic2 ON ic2.object_id = i.object_id AND ic2.index_id = i.index_id AND ic2.key_ordinal = 2 AND ic2.is_descending_key = 1
        JOIN sys.columns c2 ON c2.object_id = ic2.object_id AND c2.column_id = ic2.column_id AND c2.name = N'created_at'
        WHERE i.object_id = OBJECT_ID(N'dbo.Review') AND i.name = N'IX_Review_FeaturedCreatedAt'
          AND i.type = 2 AND i.is_unique = 0 AND i.is_unique_constraint = 0 AND i.is_disabled = 0 AND i.has_filter = 1
          AND LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(i.filter_definition, N'[', N''), N']', N''), N'(', N''), N')', N''), N' ', N''), CHAR(13), N''), CHAR(10), N'')) = N'is_featured=1'
          AND (SELECT COUNT(*) FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.key_ordinal > 0) = 2
          AND NOT EXISTS (SELECT 1 FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.is_included_column = 1)
    ) THROW 51514, 'Pre-migration IX_Review_FeaturedCreatedAt invalid.', 1;

    IF (SELECT COUNT(*) FROM sys.indexes i WHERE i.object_id = OBJECT_ID(N'dbo.Review') AND i.index_id > 1) <> 2
        THROW 51515, 'Pre-migration Review has unexpected non-PK indexes.', 1;

    IF EXISTS (
        SELECT expected.constraint_name
        FROM (VALUES (N'CK_Review_Rating'), (N'CK_Review_FeaturedConsent')) expected(constraint_name)
        LEFT JOIN sys.check_constraints cc ON cc.parent_object_id = OBJECT_ID(N'dbo.Review') AND cc.name = expected.constraint_name
        WHERE cc.object_id IS NULL OR cc.is_disabled <> 0 OR cc.is_not_trusted <> 0
    ) OR (SELECT COUNT(*) FROM sys.check_constraints cc WHERE cc.parent_object_id = OBJECT_ID(N'dbo.Review')) <> 2
        THROW 51516, 'Pre-migration Review checks invalid.', 1;

    BEGIN TRY
        BEGIN TRANSACTION;

        DECLARE @reviews_before bigint = (SELECT COUNT_BIG(*) FROM dbo.Review);
        DECLARE @reviews_backfilled bigint;
        DECLARE @reviews_deleted bigint;

        ALTER TABLE dbo.Review ADD product_id int NULL;
        IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Review') AND name = N'product_id' AND TYPE_NAME(system_type_id) = N'int' AND is_nullable = 1)
            THROW 51505, 'Review product_id expansion failed.', 1;

        EXEC sys.sp_executesql N';WITH ExactProduct AS (
            SELECT r.review_id, MIN(oi.product_id) AS product_id
            FROM dbo.Review r
            JOIN dbo.OrderItem oi ON oi.order_id = r.order_id
            GROUP BY r.review_id
            HAVING COUNT(DISTINCT oi.product_id) = 1
        )
        UPDATE r SET product_id = ep.product_id
        FROM dbo.Review r
        JOIN ExactProduct ep ON ep.review_id = r.review_id;
        SET @count = @@ROWCOUNT;', N'@count bigint OUTPUT', @count = @reviews_backfilled OUTPUT;

        EXEC sys.sp_executesql N'DELETE FROM dbo.Review WHERE product_id IS NULL;
        SET @count = @@ROWCOUNT;', N'@count bigint OUTPUT', @count = @reviews_deleted OUTPUT;
        IF @reviews_backfilled + @reviews_deleted <> @reviews_before
            THROW 51506, 'Review backfill audit counts do not reconcile.', 1;

        ALTER TABLE dbo.Review WITH CHECK ADD CONSTRAINT FK_Review_Product FOREIGN KEY (product_id) REFERENCES dbo.Product(product_id);
        IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.Review') AND name = N'FK_Review_Product' AND is_disabled = 0 AND is_not_trusted = 0)
            THROW 51507, 'FK_Review_Product creation failed.', 1;

        ALTER TABLE dbo.Review ALTER COLUMN product_id int NOT NULL;
        IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.Review') AND name = N'product_id' AND TYPE_NAME(system_type_id) = N'int' AND is_nullable = 0)
            THROW 51508, 'Review product_id nullability change failed.', 1;

        DECLARE @legacy_unique sysname = (
            SELECT i.name FROM sys.indexes i
            JOIN sys.index_columns ic1 ON ic1.object_id = i.object_id AND ic1.index_id = i.index_id AND ic1.key_ordinal = 1
            JOIN sys.columns c1 ON c1.object_id = ic1.object_id AND c1.column_id = ic1.column_id AND c1.name = N'user_id'
            JOIN sys.index_columns ic2 ON ic2.object_id = i.object_id AND ic2.index_id = i.index_id AND ic2.key_ordinal = 2
            JOIN sys.columns c2 ON c2.object_id = ic2.object_id AND c2.column_id = ic2.column_id AND c2.name = N'order_id'
            WHERE i.object_id = OBJECT_ID(N'dbo.Review') AND i.is_unique_constraint = 1
              AND (SELECT COUNT(*) FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.key_ordinal > 0) = 2
        );
        DECLARE @drop_unique_sql nvarchar(1000) = N'ALTER TABLE dbo.Review DROP CONSTRAINT ' + QUOTENAME(@legacy_unique);
        EXEC (@drop_unique_sql);
        IF EXISTS (
            SELECT 1 FROM sys.indexes i
            JOIN sys.index_columns ic1 ON ic1.object_id = i.object_id AND ic1.index_id = i.index_id AND ic1.key_ordinal = 1
            JOIN sys.columns c1 ON c1.object_id = ic1.object_id AND c1.column_id = ic1.column_id AND c1.name = N'user_id'
            JOIN sys.index_columns ic2 ON ic2.object_id = i.object_id AND ic2.index_id = i.index_id AND ic2.key_ordinal = 2
            JOIN sys.columns c2 ON c2.object_id = ic2.object_id AND c2.column_id = ic2.column_id AND c2.name = N'order_id'
            WHERE i.object_id = OBJECT_ID(N'dbo.Review') AND i.is_unique_constraint = 1
              AND (SELECT COUNT(*) FROM sys.index_columns x WHERE x.object_id = i.object_id AND x.index_id = i.index_id AND x.key_ordinal > 0) = 2
        ) THROW 51509, 'Legacy Review user/order unique removal failed.', 1;

        ALTER TABLE dbo.Review ADD CONSTRAINT UQ_Review_UserOrderProduct UNIQUE (user_id, order_id, product_id);
        IF OBJECT_ID(N'dbo.UQ_Review_UserOrderProduct', N'UQ') IS NULL
            THROW 51510, 'UQ_Review_UserOrderProduct creation failed.', 1;

        CREATE INDEX IX_Review_ProductCreatedAt ON dbo.Review(product_id, created_at DESC, review_id DESC);
        IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Review') AND name = N'IX_Review_ProductCreatedAt' AND is_disabled = 0)
            THROW 51511, 'IX_Review_ProductCreatedAt creation failed.', 1;

        INSERT dbo.SchemaMigrationHistory(migration_id, details)
        VALUES ('050_product_scoped_reviews', N'Scoped reviews to purchased products and removed ambiguous legacy rows');

        COMMIT TRANSACTION;

        SELECT @reviews_before AS reviews_before, @reviews_backfilled AS reviews_backfilled, @reviews_deleted AS reviews_deleted,
               COUNT_BIG(*) AS reviews_after
        FROM dbo.Review;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH;
END;
GO
