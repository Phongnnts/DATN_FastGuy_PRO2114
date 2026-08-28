package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class OrderSchedulerClosingPolicyTest {
    @Test void schedulerRunsFivePhasesInDeterministicOrderAndIsolatesEveryFailure() {
        List<String> phases = new ArrayList<>();
        OrderScheduler.runTick(failing(phases, "check-in"), failing(phases, "rollover"),
                failing(phases, "cutoff"), failing(phases, "check-out"), failing(phases, "expiry"));
        assertEquals(List.of("check-in", "rollover", "cutoff", "check-out", "expiry"), phases);
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
