package service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StaffHandoverLockOrderTest {
    @Test
    void claimLocksAndRevalidatesReceivingShiftBeforeOrder() throws Exception {
        String source = Files.readString(Path.of("src/main/java/service/OrderTransitionService.java"));
        int start = source.indexOf("public MutationResult claimHandover");
        int end = source.indexOf("static boolean matchesExpectedStatus", start);
        String body = source.substring(start, end);

        int shift = body.indexOf("currentActiveShift(em, staff, \"STAFF\")");
        int revalidate = body.indexOf("isCurrentActiveShift(receiving, staff, \"STAFF\"");
        int order = body.indexOf("em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE)");
        assertTrue(shift >= 0);
        assertTrue(revalidate > shift);
        assertTrue(order > revalidate);
    }
}
