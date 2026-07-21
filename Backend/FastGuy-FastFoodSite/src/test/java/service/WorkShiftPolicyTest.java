package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import entity.WorkShift;

class WorkShiftPolicyTest {
    @Test
    void checkoutStartsAtShiftEnd() {
        LocalTime end = LocalTime.of(17, 0);
        assertFalse(WorkShiftService.canCheckOut(LocalTime.of(16, 59), end));
        assertTrue(WorkShiftService.canCheckOut(LocalTime.of(17, 0), end));
        assertTrue(WorkShiftService.canCheckOut(LocalTime.of(20, 0), end));
    }

    @Test
    void checkoutRemainsAllowedAfterShiftDate() {
        WorkShift shift = shift(LocalTime.of(9, 0), LocalTime.of(17, 0), "CHECKED_IN");
        shift.setShiftDate(LocalDate.of(2026, 7, 20));
        shift.setCheckInAt(LocalDateTime.of(2026, 7, 20, 9, 0));
        assertTrue(WorkShiftService.canCheckOut(shift, LocalDateTime.of(2026, 7, 21, 8, 0)));
    }

    @Test
    void checkoutRequiresCheckedInStatus() {
        WorkShift shift = shift(LocalTime.of(9, 0), LocalTime.of(17, 0), "SCHEDULED");
        shift.setCheckInAt(LocalDateTime.of(2026, 7, 21, 9, 0));
        assertFalse(WorkShiftService.canCheckOut(shift, LocalDateTime.of(2026, 7, 21, 18, 0)));
    }

    @Test
    void checkoutRejectsBeforeEndOnShiftDate() {
        WorkShift shift = shift(LocalTime.of(9, 0), LocalTime.of(17, 0), "CHECKED_IN");
        shift.setCheckInAt(LocalDateTime.of(2026, 7, 21, 9, 0));
        assertFalse(WorkShiftService.canCheckOut(shift, LocalDateTime.of(2026, 7, 21, 16, 59)));
        assertTrue(WorkShiftService.canCheckOut(shift, LocalDateTime.of(2026, 7, 21, 17, 0)));
    }

    @Test
    void checkedInShiftHasPriority() {
        WorkShift upcoming = shift(LocalTime.of(14, 0), LocalTime.of(16, 0), "SCHEDULED");
        WorkShift checkedIn = shift(LocalTime.of(8, 0), LocalTime.of(10, 0), "CHECKED_IN");
        WorkShiftService.CurrentShift current = WorkShiftService.current(List.of(upcoming, checkedIn), LocalTime.NOON);
        assertEquals("CHECKED_IN", current.state());
        assertSame(checkedIn, current.shift());
    }

    @Test
    void expiredMissedShiftDoesNotHideUpcomingShift() {
        WorkShift missed = shift(LocalTime.of(8, 0), LocalTime.of(9, 0), "SCHEDULED");
        WorkShift upcoming = shift(LocalTime.of(14, 0), LocalTime.of(16, 0), "SCHEDULED");
        WorkShiftService.CurrentShift current = WorkShiftService.current(List.of(missed, upcoming), LocalTime.NOON);
        assertEquals("UPCOMING", current.state());
        assertSame(upcoming, current.shift());
    }

    @Test
    void checkInWindowIncludesBothBoundaries() {
        WorkShift shift = shift(LocalTime.of(9, 0), LocalTime.of(17, 0), "SCHEDULED");
        assertEquals("UPCOMING", WorkShiftService.current(List.of(shift), LocalTime.of(8, 44)).state());
        assertEquals("CHECK_IN_ALLOWED", WorkShiftService.current(List.of(shift), LocalTime.of(8, 45)).state());
        assertEquals("CHECK_IN_ALLOWED", WorkShiftService.current(List.of(shift), LocalTime.of(17, 15)).state());
        assertEquals("NONE", WorkShiftService.current(List.of(shift), LocalTime.of(17, 16)).state());
    }

    @Test
    void allCompletedShiftsReturnCheckedOut() {
        WorkShift first = shift(LocalTime.of(8, 0), LocalTime.of(10, 0), "CHECKED_OUT");
        WorkShift second = shift(LocalTime.of(12, 0), LocalTime.of(14, 0), "CHECKED_OUT");
        first.setCheckOutAt(LocalDateTime.of(2026, 7, 21, 10, 0));
        second.setCheckOutAt(LocalDateTime.of(2026, 7, 21, 14, 0));
        assertEquals("CHECKED_OUT", WorkShiftService.current(List.of(first, second), LocalTime.of(15, 0)).state());
    }

    @Test
    void noShiftsReturnNone() {
        WorkShiftService.CurrentShift current = WorkShiftService.current(List.of(), LocalTime.NOON);
        assertEquals("NONE", current.state());
        assertEquals(null, current.shift());
    }

    private WorkShift shift(LocalTime start, LocalTime end, String status) {
        WorkShift shift = new WorkShift();
        shift.setShiftDate(LocalDate.of(2026, 7, 21));
        shift.setStartTime(start);
        shift.setEndTime(end);
        shift.setStatus(status);
        return shift;
    }
}
