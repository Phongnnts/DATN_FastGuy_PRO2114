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
}
