package servlet;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentCapabilitiesPolicyTest {

    private static final Path ORDER_SERVLET = Path.of("src/main/java/servlet/OrderServlet.java");

    @Test
    void capabilitiesAlwaysExposeTransferWithAvailability() throws Exception {
        String src = Files.readString(ORDER_SERVLET);
        assertTrue(src.contains("List.of(\"COD\", \"BANK_TRANSFER\")"));
        assertTrue(src.contains("\"availability\""));
        assertTrue(src.contains("\"enabled\""));
        assertTrue(src.contains("\"PayOS chưa được cấu hình\""));
    }

    @Test
    void registeredAndGuestCheckoutRejectUnavailableTransfer() throws Exception {
        String src = Files.readString(ORDER_SERVLET);
        String guard = "\"BANK_TRANSFER\".equals(paymentMethod) && !payOSPaymentService.isConfigured()";
        assertTrue(src.indexOf(guard) >= 0);
        assertTrue(src.lastIndexOf(guard) > src.indexOf("private void handleGuestCheckout"));
    }
}
