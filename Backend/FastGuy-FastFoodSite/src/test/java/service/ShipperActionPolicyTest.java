package service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ShipperActionPolicyTest {
    @Test
    void assignedOrderAllowsOnlyPickup() {
        assertEquals(Set.of("PICKED_UP"), ShipperService.getAllowedActions("ASSIGNED", "COD", "UNPAID"));
    }

    @Test
    void codPickedUpOrderAllowsDeliveryCollection() {
        assertEquals(Set.of("DELIVERED"), ShipperService.getAllowedActions("PICKED_UP", "COD", "UNPAID"));
    }

    @Test
    void nonCodDeliveryRequiresPaidStatus() {
        assertEquals(Set.of(), ShipperService.getAllowedActions("PICKED_UP", "BANK_TRANSFER", "UNPAID"));
        assertEquals(Set.of("DELIVERED"), ShipperService.getAllowedActions("PICKED_UP", "BANK_TRANSFER", "PAID"));
    }

    @Test
    void terminalAndUnassignedOrdersExposeNoActions() {
        assertEquals(Set.of(), ShipperService.getAllowedActions("DELIVERED", "COD", "PAID"));
        assertEquals(Set.of(), ShipperService.getAllowedActions("CANCELLED", "COD", "PAID"));
        assertEquals(Set.of(), ShipperService.getAllowedActions("READY", "COD", "UNPAID"));
    }
}
