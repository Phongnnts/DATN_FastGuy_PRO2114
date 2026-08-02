package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CheckoutIdempotencyPolicyTest {
    @Test
    void keyCanOnlyBeReusedForSameRequest() {
        assertTrue(OrderService.matchesRequestHash("abc", "abc"));
        assertFalse(OrderService.matchesRequestHash("abc", "def"));
        assertFalse(OrderService.matchesRequestHash(null, "abc"));
    }

    @Test
    void lockSqlStartsPhysicalTransaction() {
        assertEquals("IF @@TRANCOUNT = 0 BEGIN TRANSACTION; DECLARE @result int; EXEC @result = sp_getapplock "
                        + "@Resource = :resource, @LockMode = 'Exclusive', @LockOwner = 'Transaction', "
                        + "@LockTimeout = 10000; SELECT @result",
                OrderService.idempotencyLockSql());
    }
}
