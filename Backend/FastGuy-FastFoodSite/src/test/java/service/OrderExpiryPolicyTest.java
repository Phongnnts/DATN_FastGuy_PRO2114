package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import entity.Orders;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class OrderExpiryPolicyTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 28, 12, 0);

    @Test
    void appliesStatusAndPaymentSpecificTimeoutsAtBoundary() {
        assertExpired("PENDING", "BANK_TRANSFER", "UNPAID", 15);
        assertExpired("PENDING", "COD", "UNPAID", 10);
        assertExpired("CONFIRMED", "COD", "UNPAID", 15);
        assertExpired("PREPARING", "COD", "UNPAID", 20);
        assertExpired("READY", "COD", "UNPAID", 15);
    }

    @Test
    void ignoresBeforeBoundaryAndDeliveryOrTerminalStates() {
        Orders order = order("PENDING", "COD", "UNPAID", 9);
        assertFalse(OrderExpiryPolicy.isExpired(order, NOW));
        for (String status : new String[] {"ASSIGNED", "PICKED_UP", "DELIVERED", "CANCELLED", "DELIVERY_FAILED", "RETURNED_TO_STORE"}) {
            order.setOrderStatus(status);
            assertFalse(OrderExpiryPolicy.isExpired(order, NOW));
        }
    }

    @Test
    void cutoffOnlyCancelsStoreHeldStatuses() {
        Orders order = order("PENDING", "COD", "UNPAID", 1);
        for (String status : new String[] {"PENDING", "CONFIRMED", "PREPARING", "READY"}) {
            order.setOrderStatus(status);
            assertTrue(OrderExpiryPolicy.isCutoffCancellationCandidate(order));
        }
        for (String status : new String[] {"ASSIGNED", "PICKED_UP", "DELIVERED", "CANCELLED"}) {
            order.setOrderStatus(status);
            assertFalse(OrderExpiryPolicy.isCutoffCancellationCandidate(order));
        }
    }

    @Test
    void calculatesSharedSerializerMetadataFromStatusPolicy() {
        Orders order = order("PENDING", "COD", "UNPAID", 5);
        OrderExpiryPolicy.Metadata metadata = OrderExpiryPolicy.metadata(order, NOW);
        assertEquals(NOW.minusMinutes(5), metadata.statusEnteredAt());
        assertEquals(NOW.plusMinutes(5), metadata.expiresAt());
        assertEquals(300L, metadata.remainingSeconds());
        assertEquals("AUTO_CANCEL", metadata.timeoutPolicy());
    }

    @Test
    void returnsNullableTimeoutMetadataOutsideStoreHeldStates() {
        Orders order = order("ASSIGNED", "COD", "UNPAID", 5);
        OrderExpiryPolicy.Metadata metadata = OrderExpiryPolicy.metadata(order, NOW);
        assertEquals(order.getStatusEnteredAt(), metadata.statusEnteredAt());
        assertTrue(metadata.expiresAt() == null);
        assertTrue(metadata.remainingSeconds() == null);
        assertTrue(metadata.timeoutPolicy() == null);
    }

    @Test
    void realTransitionUpdatesTimestampWhileSameStatusDoesNot() {
        Orders order = order("READY", "COD", "UNPAID", 5);
        LocalDateTime entered = order.getStatusEnteredAt();
        OrderTransitionService.applyStatus(order, "READY", NOW);
        assertEquals(entered, order.getStatusEnteredAt());
        OrderTransitionService.applyStatus(order, "ASSIGNED", NOW);
        assertEquals(NOW, order.getStatusEnteredAt());
    }

    private void assertExpired(String status, String method, String payment, long minutes) {
        Orders order = order(status, method, payment, minutes);
        assertTrue(OrderExpiryPolicy.isExpired(order, NOW));
    }

    private Orders order(String status, String method, String payment, long minutes) {
        Orders order = new Orders();
        order.setOrderStatus(status);
        order.setPaymentMethod(method);
        order.setPaymentStatus(payment);
        order.setStatusEnteredAt(NOW.minusMinutes(minutes));
        return order;
    }
}
