package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import service.OrderTransitionService;
class ShipperOptimisticConflictContractTest {
    @Test
    void expectedStatusValidationAcceptsOnlyCanonicalNonBlankStatus() {
        assertTrue(ShipperServlet.isValidExpectedStatus("ASSIGNED"));
        assertTrue(ShipperServlet.isValidExpectedStatus("PICKED_UP"));
        assertFalse(ShipperServlet.isValidExpectedStatus(null));
        assertFalse(ShipperServlet.isValidExpectedStatus(""));
        assertFalse(ShipperServlet.isValidExpectedStatus("   "));
        assertFalse(ShipperServlet.isValidExpectedStatus("NOT_A_STATUS"));
    }

    @Test
    void shipperMutationUsesLockedTransitionResultAndMapsConflictTo409() throws IOException {
        String servlet = Files.readString(Path.of("src/main/java/servlet/ShipperServlet.java"));
        String service = Files.readString(Path.of("src/main/java/service/ShipperService.java"));

        assertTrue(servlet.contains("body.get(\"expectedStatus\")"));
        assertTrue(servlet.contains("MutationResult.CONFLICT"));
        assertTrue(servlet.contains("409"));
        assertEquals(422, ShipperServlet.statusFor(OrderTransitionService.MutationResult.UNPROCESSABLE));
        assertTrue(service.contains("String expectedStatus"));
        assertTrue(service.contains("transitionService.transition"));
        assertEquals("Đơn hàng đã thay đổi trạng thái. Dữ liệu mới nhất đã được tải lại.", ShipperServlet.CONFLICT_MESSAGE);
    }
}
