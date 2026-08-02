package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AdminUserOrderPolicyTest {
    @Test
    void selectsOrderRelationshipByRole() {
        assertEquals("CUSTOMER", AdminUserServlet.orderRelationship("USER"));
        assertEquals("STAFF", AdminUserServlet.orderRelationship("STAFF"));
        assertEquals("SHIPPER", AdminUserServlet.orderRelationship("SHIPPER"));
        assertEquals("NONE", AdminUserServlet.orderRelationship("ADMIN"));
    }
}
