package servlet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OrderPaymentStatusPolicyTest {
    @Test
    void providerVerificationRunsOnlyAfterAuthorizationForBankTransfer() {
        assertTrue(OrderServlet.shouldVerifyPaymentStatus(true, "BANK_TRANSFER"));
        assertFalse(OrderServlet.shouldVerifyPaymentStatus(false, "BANK_TRANSFER"));
        assertFalse(OrderServlet.shouldVerifyPaymentStatus(true, "COD"));
    }

    @Test
    void authenticatedPaymentStatusIncludesOrderStatus() throws Exception {
        String source = Files.readString(Path.of("src/main/java/servlet/OrderServlet.java"));
        int authenticatedStatus = source.indexOf("if (path != null && path.endsWith(\"/payment-status\"))");
        int nextEndpoint = source.indexOf("if (\"/history\".equals(path))", authenticatedStatus);
        String branch = source.substring(authenticatedStatus, nextEndpoint);
        assertTrue(branch.contains("pd.put(\"orderStatus\", order.getOrderStatus())"));
    }

    @Test
    void providerCancelledStatusUsesCanonicalUnpaidPendingCancellation() throws Exception {
        String source = Files.readString(Path.of("src/main/java/service/PayOSPaymentService.java"));
        assertTrue(source.contains("\"CANCELLED\".equals(status)"));
        assertTrue(source.contains("cancelOrder(orderId, null, null"));
        assertTrue(source.contains("true, \"UNPAID\")"));
    }

    @Test
    void guestCheckoutAllowsOnlyBankTransfer() {
        assertTrue(OrderServlet.isGuestPaymentAllowed("BANK_TRANSFER"));
        assertFalse(OrderServlet.isGuestPaymentAllowed("COD"));
        assertFalse(OrderServlet.isGuestPaymentAllowed(null));
    }
}
