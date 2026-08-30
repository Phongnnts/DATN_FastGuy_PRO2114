package service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class StaffPayRateMigrationPolicyTest {
    @Test
    void migrationAndCanonicalSchemasDefineEffectiveRatesAndImmutableSnapshots() throws Exception {
        String migration = Files.readString(Path.of("../../database/migrations/062_staff_pay_rate_snapshot.sql"));
        String validator = Files.readString(Path.of("../../database/migrations/062_validate.sql"));
        for (String token : new String[]{"StaffPayRate", "effective_from", "regular_hourly_rate", "overtime_hourly_rate", "UQ_StaffPayRate_User_EffectiveFrom", "pay_snapshot_status", "regular_hourly_rate_snapshot", "overtime_hourly_rate_snapshot", "regular_pay_amount", "overtime_pay_amount", "total_pay_amount", "LEGACY_UNAVAILABLE", "CALCULATED", "062_staff_pay_rate_snapshot"}) {
            assertTrue(migration.contains(token), token);
            assertTrue(validator.contains(token), token);
        }
        for (String file : new String[]{"../../database/init.sql", "../../database/DB_FastGuy.sql"}) {
            String schema = Files.readString(Path.of(file));
            assertTrue(schema.contains("CREATE TABLE dbo.StaffPayRate"), file);
            assertTrue(schema.contains("pay_snapshot_status varchar(30) NULL"), file);
        }
    }
}
