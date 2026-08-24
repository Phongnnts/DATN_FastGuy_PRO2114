package servlet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AdminInventoryServletPolicyTest {
    @Test
    void allowsNullBlankAndKnownTypes() {
        assertTrue(AdminInventoryServlet.isValidTransactionType(null));
        assertTrue(AdminInventoryServlet.isValidTransactionType(""));
        assertTrue(AdminInventoryServlet.isValidTransactionType("RESERVE"));
        assertTrue(AdminInventoryServlet.isValidTransactionType("RELEASE"));
        assertTrue(AdminInventoryServlet.isValidTransactionType("CONSUME"));
        assertTrue(AdminInventoryServlet.isValidTransactionType("RECEIPT"));
        assertTrue(AdminInventoryServlet.isValidTransactionType("ADJUSTMENT"));
        assertTrue(AdminInventoryServlet.isValidTransactionType("WASTE"));
        assertTrue(AdminInventoryServlet.isValidTransactionType("RETURN"));
    }

    @Test
    void rejectsUnknownTypes() {
        assertFalse(AdminInventoryServlet.isValidTransactionType("DELETE"));
        assertFalse(AdminInventoryServlet.isValidTransactionType("reserve"));
        assertFalse(AdminInventoryServlet.isValidTransactionType("ADJUST"));
    }
}
