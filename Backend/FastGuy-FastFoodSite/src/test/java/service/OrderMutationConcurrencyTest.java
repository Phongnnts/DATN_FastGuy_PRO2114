package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import entity.Orders;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import servlet.StaffOrderServlet;

class OrderMutationConcurrencyTest {
    @Test
    void expectedStatusMustMatchLockedOrder() {
        Orders order = new Orders();
        order.setOrderStatus("READY");

        assertTrue(OrderTransitionService.matchesExpectedStatus(order, "READY"));
        assertFalse(OrderTransitionService.matchesExpectedStatus(order, "PREPARING"));
        assertFalse(OrderTransitionService.matchesExpectedStatus(order, null));
    }

    @Test
    void staffMutationContractCarriesExpectedStatusAndConflict() throws IOException {
        String transition = Files.readString(Path.of("src/main/java/service/OrderTransitionService.java"));
        String staff = Files.readString(Path.of("src/main/java/service/StaffOrderService.java"));
        String servlet = Files.readString(Path.of("src/main/java/servlet/StaffOrderServlet.java"));

        assertTrue(transition.contains("matchesExpectedStatus(order, expectedStatus)"));
        assertTrue(staff.contains("String expectedStatus"));
        assertTrue(servlet.contains("body.get(\"expectedStatus\")"));
        assertTrue(servlet.contains("MutationResult.CONFLICT"));
        assertTrue(servlet.contains("409"));
        assertEquals("Đơn hàng đã được cập nhật. Vui lòng thử lại.", StaffOrderServlet.CONFLICT_MESSAGE);
    }
}
