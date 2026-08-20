package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import servlet.StaffOrderServlet;

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
        assertTrue(service.contains("ws.shiftDate = :today"));
        assertTrue(service.contains("WorkShiftService.isValidCheckedInShift"));
        assertTrue(dao.contains("o.assignedAt >= :shiftStart"));
        for (String field : new String[]{"id", "fullName", "phone", "activeOrderCount"}) {
            assertTrue(servlet.contains("m.put(\"" + field + "\""), field);
        }
    }

    @Test
    void staleShipperSelectionIsUnprocessable() {
        assertTrue(StaffOrderServlet.statusForAssignment(OrderTransitionService.MutationResult.UNPROCESSABLE) == 422);
        assertTrue(StaffOrderServlet.statusForAssignment(OrderTransitionService.MutationResult.CONFLICT) == 409);
        assertTrue(StaffOrderServlet.statusForAssignment(OrderTransitionService.MutationResult.SUCCESS) == 200);
    }

    @Test
    void workloadQueryRequiresCurrentShiftStart() throws IOException {
        String dao = Files.readString(Path.of("src/main/java/dao/OrdersDAO.java"));
        String service = Files.readString(Path.of("src/main/java/service/StaffOrderService.java"));
        assertTrue(dao.contains("countActiveByShipper(int shipperId, LocalDateTime shiftStart)"));
        assertTrue(service.contains("countActiveOrders(int shipperId, LocalDateTime shiftStart)"));
        String servlet = Files.readString(Path.of("src/main/java/servlet/StaffOrderServlet.java"));
        assertTrue(servlet.contains("countActiveOrders(user.getUserId(), shift.getCheckInAt())"));
    }
}
