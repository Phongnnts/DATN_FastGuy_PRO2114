package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class HomepageMerchandisingMigrationPolicyTest {
    private static final Path ROOT = Path.of("../..");

    @Test
    void migrationAddsGuardedHomepageMetadataAfter047() throws Exception {
        String migration = Files.readString(ROOT.resolve("database/migrations/048_homepage_merchandising.sql"));

        for (String token : new String[] {
                "migration_id = '047_low_stock_threshold'", "migration_id = '048_homepage_merchandising'",
                "is_new", "spice_level", "homepage_occasion", "homepage_sort_order", "is_featured", "homepage_consent",
                "CK_Product_SpiceLevel", "CK_ProductCombo_HomepageOccasion", "CK_Review_FeaturedConsent",
                "IX_ProductCombo_HomepageOccasion", "IX_Review_FeaturedCreatedAt",
                "SET XACT_ABORT ON", "BEGIN TRY", "BEGIN CATCH", "THROW;"}) {
            assertTrue(migration.contains(token), token);
        }

        assertEquals(5, migration.split("EXEC sys.sp_executesql N'", -1).length - 1);
        assertTrue(migration.contains("ALTER TABLE dbo.Review ADD homepage_consent bit NOT NULL CONSTRAINT DF_Review_HomepageConsent DEFAULT 0 WITH VALUES"));
        for (String ddl : new String[] {
                "ALTER TABLE dbo.Product WITH CHECK ADD CONSTRAINT CK_Product_SpiceLevel CHECK (spice_level BETWEEN 0 AND 3)",
                "ALTER TABLE dbo.ProductCombo WITH CHECK ADD CONSTRAINT CK_ProductCombo_HomepageOccasion CHECK (homepage_occasion IS NULL OR homepage_occasion IN (''QUICK_BREAK'', ''OFFICE_LUNCH'', ''STUDENT'', ''GROUP''))",
                "ALTER TABLE dbo.Review WITH CHECK ADD CONSTRAINT CK_Review_FeaturedConsent CHECK (is_featured = 0 OR homepage_consent = 1)",
                "CREATE INDEX IX_ProductCombo_HomepageOccasion ON dbo.ProductCombo(homepage_occasion, homepage_sort_order)",
                "CREATE INDEX IX_Review_FeaturedCreatedAt ON dbo.Review(is_featured, created_at DESC)"}) {
            assertTrue(migration.contains("EXEC sys.sp_executesql N'" + ddl), ddl);
        }
        int finalDynamicDdl = migration.lastIndexOf("EXEC sys.sp_executesql N'");
        assertTrue(migration.indexOf("Pre-existing homepage merchandising column definition invalid.") > finalDynamicDdl);
        assertTrue(migration.indexOf("INSERT dbo.SchemaMigrationHistory") > finalDynamicDdl);
    }

    @Test
    void validatorAndFreshSchemasShareHomepageMetadata() throws Exception {
        String validator = Files.readString(ROOT.resolve("database/migrations/048_validate.sql"));
        String init = Files.readString(ROOT.resolve("database/init.sql"));
        String snapshot = Files.readString(ROOT.resolve("database/DB_FastGuy.sql"));

        for (String token : new String[] {"is_new", "spice_level", "homepage_occasion", "homepage_sort_order", "is_featured", "homepage_consent", "DF_Review_HomepageConsent"}) {
            assertTrue(validator.contains(token), "validator " + token);
            assertTrue(init.contains(token), "init " + token);
            assertTrue(snapshot.contains(token), "snapshot " + token);
        }
        assertTrue(validator.contains("BETWEEN 0 AND 3"));
        assertTrue(validator.contains("QUICK_BREAK"));
        assertTrue(validator.contains("OFFICE_LUNCH"));
        assertTrue(validator.contains("STUDENT"));
        assertTrue(validator.contains("GROUP"));
        assertTrue(validator.contains("CK_Review_FeaturedConsent"));
        assertTrue(validator.contains("is_featured=0orhomepage_consent=1"));
        for (String source : new String[] {init, snapshot}) {
            assertTrue(source.contains("CK_Review_FeaturedConsent"));
            assertTrue(source.contains("is_featured = 0 OR homepage_consent = 1"));
        }
        assertTrue(validator.contains("is_disabled = 1 OR is_not_trusted = 1"));
        assertTrue(validator.contains("WHERE is_featured = 1 AND homepage_consent = 0"));
        for (String setting : new String[] {"SET ANSI_NULLS ON", "SET QUOTED_IDENTIFIER ON", "SET ANSI_PADDING ON",
                "SET ANSI_WARNINGS ON", "SET ARITHABORT ON", "SET CONCAT_NULL_YIELDS_NULL ON", "SET NUMERIC_ROUNDABORT OFF"}) {
            assertTrue(init.contains(setting), "init " + setting);
            assertTrue(snapshot.contains(setting), "snapshot " + setting);
        }
    }
}
