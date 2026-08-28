package service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class WorkShiftAttendanceMigrationPolicyTest {
    @Test
    void migrationValidatorCanonicalSchemasAndJpaExposeAttendanceApproval() throws Exception {
        String migration = Files.readString(Path.of("../../database/migrations/061_work_shift_attendance_approval.sql"));
        String validator = Files.readString(Path.of("../../database/migrations/061_validate.sql"));
        for (String required : new String[] { "attendance_status", "approved_minutes", "approved_overtime_minutes", "attendance_note", "approved_by", "approved_at", "CK_WorkShift_AttendanceStatus", "CK_WorkShift_ApprovedMinutes", "FK_WorkShift_ApprovedBy", "IX_WorkShift_AttendanceReview" }) {
            assertTrue(migration.contains(required), required + " migration");
            assertTrue(validator.contains(required), required + " validator");
        }
        assertTrue(migration.contains("sp_getapplock"));
        assertTrue(migration.contains("SET XACT_ABORT ON"));
        assertTrue(migration.contains("SET QUOTED_IDENTIFIER ON"));
        assertTrue(migration.contains("SET ANSI_NULLS ON"));
        assertTrue(migration.contains("SET ANSI_WARNINGS ON"));
        assertTrue(migration.contains("SET ARITHABORT ON"));
        assertTrue(migration.contains("SET CONCAT_NULL_YIELDS_NULL ON"));
        assertTrue(migration.contains("SET NUMERIC_ROUNDABORT OFF"));
        assertTrue(migration.contains("EXEC sys.sp_executesql N'ALTER TABLE dbo.WorkShift WITH CHECK ADD CONSTRAINT CK_WorkShift_AttendanceStatus"));
        assertTrue(migration.contains("EXEC sys.sp_executesql N'CREATE INDEX IX_WorkShift_AttendanceReview"));
        assertTrue(migration.contains("061_work_shift_attendance_approval"));
        int lock = migration.indexOf("sp_getapplock");
        int lockedHistoryRecheck = migration.indexOf("IF EXISTS(SELECT 1 FROM dbo.SchemaMigrationHistory", lock);
        int alter = migration.indexOf("ALTER TABLE dbo.WorkShift ADD");
        assertTrue(lock >= 0 && lockedHistoryRecheck > lock && alter > lockedHistoryRecheck, "history must be rechecked after lock before ALTER");
        assertTrue(migration.indexOf("061 history exists but columns are incomplete", lockedHistoryRecheck) > lockedHistoryRecheck);
        for (String file : new String[] { "init.sql", "DB_FastGuy.sql" }) {
            String schema = Files.readString(Path.of("../../database/" + file));
            assertTrue(schema.contains("attendance_status varchar(20) NULL"), file);
            assertTrue(schema.contains("approved_minutes int NULL"), file);
            assertTrue(schema.contains("approved_overtime_minutes int NULL"), file);
            assertTrue(schema.contains("attendance_note nvarchar(500) NULL"), file);
            assertTrue(schema.contains("approved_by int NULL"), file);
            assertTrue(schema.contains("approved_at datetime2(0) NULL"), file);
        }
        String entity = Files.readString(Path.of("src/main/java/entity/WorkShift.java"));
        for (String column : new String[] { "attendance_status", "approved_minutes", "approved_overtime_minutes", "attendance_note", "approved_by", "approved_at" }) assertTrue(entity.contains("@Column(name = \"" + column + "\")"), column);
        String runbook = Files.readString(Path.of("../../database/migrations/RUNBOOK.md"));
        assertTrue(runbook.contains("061_work_shift_attendance_approval.sql"));
        assertTrue(runbook.contains("061_validate.sql"));
    }
}
