package service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class StaffShiftOwnershipMigrationPolicyTest {
    @Test
    void canonicalSchemasMatchOwnershipMigration() throws Exception {
        for (String file : new String[] { "init.sql", "DB_FastGuy.sql" }) {
            String source = Files.readString(Path.of("../../database/" + file));
            assertTrue(source.contains("staff_shift_id int NULL"), file);
            assertTrue(source.contains("FK_Orders_StaffShift"), file);
            assertTrue(source.contains("REFERENCES dbo.WorkShift(shift_id)"), file);
            assertTrue(source.contains("CREATE INDEX IX_Orders_StaffShift_Status ON dbo.Orders(staff_shift_id, order_status)"), file);
            assertTrue(source.contains("Orders.staff_shift_id must reference a STAFF shift."), file);
            int seedStart = source.indexOf("INSERT INTO dbo.Orders");
            int valuesStart = source.indexOf("VALUES", seedStart);
            if (seedStart >= 0) assertTrue(!source.substring(seedStart, valuesStart).contains("staff_shift_id"), file + " seed ownership must remain null by omission");
        }
    }

    @Test
    void alreadyAppliedBranchRejectsEveryIncompleteArtifact() throws Exception {
        String migration = Files.readString(Path.of("../../database/migrations/058_staff_shift_ownership.sql"));
        assertTrue(migration.contains("sys.columns") && migration.contains("sys.types"));
        assertTrue(migration.contains("t.name=N'int'") && migration.contains("c.is_nullable=1"));
        assertTrue(migration.contains("is_disabled=0"));
        assertTrue(migration.contains("is_not_trusted=0"));
        assertTrue(migration.contains("FK_Orders_StaffShift"));
        assertTrue(migration.contains("referenced_object_id=OBJECT_ID(N'dbo.WorkShift')"));
        assertTrue(migration.contains("pc.name=N'staff_shift_id'") && migration.contains("rc.name=N'shift_id'"));
        assertTrue(migration.contains("IX_Orders_StaffShift_Status"));
        assertTrue(migration.contains("key_ordinal=1") && migration.contains("key_ordinal=2"));
        assertTrue(migration.contains("TR_Orders_AssignmentRoleGuard"));
        assertTrue(migration.contains("Orders.staff_shift_id must reference a STAFF shift."));
    }

    @Test
    void ownershipFixtureSeedsSchemaValidDistinctShiftCodes() throws Exception {
        String fixture = Files.readString(Path.of("src/test/java/integration/StaffShiftOwnershipHandoverIT.java"));
        assertTrue(fixture.contains("insertShift(em, currentStaffId, now, \"MORNING\", \"NON_STAFF\")"));
        assertTrue(fixture.contains("insertShift(em, otherStaffId, now, \"AFTERNOON\", \"NON_STAFF\")"));
        assertTrue(fixture.contains("insertShift(em, shipperId, now, \"EVENING\", \"NON_STAFF\")"));
        assertTrue(fixture.contains("shift.setShiftCode(code)"));
        assertTrue(fixture.contains("shift.setStaffRoleSnapshot(staffRoleSnapshot)"));
        assertTrue(fixture.contains("Assumptions.assumeTrue(now.toLocalTime().isAfter(LocalTime.of(0, 1))"));
        assertTrue(fixture.contains("StaffShiftOwnershipHandoverIT requires business time after 00:01 so endTime is after startTime"));
        assertTrue(fixture.contains("shift.setStartTime(LocalTime.MIDNIGHT)"));
        assertTrue(fixture.contains("shift.setEndTime(now.toLocalTime().minusMinutes(1))"));
        assertTrue(fixture.contains("order.setShippingProvider(\"GHN\")"));
        assertTrue(fixture.indexOf("order.setShippingProvider(\"GHN\")") < fixture.indexOf("em.persist(order)"));
        assertTrue(!fixture.contains("now.minusHours(2).toLocalTime()"));
    }

    @Test
    void runbookIncludes058DisposableValidationIdempotencyAndRetainedRecoveryGate() throws Exception {
        String runbook = Files.readString(Path.of("../../database/migrations/RUNBOOK.md"));
        assertTrue(runbook.contains("058_staff_shift_ownership.sql"));
        assertTrue(runbook.contains("058_validate.sql"));
        assertTrue(runbook.contains("rerun migration 058"));
        assertTrue(runbook.contains("separate retained approval"));
        assertTrue(runbook.contains("restore the verified backup"));
    }
}
