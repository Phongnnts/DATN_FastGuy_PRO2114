package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import entity.Orders;
import entity.User;

class OrderAccessPolicyTest {
    @Test
    void guestOrderIsNotAccessibleThroughUserEndpoint() {
        assertFalse(OrderService.canUserAccess(new Orders(), 10));
    }

    @Test
    void userCanOnlyAccessOwnOrder() {
        User owner = new User();
        owner.setUserId(10);
        Orders order = new Orders();
        order.setUser(owner);

        assertTrue(OrderService.canUserAccess(order, 10));
        assertFalse(OrderService.canUserAccess(order, 11));
    }
}
