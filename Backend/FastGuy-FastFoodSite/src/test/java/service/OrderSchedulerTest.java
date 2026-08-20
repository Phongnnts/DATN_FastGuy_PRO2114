package service;

import entity.Orders;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderSchedulerTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 20, 12, 0);

    @Test
    void cancelsOnlyUnpaidCodPendingOrdersOlderThanThreeHours() {
        assertTrue(OrderScheduler.isStaleCodPending(order("PENDING", "COD", "UNPAID", NOW.minusHours(3).minusSeconds(1)), NOW));
        assertFalse(OrderScheduler.isStaleCodPending(order("PENDING", "COD", "UNPAID", NOW.minusHours(3)), NOW));
        assertFalse(OrderScheduler.isStaleCodPending(order("PENDING", "COD", "UNPAID", NOW.minusHours(2)), NOW));
        assertFalse(OrderScheduler.isStaleCodPending(order("PENDING", "COD", "PAID", NOW.minusHours(4)), NOW));
        assertFalse(OrderScheduler.isStaleCodPending(order("READY", "BANK_TRANSFER", "PAID", NOW.minusHours(4)), NOW));
        assertFalse(OrderScheduler.isStaleCodPending(order("PENDING", "BANK_TRANSFER", "UNPAID", NOW.minusHours(4)), NOW));
    }

    private Orders order(String status, String paymentMethod, String paymentStatus, LocalDateTime createdAt) {
        Orders order = new Orders();
        order.setOrderStatus(status);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(paymentStatus);
        order.setCreatedAt(createdAt);
        return order;
    }
}
