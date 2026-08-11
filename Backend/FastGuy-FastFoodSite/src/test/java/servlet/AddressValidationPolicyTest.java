package servlet;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AddressValidationPolicyTest {
    private static final Path SOURCE = Path.of("src/main/java/servlet/AddressServlet.java");

    @Test
    void invalidAddressReturns400BeforePostServiceCall() throws IOException {
        String source = Files.readString(SOURCE);
        String post = source.substring(source.indexOf("protected void doPost"), source.indexOf("protected void doPut"));

        assertValidationReturnsBefore(post, "addressService.create");
    }

    @Test
    void invalidAddressReturns400BeforePutServiceCall() throws IOException {
        String source = Files.readString(SOURCE);
        String put = source.substring(source.indexOf("protected void doPut"), source.indexOf("protected void doDelete"));

        assertValidationReturnsBefore(put, "addressService.update");
    }

    private void assertValidationReturnsBefore(String method, String serviceCall) {
        int validation = method.indexOf("AddressValidator.validate(body)");
        int badRequest = method.indexOf("ApiResponse.error(resp, validationError, 400)", validation);
        int earlyReturn = method.indexOf("return;", badRequest);
        int conversion = method.indexOf("toAddress(body)");
        int service = method.indexOf(serviceCall);

        assertTrue(validation >= 0);
        assertTrue(badRequest > validation);
        assertTrue(earlyReturn > badRequest);
        assertTrue(conversion > earlyReturn);
        assertTrue(service > earlyReturn);
    }
}
