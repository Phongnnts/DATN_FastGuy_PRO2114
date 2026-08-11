package service;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckoutShippingPolicyTest {

    private static final Path ORDER_SERVICE = Path.of("src/main/java/service/OrderService.java");

    @Test
    void registeredAndGuestCheckoutRequireGhnFee() throws Exception {
        String src = Files.readString(ORDER_SERVICE);
        assertTrue(src.indexOf("calculateGhnFee(lines, ghnDistrictId, ghnWardCode)") >= 0);
        assertTrue(src.lastIndexOf("calculateGhnFee(lines, ghnDistrictId, ghnWardCode)")
                > src.indexOf("public Orders guestCheckout"));
    }

    @Test
    void invalidGhnFeeFailsClosedWithoutConfiguredFeeFallback() throws Exception {
        String src = Files.readString(ORDER_SERVICE);
        assertTrue(src.contains("Object fee = quote.get(\"fee\");"));
        assertTrue(src.contains("if (!(fee instanceof Number))"));
        assertTrue(src.contains("Không thể tính phí giao hàng từ GHN. Vui lòng thử lại"));
        assertFalse(src.contains("getDeliveryFee()"));
    }
}
