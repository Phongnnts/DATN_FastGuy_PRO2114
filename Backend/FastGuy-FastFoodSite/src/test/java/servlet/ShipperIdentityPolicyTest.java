package servlet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShipperIdentityPolicyTest {
    @Test
    void requiresCurrentActiveShipperIdentity() {
        assertTrue(ShipperServlet.isActiveShipper("SHIPPER", "ACTIVE"));
        assertFalse(ShipperServlet.isActiveShipper("SHIPPER", "INACTIVE"));
        assertFalse(ShipperServlet.isActiveShipper("USER", "ACTIVE"));
    }
}
