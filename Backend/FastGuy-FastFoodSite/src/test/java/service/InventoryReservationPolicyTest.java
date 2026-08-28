package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InventoryReservationPolicyTest {
    @Test
    void reservationOnlyMovesForward() {
        assertTrue(InventoryReservationService.canTransition("RESERVED", "CONSUMED"));
        assertTrue(InventoryReservationService.canTransition("RESERVED", "RELEASED"));
        assertTrue(InventoryReservationService.canTransition("CONSUMED", "WASTED"));
        assertFalse(InventoryReservationService.canTransition("CONSUMED", "RELEASED"));
        assertFalse(InventoryReservationService.canTransition("WASTED", "WASTED"));
        assertFalse(InventoryReservationService.canTransition("RELEASED", "RELEASED"));
    }
}
