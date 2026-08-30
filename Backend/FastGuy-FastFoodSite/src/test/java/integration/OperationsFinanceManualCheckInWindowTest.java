package integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class OperationsFinanceManualCheckInWindowTest {
    @Test
    void manualCheckInWindowAcceptsDayBoundariesWithoutGraceWrapping() {
        for (LocalTime current : List.of(LocalTime.MIDNIGHT, LocalTime.NOON, LocalTime.MAX)) {
            LocalTime[] window = OperationsFinanceIT.manualCheckInWindow(current);
            LocalTime start = window[0];
            LocalTime end = window[1];
            LocalTime graceStart = start.minusMinutes(15);
            LocalTime graceEnd = end.plusMinutes(15);

            assertTrue(start.isBefore(end));
            assertFalse(current.isBefore(graceStart));
            assertFalse(current.isAfter(graceEnd));
            assertFalse(graceStart.isAfter(start));
            assertFalse(graceEnd.isBefore(end));
        }
    }
}
