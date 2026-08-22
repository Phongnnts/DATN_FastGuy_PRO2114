package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class ProductScopedReviewMigrationPolicyTest {
    private static final Path ROOT = Path.of("../..");

    @Test
    void migrationBackfillsExactProductsAuditsDeletionAndGuardsShape() throws Exception {
        String migration = Files.readString(ROOT.resolve("database/migrations/050_product_scoped_reviews.sql"));

        for (String token : new String[] {
                "migration_id = '049_category_images'", "migration_id = '050_product_scoped_reviews'",
                "SET XACT_ABORT ON", "SET QUOTED_IDENTIFIER ON", "BEGIN TRY", "BEGIN CATCH", "BEGIN TRANSACTION", "THROW;",
                "HAVING COUNT(DISTINCT oi.product_id) = 1", "DELETE FROM dbo.Review WHERE product_id IS NULL",
                "FK_Review_Product", "UQ_Review_UserOrderProduct", "IX_Review_ProductCreatedAt",
                "@reviews_before", "@reviews_backfilled", "@reviews_deleted"}) {
            assertTrue(migration.contains(token), token);
        }
        assertFalse(migration.contains("@reviews_backfilled = 16"));
        assertFalse(migration.contains("@reviews_deleted = 1"));
        assertTrue(migration.indexOf("Pre-migration Review schema shape invalid.") < migration.indexOf("ALTER TABLE dbo.Review ADD product_id"));
        assertTrue(migration.contains("EXEC sys.sp_executesql N'IF EXISTS (SELECT 1 FROM dbo.Review r LEFT JOIN dbo.Product p ON p.product_id = r.product_id"));
        assertTrue(migration.contains("EXEC sys.sp_executesql N';WITH ExactProduct AS ("));
        assertTrue(migration.contains("EXEC sys.sp_executesql N'DELETE FROM dbo.Review WHERE product_id IS NULL;"));
        assertTrue(migration.indexOf("INSERT dbo.SchemaMigrationHistory") > migration.indexOf("CREATE INDEX IX_Review_ProductCreatedAt"));
    }

    @Test
    void initialPathValidatesExact049ReviewContractBeforeAnyWrite() throws Exception {
        String migration = compact(Files.readString(ROOT.resolve("database/migrations/050_product_scoped_reviews.sql")));
        String initial = migration.substring(migration.indexOf("ELSEBEGIN") + "ELSEBEGIN".length());
        String preflight = initial.substring(0, initial.indexOf("BEGINTRY"));

        assertTrue(preflight.contains("(CAST(NULLASsysname),N'user_id',N'Users',N'user_id')"));
        assertTrue(preflight.contains("(CAST(NULLASsysname),N'order_id',N'Orders',N'order_id')"));
        assertFalse(preflight.contains("(N'FK_Review_Product',N'product_id',N'Product',N'product_id')"));
        assertTrue(preflight.contains("COUNT(*)FROMsys.foreign_keysfkWHEREfk.parent_object_id=OBJECT_ID(N'dbo.Review'))<>2"));
        assertFalse(preflight.contains("i.name=N'UQ_Review_UserOrder'"));
        assertTrue(preflight.contains("i.is_unique_constraint=1"));
        assertTrue(preflight.contains("c1.name=N'user_id'") && preflight.contains("c2.name=N'order_id'"));
        assertFalse(preflight.contains("i.name=N'IX_Review_Order'"));
        assertTrue(preflight.contains("i.name=N'IX_Review_FeaturedCreatedAt'"));
        assertTrue(preflight.contains("COUNT(*)FROMsys.indexesiWHEREi.object_id=OBJECT_ID(N'dbo.Review')ANDi.index_id>1") && preflight.contains("<>2"));
        assertTrue(preflight.contains("(N'CK_Review_Rating')") && preflight.contains("(N'CK_Review_FeaturedConsent')"));
        assertTrue(preflight.contains("COUNT(*)FROMsys.check_constraintsccWHEREcc.parent_object_id=OBJECT_ID(N'dbo.Review'))<>2"));
        assertTrue(migration.indexOf("BEGINTRY") < migration.indexOf("BEGINTRANSACTION"));
        assertTrue(migration.indexOf("BEGINTRANSACTION") < migration.indexOf("ALTERTABLEdbo.ReviewADDproduct_id"));
    }

    @Test
    void validatorRequiresExactRetainedForeignKeysIndexesAndUniqueConstraint() throws Exception {
        String validator = compact(Files.readString(ROOT.resolve("database/migrations/050_validate.sql")));
        String migration = compact(Files.readString(ROOT.resolve("database/migrations/050_product_scoped_reviews.sql")));

        for (String source : new String[] {validator, migration.substring(0, migration.indexOf("ELSEBEGIN"))}) {
            assertTrue(source.contains("(CAST(NULLASsysname),N'user_id',N'Users',N'user_id')"));
            assertTrue(source.contains("(CAST(NULLASsysname),N'order_id',N'Orders',N'order_id')"));
            assertTrue(source.contains("(N'FK_Review_Product',N'product_id',N'Product',N'product_id')"));
            assertTrue(source.contains("fk.is_disabled<>0ORfk.is_not_trusted<>0") || source.contains("fk.is_disabled=0ANDfk.is_not_trusted=0"));
            assertTrue(source.contains("i.is_unique_constraint=1"));
            assertFalse(source.contains("i.name=N'IX_Review_Order'"));
            assertTrue(source.contains("i.name=N'IX_Review_FeaturedCreatedAt'") && source.contains("i.has_filter=1"));
            assertTrue(source.contains("i.name=N'IX_Review_ProductCreatedAt'") && source.contains("c3.name=N'review_id'"));
            assertTrue(source.contains("COUNT(*)FROMsys.indexesiWHEREi.object_id=OBJECT_ID(N'dbo.Review')ANDi.index_id>1") && source.contains("<>3"));
        }
        assertTrue(validator.contains("HAVINGCOUNT(*)>1"));
        assertTrue(validator.contains("SUM(CASEWHENr.product_idISNULL"));
        assertTrue(validator.contains("orphan_reviews") && validator.contains("null_products") && validator.contains("duplicate_triples"));
        assertTrue(validator.contains("050productscopedreviewsvalidationpassed."));
    }

    @Test
    void canonicalSchemasSeedAndRunbookUseProductScopedReviews() throws Exception {
        String init = Files.readString(ROOT.resolve("database/init.sql"));
        String snapshot = Files.readString(ROOT.resolve("database/DB_FastGuy.sql"));
        String seed = Files.readString(ROOT.resolve("database/seed_demo.sql"));
        String runbook = Files.readString(ROOT.resolve("database/migrations/RUNBOOK.md"));

        Set<String> expectedIndexes = Set.of(
                "IX_Review_Order|order_id",
                "IX_Review_ProductCreatedAt|product_id, created_at DESC, review_id DESC",
                "IX_Review_FeaturedCreatedAt|is_featured, created_at DESC|is_featured = 1");
        for (String schema : new String[] {init, snapshot}) {
            assertTrue(schema.contains("product_id int NOT NULL CONSTRAINT FK_Review_Product REFERENCES dbo.Product(product_id)"));
            assertTrue(schema.contains("CONSTRAINT UQ_Review_UserOrderProduct UNIQUE (user_id, order_id, product_id)"));
            assertFalse(schema.contains("CONSTRAINT UQ_Review_UserOrder UNIQUE (user_id, order_id)"));
            assertTrue(reviewIndexes(schema).equals(expectedIndexes), reviewIndexes(schema).toString());
        }
        assertTrue(seed.contains("migration_id = '050_product_scoped_reviews'"));
        assertTrue(seed.contains("INSERT dbo.Review(user_id,order_id,product_id,rating,comment,created_at,updated_at)"));
        assertTrue(seed.contains("r.product_id=oi.product_id"));
        assertTrue(runbook.contains("050_product_scoped_reviews.sql"));
        assertTrue(runbook.contains("050_validate.sql"));
        assertTrue(runbook.contains("fresh disposable"));
        assertTrue(runbook.contains("separate approval"));
        assertTrue(runbook.contains("Never run `init.sql` against retained data."));
    }

    private static String compact(String source) {
        return source.replaceAll("\\s+", "");
    }

    private static Set<String> reviewIndexes(String schema) {
        Matcher matcher = Pattern.compile("CREATE INDEX (IX_Review_[A-Za-z]+) ON dbo\\.Review\\(([^)]*)\\)(?: WHERE ([^;]*))?;").matcher(schema);
        Set<String> indexes = new LinkedHashSet<>();
        while (matcher.find()) {
            indexes.add(matcher.group(1) + "|" + matcher.group(2) + (matcher.group(3) == null ? "" : "|" + matcher.group(3)));
        }
        return indexes;
    }
}
