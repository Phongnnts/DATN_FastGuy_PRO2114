package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ShipperAssignmentPolicyTest {
    @Test
    void pickupRequiresAssignmentToCurrentShipper() {
        assertTrue(ShipperService.canPickUp("ASSIGNED", 3, 3));
        assertFalse(ShipperService.canPickUp("READY", 3, 3));
        assertFalse(ShipperService.canPickUp("ASSIGNED", 2, 3));
        assertFalse(ShipperService.canPickUp("ASSIGNED", null, 3));
    }

    @Test
    void availableShippersExposeActiveWorkload() throws IOException {
        String dao = Files.readString(Path.of("src/main/java/dao/OrdersDAO.java"));
        String service = Files.readString(Path.of("src/main/java/service/StaffOrderService.java"));
        String servlet = Files.readString(Path.of("src/main/java/servlet/StaffOrderServlet.java"));

        assertTrue(dao.contains("countActiveByShipper"));
        assertTrue(dao.contains("'ASSIGNED','PICKED_UP'"));
        assertTrue(service.contains("ws.user.status = 'ACTIVE'"));
        assertTrue(service.contains("ws.status = 'CHECKED_IN'"));
        assertTrue(service.contains("ws.checkInAt IS NOT NULL"));
        assertTrue(service.contains("ws.checkOutAt IS NULL"));
        for (String field : new String[]{"id", "fullName", "phone", "activeOrderCount"}) {
            assertTrue(servlet.contains("m.put(\"" + field + "\""), field);
        }
    }
}
