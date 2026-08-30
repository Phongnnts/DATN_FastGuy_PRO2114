package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import entity.User;
import entity.WorkShift;

class WorkShiftSchedulingPolicyTest {
    @Test
    void staffTemplatesAreFixed() {
        assertEquals(Map.of(
                "MORNING", List.of(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                "AFTERNOON", List.of(LocalTime.of(12, 0), LocalTime.of(16, 0)),
                "EVENING", List.of(LocalTime.of(16, 0), LocalTime.of(21, 0))), WorkShiftService.STAFF_TEMPLATES);
    }

    @Test
    void weekStartMustBeMondayAndNotAfterCurrentWeek() {
        LocalDate currentMonday = LocalDate.of(2026, 8, 24);
        assertEquals(currentMonday, WorkShiftService.validateWeekStart("2026-08-24", currentMonday));
        assertThrows(IllegalArgumentException.class, () -> WorkShiftService.validateWeekStart("2026-08-25", currentMonday));
        assertThrows(IllegalArgumentException.class, () -> WorkShiftService.validateWeekStart("2026-08-31", currentMonday));
    }

    @Test
    void monitoringStatesAndSeverityMatchContract() {
        WorkShift scheduled = shift("MORNING", "SCHEDULED");
        assertEquals(List.of("SCHEDULED", "INFO"), WorkShiftService.monitoring(scheduled, LocalDateTime.of(2026, 8, 24, 7, 0), false, 0));
        assertEquals(List.of("CHECK_IN_WINDOW", "INFO"), WorkShiftService.monitoring(scheduled, LocalDateTime.of(2026, 8, 24, 7, 50), false, 0));
        assertEquals(List.of("LATE", "WARNING"), WorkShiftService.monitoring(scheduled, LocalDateTime.of(2026, 8, 24, 8, 6), false, 0));
        scheduled.setStatus("CHECKED_IN");
        scheduled.setCheckInAt(LocalDateTime.of(2026, 8, 24, 8, 5));
        scheduled.setCheckInSource("AUTO");
        assertEquals(List.of("ACTIVE_AUTO", "WARNING"), WorkShiftService.monitoring(scheduled, LocalDateTime.of(2026, 8, 24, 10, 0), false, 0));
        assertEquals(List.of("ROLLOVER_BLOCKED", "CRITICAL"), WorkShiftService.monitoring(scheduled, LocalDateTime.of(2026, 8, 24, 12, 6), true, 2));
    }

    @Test
    void historicalAutomaticCompletionRemainsVisibleInMonitoring() {
        WorkShift completed = shift("MORNING", "CHECKED_OUT");
        completed.setCheckInAt(LocalDateTime.of(2026, 8, 24, 8, 5));
        completed.setCheckOutAt(LocalDateTime.of(2026, 8, 24, 12, 5));
        completed.setCheckInSource("AUTO");
        completed.setCheckOutSource("AUTO");
        assertEquals(List.of("COMPLETED_AUTO", "INFO"), WorkShiftService.monitoring(completed, LocalDateTime.of(2026, 8, 25, 8, 0), false, 0));
    }

    private WorkShift shift(String code, String status) {
        WorkShift shift = new WorkShift();
        User user = new User();
        user.setUserId(2);
        user.setFullName("Staff");
        shift.setUser(user);
        shift.setShiftDate(LocalDate.of(2026, 8, 24));
        shift.setShiftCode(code);
        shift.setStartTime(WorkShiftService.STAFF_TEMPLATES.get(code).get(0));
        shift.setEndTime(WorkShiftService.STAFF_TEMPLATES.get(code).get(1));
        shift.setStatus(status);
        return shift;
    }
}
