package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import servlet.StaffOrderServlet;
import servlet.StaffSupportTicketServlet;

class StaffShiftAccessPolicyTest {
    @Test
    void historyAndExportBypassOnlyCheckedInShift() {
        assertFalse(StaffOrderServlet.requiresCheckedInShift("GET", "/history"));
        assertFalse(StaffOrderServlet.requiresCheckedInShift("GET", "/export"));
        assertTrue(StaffOrderServlet.hasRouteAccess("GET", "/history", true, false));
        assertTrue(StaffOrderServlet.hasRouteAccess("GET", "/export", true, false));
        assertFalse(StaffOrderServlet.hasRouteAccess("GET", "/history", false, true));
        assertTrue(StaffOrderServlet.requiresCheckedInShift("POST", "/history"));
    }

    @Test
    void everyActiveOrderRouteRequiresCheckedInShift() {
        assertTrue(StaffOrderServlet.requiresCheckedInShift("GET", null));
        assertTrue(StaffOrderServlet.requiresCheckedInShift("GET", "/"));
        assertTrue(StaffOrderServlet.requiresCheckedInShift("GET", "/shippers"));
        assertTrue(StaffOrderServlet.requiresCheckedInShift("GET", "/confirmed"));
        assertTrue(StaffOrderServlet.requiresCheckedInShift("GET", "/preparing"));
        assertTrue(StaffOrderServlet.requiresCheckedInShift("GET", "/ready"));
        assertFalse(StaffOrderServlet.requiresCheckedInShift("GET", "/export"));
        assertTrue(StaffOrderServlet.requiresCheckedInShift("GET", "/12"));
        assertTrue(StaffOrderServlet.requiresCheckedInShift("POST", "/12/notes"));
        assertTrue(StaffOrderServlet.requiresCheckedInShift("PUT", "/12/status"));
        assertTrue(StaffOrderServlet.requiresCheckedInShift("PUT", "/12/assign-shipper"));
    }

    @Test
    void currentIdentityRequiresActiveStaffAccount() {
        assertTrue(StaffShiftAccessService.isValidStaffIdentity("STAFF", "ACTIVE"));
        assertFalse(StaffShiftAccessService.isValidStaffIdentity("USER", "ACTIVE"));
        assertFalse(StaffShiftAccessService.isValidStaffIdentity("STAFF", "INACTIVE"));
        assertFalse(StaffShiftAccessService.isValidStaffIdentity(null, "ACTIVE"));
        assertFalse(StaffShiftAccessService.isValidStaffIdentity("STAFF", null));
    }

    @Test
    void checkedInStateIsIndependentFromIdentity() {
        assertTrue(StaffShiftAccessService.isCheckedIn("CHECKED_IN"));
        assertFalse(StaffShiftAccessService.isCheckedIn("UPCOMING"));
        assertFalse(StaffShiftAccessService.isCheckedIn(null));
    }

    @Test
    void supportReadsRequireActiveStaffButMutationsAlsoRequireShift() {
        assertTrue(StaffSupportTicketServlet.hasRouteAccess("GET", true, false));
        assertFalse(StaffSupportTicketServlet.hasRouteAccess("GET", false, true));
        assertTrue(StaffSupportTicketServlet.hasRouteAccess("PUT", true, true));
        assertFalse(StaffSupportTicketServlet.hasRouteAccess("PUT", true, false));
        assertFalse(StaffSupportTicketServlet.hasRouteAccess("PUT", false, true));
    }

    @Test
    void shipperTerminalDetailCanBeReadOutsideShift() {
        assertTrue(ShipperShiftAccessService.canReadOwnedOrder("DELIVERED", false));
        assertTrue(ShipperShiftAccessService.canReadOwnedOrder("CANCELLED", false));
        assertTrue(ShipperShiftAccessService.canReadOwnedOrder("ASSIGNED", true));
        assertFalse(ShipperShiftAccessService.canReadOwnedOrder("ASSIGNED", false));
        assertFalse(ShipperShiftAccessService.canReadOwnedOrder("PICKED_UP", false));
    }

    @Test
    void shipperMutationsAlwaysRequireCheckedInShift() throws IOException {
        String servlet = Files.readString(Path.of("src/main/java/servlet/ShipperServlet.java"));
        int putStart = servlet.indexOf("protected void doPut");
        int shiftGuard = servlet.indexOf("requireCheckedInShift(req, resp, shipperId)", putStart);
        int pathParse = servlet.indexOf("parseMutationPath(req.getPathInfo())", putStart);
        int actionDispatch = servlet.indexOf("switch (mutation.action())", putStart);

        assertTrue(shiftGuard > putStart);
        assertTrue(pathParse > shiftGuard);
        assertTrue(actionDispatch > pathParse);
    }
}
