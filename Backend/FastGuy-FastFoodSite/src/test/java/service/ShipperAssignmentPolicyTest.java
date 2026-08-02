package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShipperAssignmentPolicyTest {
    @Test
    void pickupRequiresAssignmentToCurrentShipper() {
        assertTrue(ShipperService.canPickUp("ASSIGNED", 3, 3));
        assertFalse(ShipperService.canPickUp("READY", 3, 3));
        assertFalse(ShipperService.canPickUp("ASSIGNED", 2, 3));
        assertFalse(ShipperService.canPickUp("ASSIGNED", null, 3));
    }
}
