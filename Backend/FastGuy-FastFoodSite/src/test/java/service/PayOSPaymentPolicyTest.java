package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void providerResponseRequiresExactOrderAmountAndStoredReference() {
        Map<String, Object> matching = Map.of("orderCode", 12, "amount", 45000, "paymentLinkId", "ref");

        assertTrue(PayOSPaymentService.matchesProviderResponse(matching, "ref", 12, 45000));
        assertFalse(PayOSPaymentService.matchesProviderResponse(matching, "other", 12, 45000));
        assertFalse(PayOSPaymentService.matchesProviderResponse(matching, "", 12, 45000));
        assertFalse(PayOSPaymentService.matchesProviderResponse(matching, null, 12, 45000));
        assertFalse(PayOSPaymentService.matchesProviderResponse(
                Map.of("amount", 45000, "paymentLinkId", "ref"), "ref", 12, 45000));
        assertFalse(PayOSPaymentService.matchesProviderResponse(
                Map.of("orderCode", 12, "amount", 45001, "paymentLinkId", "ref"), "ref", 12, 45000));
        assertFalse(PayOSPaymentService.matchesProviderResponse(
                Map.of("orderCode", 12, "amount", 45000, "paymentLinkId", ""), "ref", 12, 45000));
    }

    @Test
    void webhookRequiresExactOrderAndAttemptReferenceAndAmount() {
        assertTrue(PayOSPaymentService.matchesWebhookPayment("ref", 45000, "ref", 45000, "ref"));
        assertFalse(PayOSPaymentService.matchesWebhookPayment(null, 45000, "ref", 45000, "ref"));
        assertFalse(PayOSPaymentService.matchesWebhookPayment("ref", 45000, null, 45000, "ref"));
        assertFalse(PayOSPaymentService.matchesWebhookPayment("ref", 45000, "ref", 45001, "ref"));
        assertFalse(PayOSPaymentService.matchesWebhookPayment("ref", 45000, "ref", 45000, "other"));
    }

    @Test
    void providerPaidReconcilesCancelledOrderEvenAfterTerminalRefundDecision() {
        Orders order = new Orders();
        order.setOrderStatus("CANCELLED");
        order.setPaymentStatus("UNPAID");
        order.setRefundStatus("REJECTED");

        PayOSPaymentService.reconcilePaidOrder(order, LocalDateTime.now());

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
