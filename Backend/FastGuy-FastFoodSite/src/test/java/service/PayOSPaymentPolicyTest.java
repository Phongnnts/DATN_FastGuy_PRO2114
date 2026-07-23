package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import entity.Orders;

class PayOSPaymentPolicyTest {
    @Test
    void latePaymentOnCancelledOrderRequestsRefund() {
        Orders order = new Orders();
        order.setOrderStatus("CANCELLED");
        order.setPaymentStatus("UNPAID");

        PayOSPaymentService.markPaid(order, LocalDateTime.now());

        assertEquals("PAID", order.getPaymentStatus());
        assertEquals("PENDING", order.getRefundStatus());
    }

    @Test
    void normalPaymentDoesNotRequestRefund() {
        Orders order = new Orders();
        order.setOrderStatus("PENDING");
        order.setPaymentStatus("UNPAID");

        PayOSPaymentService.markPaid(order, LocalDateTime.now());

        assertEquals("PAID", order.getPaymentStatus());
        assertNull(order.getRefundStatus());
    }

}
