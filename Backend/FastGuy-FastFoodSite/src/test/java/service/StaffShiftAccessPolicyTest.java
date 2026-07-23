package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import servlet.StaffOrderServlet;

class StaffShiftAccessPolicyTest {
    @Test
    void historyBypassesOnlyCheckedInShift() {
        assertFalse(StaffOrderServlet.requiresCheckedInShift("GET", "/history"));
        assertTrue(StaffOrderServlet.hasRouteAccess("GET", "/history", true, false));
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
        assertTrue(StaffOrderServlet.requiresCheckedInShift("GET", "/export"));
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
}
