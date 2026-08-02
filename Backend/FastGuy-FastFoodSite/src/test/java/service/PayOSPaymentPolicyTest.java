package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.Map;

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
    void providerResponseRequiresExactOrderAmountAndReference() {
        assertEquals(true, PayOSPaymentService.matchesProviderResponse(
                Map.of("orderCode", 12, "amount", 45000, "paymentLinkId", "ref"), 12, 45000));
        assertEquals(false, PayOSPaymentService.matchesProviderResponse(Map.of("amount", 45000, "paymentLinkId", "ref"), 12, 45000));
        assertEquals(false, PayOSPaymentService.matchesProviderResponse(
                Map.of("orderCode", 12, "amount", 45001, "paymentLinkId", "ref"), 12, 45000));
        assertEquals(false, PayOSPaymentService.matchesProviderResponse(
                Map.of("orderCode", 12, "amount", 45000, "paymentLinkId", ""), 12, 45000));
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
