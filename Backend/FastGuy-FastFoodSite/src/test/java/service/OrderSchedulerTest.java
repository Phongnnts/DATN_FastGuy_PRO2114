package service;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class OrderSchedulerTest {
    @Test void legacyCodThreeHourPolicyIsAbsent() {
        assertFalse(Arrays.stream(OrderScheduler.class.getDeclaredMethods()).map(Method::getName).toList()
                .contains("isStaleCodPending"));
    }
}
