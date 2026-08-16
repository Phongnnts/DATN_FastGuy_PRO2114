package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class LowStockThresholdMigrationPolicyTest {
    @Test
    void migrationRunsAfterCodAndSeedsWithoutOverwriting() throws Exception {
        String migration = Files.readString(Path.of("../../database/migrations/047_low_stock_threshold.sql"));
        assertTrue(migration.contains("migration_id = '046_cod_shift_settlement'"));
        assertTrue(migration.contains("migration_id = '047_low_stock_threshold'"));
        assertTrue(migration.contains("IF NOT EXISTS (SELECT 1 FROM dbo.ShippingConfig WHERE config_key = 'low_stock_threshold')"));
        assertTrue(migration.contains("VALUES ('low_stock_threshold', '5')"));
        assertFalse(migration.contains("CREATE TABLE"));
        assertFalse(migration.contains("ALTER TABLE"));
    }

    @Test
    void validatorAndFreshBootstrapContainCanonicalDefault() throws Exception {
        String validator = Files.readString(Path.of("../../database/migrations/047_validate.sql"));
        String init = Files.readString(Path.of("../../database/init.sql"));
        String snapshot = Files.readString(Path.of("../../database/DB_FastGuy.sql"));
        assertTrue(validator.contains("047_low_stock_threshold"));
        assertTrue(validator.contains("COUNT(*) <> 1"));
        assertTrue(validator.contains("TRY_CONVERT(int, config_value)"));
        assertTrue(validator.contains("BETWEEN 1 AND 1000"));
        assertTrue(init.contains("('low_stock_threshold', '5')"));
        assertTrue(snapshot.contains("'low_stock_threshold','5'"));
    }
}
