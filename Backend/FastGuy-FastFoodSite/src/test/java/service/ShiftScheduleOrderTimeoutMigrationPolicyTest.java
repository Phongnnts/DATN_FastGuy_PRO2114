package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ShiftScheduleOrderTimeoutMigrationPolicyTest {
    @Test
    void migrationValidatorCanonicalSchemasAndJpaExposeSchedulingTimeoutPolicy() throws Exception {
        String migration = Files.readString(Path.of("../../database/migrations/059_shift_schedule_order_timeout.sql"));
        String validator = Files.readString(Path.of("../../database/migrations/059_validate.sql"));
        for (String required : new String[] { "shift_code", "check_in_source", "check_out_source", "staff_role_snapshot", "UX_WorkShift_Staff_Date_Code", "status_entered_at", "IX_Orders_Status_StatusEnteredAt", "IX_Orders_StaffShift_Status", "IX_Orders_PaymentStatus_OrderStatus_StatusEnteredAt" }) {
            assertTrue(migration.contains(required), required + " migration");
            assertTrue(validator.contains(required), required + " validator");
        }
        assertTrue(migration.contains("sp_getapplock"));
        assertTrue(migration.contains("SET XACT_ABORT ON"));
        assertTrue(migration.contains("shift_code WHEN ''MORNING'' THEN ''08:00'' WHEN ''AFTERNOON'' THEN ''12:00'' ELSE ''16:00''"));
        assertTrue(migration.contains("shift_code WHEN ''MORNING'' THEN ''12:00'' WHEN ''AFTERNOON'' THEN ''16:00'' ELSE ''21:00''"));
        assertTrue(migration.contains("CK_WorkShift_Time CHECK (start_time<end_time)"));
        assertFalse(migration.contains("'06:00'"));
        assertFalse(migration.contains("'14:00'"));
        assertFalse(migration.contains("'22:00'"));
        assertTrue(migration.contains("MANUAL") && migration.contains("AUTO"));
        assertTrue(migration.contains("updated_at") && migration.contains("created_at"));
        assertTrue(migration.contains("EXEC sys.sp_executesql N'UPDATE ws SET shift_code="));
        assertTrue(migration.contains("EXEC sys.sp_executesql N'IF EXISTS(SELECT 1 FROM dbo.WorkShift WHERE shift_code IS NULL)"));
        assertTrue(migration.contains("EXEC sys.sp_executesql N'ALTER TABLE dbo.WorkShift ALTER COLUMN shift_code"));
        assertTrue(migration.contains("EXEC sys.sp_executesql N'CREATE UNIQUE INDEX UX_WorkShift_Staff_Date_Code"));
        assertFalse(migration.contains("TR_WorkShift_RoleSnapshot"));
        String legacyGuardDrop = "IF OBJECT_ID(N'dbo.TR_WorkShift_RoleGuard',N'TR') IS NOT NULL DROP TRIGGER dbo.TR_WorkShift_RoleGuard;";
        int historyStart = migration.indexOf("IF EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory");
        int elseStart = migration.indexOf("ELSE", historyStart);
        assertTrue(migration.substring(historyStart, elseStart).contains(legacyGuardDrop));
        assertTrue(migration.substring(elseStart).contains(legacyGuardDrop));
        assertTrue(validator.contains("name IN (N'TR_WorkShift_RoleGuard',N'TR_WorkShift_RoleSnapshot')"));
        String migration040 = Files.readString(Path.of("../../database/migrations/040_production_hardening.sql"));
        assertTrue(migration040.contains("CREATE OR ALTER TRIGGER dbo.TR_WorkShift_RoleGuard"));
        assertTrue(migration.contains("EXEC sys.sp_executesql N'UPDATE dbo.Orders SET status_entered_at="));
        assertTrue(migration.contains("COALESCE(updated_at,created_at,SYSDATETIME())"));
        assertTrue(migration.contains("DEFAULT SYSDATETIME() FOR status_entered_at"));
        assertFalse(migration.contains("status_entered_at=COALESCE(updated_at,created_at,SYSUTCDATETIME())"));
        assertTrue(migration.contains("LEGACY_STAFF"));
        assertTrue(migration.contains("WHEN u.role_name=''STAFF'' AND ws.status=''SCHEDULED'' THEN ''STAFF''"));
        assertTrue(migration.contains("EXEC sys.sp_executesql N'CREATE INDEX IX_Orders_Status_StatusEnteredAt"));
        for (String file : new String[] { "init.sql", "DB_FastGuy.sql" }) {
            String schema = Files.readString(Path.of("../../database/" + file));
            assertTrue(schema.contains("shift_code varchar(10) NOT NULL"), file);
            assertTrue(schema.contains("status_entered_at datetime2(0) NOT NULL"), file);
            assertTrue(schema.contains("UX_WorkShift_User_Date_Code"), file);
            assertFalse(schema.contains("UX_WorkShift_Staff_Date_Code"), file);
            assertFalse(schema.contains("TR_WorkShift_RoleSnapshot"), file);
            assertTrue(schema.contains("IX_Orders_Status_StatusEnteredAt"), file);
            assertTrue(schema.contains("IX_Orders_PaymentStatus_OrderStatus_StatusEnteredAt"), file);
            assertTrue(schema.contains("shift_code='MORNING' AND start_time='08:00' AND end_time='12:00'"), file);
            assertTrue(schema.contains("shift_code='AFTERNOON' AND start_time='12:00' AND end_time='16:00'"), file);
            assertTrue(schema.contains("shift_code='EVENING' AND start_time='16:00' AND end_time='21:00'"), file);
            assertTrue(schema.contains("CK_WorkShift_Time CHECK (start_time < end_time)"), file);
            assertFalse(schema.contains("shift_code='EVENING' AND start_time='22:00' AND end_time='06:00'"), file);
        }
        assertTrue(validator.contains("shift_code='MORNING' AND start_time='08:00' AND end_time='12:00'"));
        assertTrue(validator.contains("shift_code='AFTERNOON' AND start_time='12:00' AND end_time='16:00'"));
        assertTrue(validator.contains("shift_code='EVENING' AND start_time='16:00' AND end_time='21:00'"));
        assertTrue(validator.contains("LEGACY_STAFF"));
        assertTrue(validator.contains("staff_role_snapshot='STAFF'"));
        String workShift = Files.readString(Path.of("src/main/java/entity/WorkShift.java"));
        String orders = Files.readString(Path.of("src/main/java/entity/Orders.java"));
        assertTrue(workShift.contains("@Column(name = \"shift_code\")"));
        assertTrue(workShift.contains("@Column(name = \"check_in_source\")"));
        assertTrue(workShift.contains("@Column(name = \"check_out_source\")"));
        assertTrue(workShift.contains("@Column(name = \"staff_role_snapshot\")"));
        assertTrue(orders.contains("@Column(name = \"status_entered_at\")"));
        String service = Files.readString(Path.of("src/main/java/service/WorkShiftService.java"));
        assertTrue(service.contains("shift.setStaffRoleSnapshot(roleSnapshot(user));"));
        assertTrue(service.contains("!\"STAFF\".equals(user.getRole()) && !\"SHIPPER\".equals(user.getRole())"));
        assertTrue(service.contains("shift.setStaffRoleSnapshot(roleSnapshot(user))"));
        assertFalse(service.contains("shift.setStaffRoleSnapshot(\"STAFF\")"));
        assertTrue(service.contains("return \"STAFF\".equals(user.getRole()) ? \"STAFF\" : \"NON_STAFF\";"));
    }
}
