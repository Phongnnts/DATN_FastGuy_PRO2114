package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import entity.User;
import entity.WorkShift;

class AutomaticAttendanceServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 28);

    @Test void checkInStartsExactlyFiveMinutesAfterScheduledStart() {
        WorkShift shift = shift("STAFF", "SCHEDULED", LocalTime.of(8, 0), LocalTime.of(12, 0));
        assertFalse(AutomaticAttendanceService.canAutoCheckIn(shift, DATE.atTime(8, 4, 59)));
        assertTrue(AutomaticAttendanceService.canAutoCheckIn(shift, DATE.atTime(8, 5)));
        WorkShiftService.autoCheckIn(shift, DATE.atTime(8, 5));
        assertFalse(AutomaticAttendanceService.canAutoCheckIn(shift, DATE.atTime(8, 6)));
    }

    @Test void checkOutStartsExactlyFiveMinutesAfterEndAndStaffRequiresNoOwnership() {
        WorkShift staff = shift("STAFF", "CHECKED_IN", LocalTime.of(8, 0), LocalTime.of(12, 0));
        staff.setCheckInAt(DATE.atTime(8, 5));
        assertFalse(AutomaticAttendanceService.canAutoCheckOut(staff, DATE.atTime(12, 4, 59), 0));
        assertFalse(AutomaticAttendanceService.canAutoCheckOut(staff, DATE.atTime(12, 5), 1));
        assertTrue(AutomaticAttendanceService.canAutoCheckOut(staff, DATE.atTime(12, 5), 0));
    }

    @Test void shipperUsesScheduledShiftWithoutOwnershipGuard() {
        WorkShift shipper = shift("SHIPPER", "CHECKED_IN", LocalTime.of(8, 0), LocalTime.of(12, 0));
        shipper.setCheckInAt(DATE.atTime(8, 5));
        assertTrue(AutomaticAttendanceService.canAutoCheckOut(shipper, DATE.atTime(12, 5), 99));
    }

    private static WorkShift shift(String role, String status, LocalTime start, LocalTime end) {
        User user = new User(); user.setRole(role); user.setStatus("ACTIVE");
        WorkShift shift = new WorkShift(); shift.setUser(user); shift.setShiftDate(DATE);
        shift.setStartTime(start); shift.setEndTime(end); shift.setStatus(status);
        return shift;
    }
}
