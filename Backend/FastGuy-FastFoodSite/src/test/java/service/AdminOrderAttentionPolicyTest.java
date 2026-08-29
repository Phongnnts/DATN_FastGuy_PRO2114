package service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import entity.Orders;

class AdminOrderAttentionPolicyTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 29, 12, 0);

    @Test
    void classifiesEveryApplicableReasonWithoutDuplicates() {
        Orders order = order(1, "DELIVERY_FAILED", NOW.minusHours(1));
        order.setRefundStatus("PENDING");

        assertEquals(List.of("DELIVERY_FAILED", "PENDING_REFUND"), AdminOrderAttentionPolicy.reasons(order, NOW));
    }

    @Test
    void usesOrderExpiryPolicyForProcessingOverdue() {
        Orders overdue = order(2, "CONFIRMED", NOW.minusMinutes(16));
        Orders active = order(3, "CONFIRMED", NOW.minusMinutes(14));

        assertEquals(List.of("PROCESSING_OVERDUE"), AdminOrderAttentionPolicy.reasons(overdue, NOW));
        assertEquals(List.of(), AdminOrderAttentionPolicy.reasons(active, NOW));
    }

    @Test
    void ordersByReasonPriorityThenOldestRelevantTime() {
        Orders refund = order(4, "CANCELLED", NOW.minusHours(4));
        refund.setRefundStatus("PENDING");
        Orders overdue = order(5, "PENDING", NOW.minusMinutes(30));
        Orders failedNewer = order(6, "DELIVERY_FAILED", NOW.minusHours(1));
        failedNewer.setDeliveryFailedAt(NOW.minusMinutes(20));
        Orders failedOlder = order(7, "DELIVERY_FAILED", NOW.minusHours(1));
        failedOlder.setDeliveryFailedAt(NOW.minusMinutes(40));

        assertEquals(List.of(7, 6, 5, 4), AdminOrderAttentionPolicy.sort(List.of(refund, overdue, failedNewer, failedOlder), NOW)
                .stream().map(Orders::getOrderId).toList());
    }

    private Orders order(int id, String status, LocalDateTime enteredAt) {
        Orders order = new Orders();
        order.setOrderId(id);
        order.setOrderStatus(status);
        order.setCreatedAt(enteredAt);
        order.setStatusEnteredAt(enteredAt);
        return order;
    }
}
