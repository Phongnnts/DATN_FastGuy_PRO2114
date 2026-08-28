package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import entity.WorkShift;

class WorkShiftAttendancePolicyTest {
    @Test
    void calculatesActualOverlapLateEarlyLeaveAndPotentialOvertime() {
        WorkShift shift = shift(LocalDateTime.of(2026, 8, 1, 8, 15), LocalDateTime.of(2026, 8, 1, 12, 30));
        WorkShiftService.Attendance minutes = WorkShiftService.attendance(shift);
        assertEquals(255, minutes.actualMinutes());
        assertEquals(225, minutes.overlapEligibleMinutes());
        assertEquals(15, minutes.lateMinutes());
        assertEquals(0, minutes.earlyLeaveMinutes());
        assertEquals(30, minutes.potentialOvertimeMinutes());
    }

    @Test
    void approvalCannotExceedEligibleOrPotentialOvertimeMinutes() {
        WorkShift shift = shift(LocalDateTime.of(2026, 8, 1, 8, 15), LocalDateTime.of(2026, 8, 1, 12, 30));
        assertThrows(IllegalArgumentException.class, () -> WorkShiftService.validateApproval(shift, 226, 30));
        assertThrows(IllegalArgumentException.class, () -> WorkShiftService.validateApproval(shift, 225, 31));
        WorkShiftService.validateApproval(shift, 225, 30);
    }

    @Test
    void approvalRequiresPendingHistoricalStaffAttendance() {
        WorkShift shift = shift(LocalDateTime.of(2026, 8, 1, 8, 0), LocalDateTime.of(2026, 8, 1, 12, 0));
        shift.setStaffRoleSnapshot("STAFF");
        shift.setAttendanceStatus("PENDING");
        WorkShiftService.validateApprovalEligibility(shift);
        shift.setStaffRoleSnapshot("NON_STAFF");
        assertThrows(IllegalArgumentException.class, () -> WorkShiftService.validateApprovalEligibility(shift));
        shift.setStaffRoleSnapshot("STAFF");
        shift.setAttendanceStatus("APPROVED");
        assertThrows(IllegalArgumentException.class, () -> WorkShiftService.validateApprovalEligibility(shift));
    }

    @Test
    void approvedMinutesRequireExactJsonIntegersWithinIntRange() {
        assertEquals(12, WorkShiftService.exactInt(12, "approvedMinutes"));
        assertEquals(12, WorkShiftService.exactInt(new BigDecimal("12"), "approvedMinutes"));
        assertThrows(IllegalArgumentException.class, () -> WorkShiftService.exactInt(new BigDecimal("12.5"), "approvedMinutes"));
        assertThrows(IllegalArgumentException.class, () -> WorkShiftService.exactInt(new BigInteger("2147483648"), "approvedMinutes"));
        assertThrows(IllegalArgumentException.class, () -> WorkShiftService.exactInt(Double.NaN, "approvedMinutes"));
    }

    @Test
    void attendanceQueryUsesHistoricalStaffSnapshotAndCompletedApprovalStates() throws Exception {
        String source = Files.readString(Path.of("src/main/java/service/WorkShiftService.java"));
        int start = source.indexOf("public List<Map<String, Object>> attendance");
        int end = source.indexOf("public Map<String, Object> approveAttendance", start);
        String query = source.substring(start, end);
        assertTrue(query.contains("ws.staffRoleSnapshot = 'STAFF'"));
        assertTrue(query.contains("ws.attendanceStatus IN ('PENDING','APPROVED')"));
        assertFalse(query.contains("ws.user.role = 'STAFF'"));
    }

    @Test
    void missingAttendanceShiftHasDedicatedNotFoundExceptionAndServlet404() throws Exception {
        assertEquals("Shift not found", new WorkShiftService.AttendanceNotFound().getMessage());
        String servlet = Files.readString(Path.of("src/main/java/servlet/AdminShiftServlet.java"));
        assertTrue(servlet.contains("catch (WorkShiftService.AttendanceNotFound e)"));
        assertTrue(servlet.contains("ApiResponse.error(resp, e.getMessage(), 404)"));
    }

    @Test
    void checkoutMarksAttendancePending() {
        WorkShift shift = shift(LocalDateTime.of(2026, 8, 1, 8, 0), null);
        WorkShiftService.completeAttendance(shift, LocalDateTime.of(2026, 8, 1, 12, 0), "AUTO");
        assertEquals("PENDING", shift.getAttendanceStatus());
        assertEquals("AUTO", shift.getCheckOutSource());
    }

    private WorkShift shift(LocalDateTime checkIn, LocalDateTime checkOut) {
        WorkShift shift = new WorkShift();
        shift.setShiftDate(LocalDate.of(2026, 8, 1));
        shift.setStartTime(LocalTime.of(8, 0));
        shift.setEndTime(LocalTime.of(12, 0));
        shift.setCheckInAt(checkIn);
        shift.setCheckOutAt(checkOut);
        shift.setStatus(checkOut == null ? "CHECKED_IN" : "CHECKED_OUT");
        return shift;
    }
}
