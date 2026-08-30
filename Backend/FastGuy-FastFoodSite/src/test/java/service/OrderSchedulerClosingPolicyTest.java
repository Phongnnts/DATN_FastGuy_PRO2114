package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class OrderSchedulerClosingPolicyTest {
    @Test
    void schedulerRunsRolloverCutoffExpiryInOrderAndIsolatesEveryFailure() {
        List<String> phases = new ArrayList<>();

        OrderScheduler.runTick(
                failing(phases, "rollover"),
                failing(phases, "cutoff"),
                failing(phases, "expiry"));

        assertEquals(List.of("rollover", "cutoff", "expiry"), phases);
    }

    @Test
    void schedulerHasNoAutomaticAttendanceDependencyOrPhase() throws Exception {
        String source = Files.readString(Path.of("src/main/java/service/OrderScheduler.java"));

        assertFalse(source.contains("AutomaticAttendanceService"));
        assertFalse(source.contains("autoCheckIns"));
        assertFalse(source.contains("autoCheckOuts"));
    }

    @Test
    void automaticAttendanceServiceIsAbsent() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("service.AutomaticAttendanceService"));
    }

    @Test void legacyCancellationMethodsAreAbsent() {
        List<String> methods = Arrays.stream(OrderScheduler.class.getDeclaredMethods()).map(Method::getName).toList();
        assertFalse(methods.contains("cancelUnpaidOrders"));
        assertFalse(methods.contains("cancelReadyOrdersAfterClosing"));
        assertFalse(methods.contains("isStaleCodPending"));
    }

    @Test void cutoffBoundaryStartsAt2045() {
        assertFalse(OrderExpiryService.isAtCutoff(LocalTime.of(20, 44, 59)));
        assertEquals(true, OrderExpiryService.isAtCutoff(LocalTime.of(20, 45)));
    }

    private static Runnable failing(List<String> phases, String name) {
        return () -> { phases.add(name); throw new IllegalStateException(name); };
    }
}
