package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import service.WorkShiftService;
import utils.DatabaseUtil;

class WorkShiftAttendanceIT {
    private static final String DATABASE = "FastGuyDB_Attendance061_Test";
    private final String token = "ATT061-" + Long.toUnsignedString(System.nanoTime());
    private final List<Integer> userIds = new ArrayList<>();
    private final List<Integer> shiftIds = new ArrayList<>();

    @Test
    void disposableAttendanceQueryApprovalAndOptimisticConflictUseRealTransactions() throws Throwable {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("FASTGUY_DISPOSABLE_DB")),
                "Set FASTGUY_DISPOSABLE_DB=true only for the approved disposable database");
        EntityManager em = DatabaseUtil.getEntityManager();
        Throwable failure = null;
        try {
            verifyTarget(em);
            int staffId = insertUser(em, "STAFF", "Attendance Staff");
            int adminId = insertUser(em, "ADMIN", "Attendance Admin");
            LocalDate date = availableDate(em);
            LocalDateTime checkIn = LocalDateTime.of(date, LocalTime.of(8, 15));
            LocalDateTime checkOut = LocalDateTime.of(date, LocalTime.of(12, 30));
            int shiftId = insertShift(em, staffId, date, checkIn, checkOut);
            WorkShiftService service = new WorkShiftService();

            Map<String, Object> attendance = service.attendance(YearMonth.from(date).toString(), staffId, "PENDING").stream()
                    .filter(row -> ((Number) row.get("shiftId")).intValue() == shiftId)
                    .findFirst().orElseThrow();
            assertEquals(255, attendance.get("actualMinutes"));
            assertEquals(225, attendance.get("overlapEligibleMinutes"));
            assertEquals(30, attendance.get("potentialOvertimeMinutes"));
            String expectedUpdatedAt = attendance.get("updatedAt").toString();

            Map<String, Object> excessive = Map.of("expectedUpdatedAt", expectedUpdatedAt,
                    "approvedMinutes", 226, "approvedOvertimeMinutes", 30);
            assertThrows(IllegalArgumentException.class, () -> service.approveAttendance(shiftId, adminId, excessive));
            Map<String, Object> stale = Map.of("expectedUpdatedAt", LocalDateTime.parse(expectedUpdatedAt).minusSeconds(1).toString(),
                    "approvedMinutes", 225, "approvedOvertimeMinutes", 30);
            assertThrows(WorkShiftService.StaleAttendanceConflict.class,
                    () -> service.approveAttendance(shiftId, adminId, stale));

            Map<String, Object> approved = service.approveAttendance(shiftId, adminId, Map.of(
                    "expectedUpdatedAt", expectedUpdatedAt, "approvedMinutes", 225,
                    "approvedOvertimeMinutes", 30, "attendanceNote", "Disposable attendance IT"));
            assertEquals("APPROVED", approved.get("attendanceStatus"));
            assertEquals(225, approved.get("approvedMinutes"));
            assertEquals(30, approved.get("approvedOvertimeMinutes"));
            assertTimestamps(em, shiftId, checkIn, checkOut);
            assertTrue(service.attendance(YearMonth.from(date).toString(), staffId, "APPROVED").stream()
                    .anyMatch(row -> ((Number) row.get("shiftId")).intValue() == shiftId));
        } catch (Throwable t) {
            failure = t;
            throw t;
        } finally {
            cleanupPreserving(em, failure);
            DatabaseUtil.close();
        }
    }

    private void verifyTarget(EntityManager em) {
        Object[] row = (Object[]) em.createNativeQuery(
                "SELECT @@SERVERNAME,DB_NAME(),DATABASEPROPERTYEX(DB_NAME(),'Status'),CAST(compatibility_level AS int) FROM sys.databases WHERE name=DB_NAME()")
                .getSingleResult();
        assertEquals("DuckJo", row[0]);
        assertEquals(DATABASE, row[1]);
        assertEquals("ONLINE", row[2]);
        assertEquals(160, ((Number) row[3]).intValue());
        assertEquals(1L, ((Number) em.createNativeQuery(
                "SELECT COUNT_BIG(*) FROM dbo.SchemaMigrationHistory WHERE migration_id='061_work_shift_attendance_approval'")
                .getSingleResult()).longValue(), "Migration 061 must be applied before fixture writes");
    }

    private int insertUser(EntityManager em, String role, String name) {
        begin(em);
        int id = ((Number) em.createNativeQuery(
                "INSERT INTO dbo.Users(role_name,email,phone,password_hash,full_name,status,favorite_ids_json,created_at,updated_at) OUTPUT INSERTED.user_id VALUES (:role,:email,:phone,'test',:name,'ACTIVE',N'[]',SYSDATETIME(),SYSDATETIME())")
                .setParameter("role", role).setParameter("email", token + userIds.size() + "@test.local")
                .setParameter("phone", "6" + String.format("%09d", Math.floorMod(token.hashCode() + userIds.size(), 1_000_000_000)))
                .setParameter("name", token + " " + name).getSingleResult()).intValue();
        em.getTransaction().commit();
        userIds.add(id);
        return id;
    }

    private LocalDate availableDate(EntityManager em) {
        LocalDate date = LocalDate.of(2099, 1, 1);
        while (((Number) em.createNativeQuery(
                "SELECT COUNT_BIG(*) FROM dbo.WorkShift WHERE shift_date=:date AND shift_code='MORNING' AND staff_role_snapshot='STAFF'")
                .setParameter("date", date).getSingleResult()).longValue() != 0) date = date.plusDays(1);
        return date;
    }

    private int insertShift(EntityManager em, int staffId, LocalDate date, LocalDateTime checkIn, LocalDateTime checkOut) {
        begin(em);
        int id = ((Number) em.createNativeQuery(
                "INSERT INTO dbo.WorkShift(user_id,shift_date,start_time,end_time,shift_code,check_in_source,check_out_source,staff_role_snapshot,check_in_at,check_out_at,status,attendance_status,created_at,updated_at) OUTPUT INSERTED.shift_id VALUES (:user,:date,'08:00','12:00','MORNING','MANUAL','MANUAL','STAFF',:checkIn,:checkOut,'CHECKED_OUT','PENDING',SYSDATETIME(),SYSDATETIME())")
                .setParameter("user", staffId).setParameter("date", date).setParameter("checkIn", checkIn)
                .setParameter("checkOut", checkOut).getSingleResult()).intValue();
        em.getTransaction().commit();
        shiftIds.add(id);
        return id;
    }

    private void assertTimestamps(EntityManager em, int shiftId, LocalDateTime checkIn, LocalDateTime checkOut) {
        em.clear();
        Object[] row = (Object[]) em.createNativeQuery("SELECT check_in_at,check_out_at FROM dbo.WorkShift WHERE shift_id=:id")
                .setParameter("id", shiftId).getSingleResult();
        assertEquals(checkIn, row[0]);
        assertEquals(checkOut, row[1]);
    }

    private void cleanupPreserving(EntityManager em, Throwable original) {
        RuntimeException cleanupFailure = null;
        try { cleanup(em); }
        catch (RuntimeException e) { cleanupFailure = e; if (original != null) original.addSuppressed(e); }
        try { em.close(); }
        catch (RuntimeException e) {
            if (original != null) original.addSuppressed(e);
            else if (cleanupFailure != null) cleanupFailure.addSuppressed(e);
            else throw e;
        }
        if (original == null && cleanupFailure != null) throw cleanupFailure;
    }

    private void cleanup(EntityManager em) {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
        begin(em);
        if (!shiftIds.isEmpty()) em.createNativeQuery("DELETE FROM dbo.WorkShift WHERE shift_id IN (:ids)").setParameter("ids", shiftIds).executeUpdate();
        if (!userIds.isEmpty()) em.createNativeQuery("DELETE FROM dbo.Users WHERE user_id IN (:ids)").setParameter("ids", userIds).executeUpdate();
        em.getTransaction().commit();
        assertEquals(0L, remaining(em), "Integration cleanup must remove every fixture row");
    }

    private long remaining(EntityManager em) {
        long count = 0;
        if (!shiftIds.isEmpty()) count += ((Number) em.createNativeQuery("SELECT COUNT_BIG(*) FROM dbo.WorkShift WHERE shift_id IN (:ids)").setParameter("ids", shiftIds).getSingleResult()).longValue();
        if (!userIds.isEmpty()) count += ((Number) em.createNativeQuery("SELECT COUNT_BIG(*) FROM dbo.Users WHERE user_id IN (:ids)").setParameter("ids", userIds).getSingleResult()).longValue();
        return count;
    }

    private void begin(EntityManager em) {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
        em.getTransaction().begin();
    }
}
