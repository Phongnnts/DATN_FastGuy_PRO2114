package servlet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrderPaymentStatusPolicyTest {
    @Test
    void providerVerificationRunsOnlyAfterAuthorizationForBankTransfer() {
        assertTrue(OrderServlet.shouldVerifyPaymentStatus(true, "BANK_TRANSFER"));
        assertFalse(OrderServlet.shouldVerifyPaymentStatus(false, "BANK_TRANSFER"));
        assertFalse(OrderServlet.shouldVerifyPaymentStatus(true, "COD"));
    }
}
