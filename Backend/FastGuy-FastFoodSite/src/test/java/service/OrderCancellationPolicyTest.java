package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrderCancellationPolicyTest {
    @Test
    void paymentTimeoutOnlyCancelsUnpaidOrder() {
        assertTrue(OrderService.matchesExpectedPaymentStatus("UNPAID", "UNPAID"));
        assertFalse(OrderService.matchesExpectedPaymentStatus("PAID", "UNPAID"));
        assertTrue(OrderService.matchesExpectedPaymentStatus("PAID", null));
    }

    @Test
    void cancellationReleasesReservedInventoryAndWastesConsumedInventory() {
        assertTrue("RELEASE".equals(InventoryReservationService.cancellationTransactionType("RESERVED")));
        assertTrue("WASTE".equals(InventoryReservationService.cancellationTransactionType("CONSUMED")));
        assertTrue(InventoryReservationService.cancellationTransactionType("WASTED") == null);
        assertTrue(InventoryReservationService.cancellationTransactionType("RELEASED") == null);
    }
}
