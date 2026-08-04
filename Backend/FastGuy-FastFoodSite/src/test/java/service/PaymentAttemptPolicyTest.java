package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PaymentAttemptPolicyTest {
    @Test
    void onlyNewAttemptCreatesProviderLink() {
        assertTrue(PayOSPaymentService.shouldCreateProviderLink(true, "CREATING"));
        assertFalse(PayOSPaymentService.shouldCreateProviderLink(false, "CREATING"));
        assertFalse(PayOSPaymentService.shouldCreateProviderLink(true, "READY"));
    }

    @Test
    void terminalRefundNeverResurrectedByWebhook() {
        assertTrue(PayOSPaymentService.shouldMarkPaid("UNPAID", null));
        assertTrue(PayOSPaymentService.shouldMarkPaid("UNPAID", "PENDING"));
        assertFalse(PayOSPaymentService.shouldMarkPaid("PAID", "PENDING"));
        assertFalse(PayOSPaymentService.shouldMarkPaid("UNPAID", "REFUNDED"));
        assertFalse(PayOSPaymentService.shouldMarkPaid("REFUNDED", "REFUNDED"));
        assertFalse(PayOSPaymentService.shouldMarkPaid("UNPAID", "REJECTED"));
        assertTrue(RefundService.isTerminal("REFUNDED"));
        assertTrue(RefundService.isTerminal("REJECTED"));
    }
}
