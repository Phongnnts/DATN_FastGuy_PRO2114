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

    @Test
    void cancellationCannotUseGenericTransition() {
        assertFalse(OrderTransitionService.canUseGenericTransition("CANCELLED"));
        assertTrue(OrderTransitionService.canUseGenericTransition("CONFIRMED"));
    }

    @Test
    void waitingStockConfirmationIsNotAWorkflowState() {
        OrderTransitionService service = new OrderTransitionService();
        assertFalse(service.canTransition("WAITING_STOCK_CONFIRM", "PENDING"));
        assertFalse(service.getAllowedActions("WAITING_STOCK_CONFIRM", "STAFF", "UNPAID").contains("PENDING"));
    }
}
