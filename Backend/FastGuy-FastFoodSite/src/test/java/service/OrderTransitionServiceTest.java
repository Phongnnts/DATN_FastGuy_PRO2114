package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrderTransitionServiceTest {
    @Test
    void paidCodCanBeDelivered() {
        assertTrue(OrderTransitionService.canDeliver("COD", "PAID"));
    }

    @Test
    void unpaidOrderCannotBeDelivered() {
        assertFalse(OrderTransitionService.canDeliver("COD", "UNPAID"));
        assertFalse(OrderTransitionService.canDeliver("BANK_TRANSFER", "UNPAID"));
    }
}
