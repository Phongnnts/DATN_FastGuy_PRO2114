package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ShiftRolloverServiceTest {
    @Test
    void onlySameDateMorningAndAfternoonHaveNextShift() {
        assertEquals("AFTERNOON", ShiftRolloverService.nextShiftCode("MORNING"));
        assertEquals("EVENING", ShiftRolloverService.nextShiftCode("AFTERNOON"));
        assertEquals(null, ShiftRolloverService.nextShiftCode("EVENING"));
        assertTrue(ShiftRolloverService.sameDate(LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 24)));
        assertFalse(ShiftRolloverService.sameDate(LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 25)));
    }

    @Test
    void rolloverStatusesAreClosedAndExact() {
        assertEquals(Set.of("CONFIRMED", "PREPARING", "READY", "DELIVERY_FAILED"), ShiftRolloverService.ROLLOVER_STATUSES);
    }

    @Test
    void rolloverAcceptsExactlyOneScheduledOrCheckedInTargetAndPrioritizesActive() throws IOException {
        String source = Files.readString(Path.of("src/main/java/service/ShiftRolloverService.java"));
        assertTrue(source.contains("ws.status IN ('SCHEDULED','CHECKED_IN')"));
        assertTrue(source.contains("ORDER BY CASE WHEN ws.status = 'CHECKED_IN' THEN 0 ELSE 1 END"));
        assertTrue(source.contains("if (next.size() != 1)"));
        assertFalse(source.contains("setCheckInAt"));
        assertFalse(source.contains("setCheckInSource"));
    }

    @Test
    void ownershipQueryMakesSecondRunIdempotent() throws IOException {
        String source = Files.readString(Path.of("src/main/java/service/ShiftRolloverService.java"));
        assertTrue(source.contains("lockActiveOwnership(em, shiftId)"));
        assertTrue(source.indexOf("lockActiveOwnership(em, shiftId)") < source.indexOf("new OrderStatusHistory"));
    }

    @Test
    void missingTargetReturnsBeforeOwnershipMutation() throws IOException {
        String source = Files.readString(Path.of("src/main/java/service/ShiftRolloverService.java"));
        assertTrue(source.indexOf("if (next.size() != 1)") < source.indexOf("lockActiveOwnership(em, shiftId)"));
    }
}
