package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DeliveryFailurePolicyTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 12, 10, 0);

    @Test
    void acceptsOnlyKnownReasonWithTrimmedNonblankNoteUpTo500Characters() {
        assertEquals(Set.of("CUSTOMER_UNREACHABLE", "INVALID_ADDRESS", "CUSTOMER_RESCHEDULED",
                "CUSTOMER_REJECTED", "SHIPPER_INCIDENT", "PRODUCT_INCIDENT"), DeliveryFailurePolicy.REASON_CODES);
        assertTrue(DeliveryFailurePolicy.isValidFailure("CUSTOMER_UNREACHABLE", "  Called twice  "));
        assertTrue(DeliveryFailurePolicy.isValidFailure("PRODUCT_INCIDENT", "x".repeat(500)));
        assertFalse(DeliveryFailurePolicy.isValidFailure("OTHER", "Called twice"));
        assertFalse(DeliveryFailurePolicy.isValidFailure("CUSTOMER_UNREACHABLE", "   "));
        assertFalse(DeliveryFailurePolicy.isValidFailure("CUSTOMER_UNREACHABLE", "x".repeat(501)));
    }

    @Test
    void acceptsAndTrimsRecoveryNotesUpTo500Characters() {
        assertEquals("retry", DeliveryFailurePolicy.normalizeNote("  retry  "));
        assertEquals("x".repeat(500), DeliveryFailurePolicy.normalizeNote("x".repeat(500)));
        assertEquals(null, DeliveryFailurePolicy.normalizeNote(null));
        assertEquals(null, DeliveryFailurePolicy.normalizeNote("   "));
        assertEquals(null, DeliveryFailurePolicy.normalizeNote("x".repeat(501)));
    }

    @Test
    void retryRequiresNonnegativeCountBelowPositiveLimit() {
        assertTrue(DeliveryFailurePolicy.canRetry(0, 2));
        assertTrue(DeliveryFailurePolicy.canRetry(1, 2));
        assertFalse(DeliveryFailurePolicy.canRetry(2, 2));
        assertFalse(DeliveryFailurePolicy.canRetry(-1, 2));
        assertFalse(DeliveryFailurePolicy.canRetry(0, 0));
    }

    @Test
    void immediateRetryRequiresNoSchedule() {
        assertTrue(DeliveryFailurePolicy.isValidSchedule("IMMEDIATE", null, NOW,
                LocalTime.of(8, 0), LocalTime.of(22, 0)));
        assertFalse(DeliveryFailurePolicy.isValidSchedule("IMMEDIATE", NOW.plusHours(1), NOW,
                LocalTime.of(8, 0), LocalTime.of(22, 0)));
    }

    @Test
    void scheduledRetryMustBeFutureWithinTwentyFourHoursAndStoreHours() {
        assertTrue(DeliveryFailurePolicy.isValidSchedule("SCHEDULED", NOW.plusHours(2), NOW,
                LocalTime.of(8, 0), LocalTime.of(22, 0)));
        assertFalse(DeliveryFailurePolicy.isValidSchedule("SCHEDULED", NOW, NOW,
                LocalTime.of(8, 0), LocalTime.of(22, 0)));
        assertFalse(DeliveryFailurePolicy.isValidSchedule("SCHEDULED", NOW.plusHours(24).plusNanos(1), NOW,
                LocalTime.of(8, 0), LocalTime.of(22, 0)));
        assertFalse(DeliveryFailurePolicy.isValidSchedule("SCHEDULED", NOW.withHour(23), NOW,
                LocalTime.of(8, 0), LocalTime.of(22, 0)));
        assertFalse(DeliveryFailurePolicy.isValidSchedule("UNKNOWN", null, NOW,
                LocalTime.of(8, 0), LocalTime.of(22, 0)));
    }

    @Test
    void scheduledRetryUsesOvernightAndEqualAllDayStoreHours() {
        assertTrue(DeliveryFailurePolicy.isValidSchedule("SCHEDULED", NOW.withHour(23), NOW,
                LocalTime.of(22, 0), LocalTime.of(6, 0)));
        assertTrue(DeliveryFailurePolicy.isValidSchedule("SCHEDULED", NOW.plusHours(2), NOW,
                LocalTime.MIDNIGHT, LocalTime.MIDNIGHT));
        assertFalse(DeliveryFailurePolicy.isValidSchedule("SCHEDULED", NOW.plusHours(2), NOW,
                LocalTime.of(22, 0), LocalTime.of(6, 0)));
    }

    @Test
    void invalidNullInputsAreRejected() {
        assertFalse(DeliveryFailurePolicy.isValidFailure(null, "note"));
        assertFalse(DeliveryFailurePolicy.isValidSchedule(null, null, NOW, LocalTime.MIN, LocalTime.MAX));
        assertFalse(DeliveryFailurePolicy.isValidSchedule("SCHEDULED", NOW.plusHours(1), null,
                LocalTime.MIN, LocalTime.MAX));
        assertFalse(DeliveryFailurePolicy.isValidSchedule("SCHEDULED", NOW.plusHours(1), NOW, null,
                LocalTime.MAX));
    }
}
