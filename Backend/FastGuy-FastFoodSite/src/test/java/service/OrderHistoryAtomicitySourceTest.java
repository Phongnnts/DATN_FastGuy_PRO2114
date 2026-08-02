package service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OrderHistoryAtomicitySourceTest {
    @Test
    void canonicalTransitionPersistsHistoryAndSideEffectsBeforeCommit() throws Exception {
        String source = Files.readString(Path.of("src/main/java/service/OrderTransitionService.java"));
        int history = source.indexOf("em.persist(new OrderStatusHistory");
        int loyalty = source.indexOf("awardForDelivery(em, order)", history);
        int commit = source.indexOf("em.getTransaction().commit();", loyalty);
        assertTrue(history >= 0 && loyalty > history && commit > loyalty);
    }

    @Test
    void orderServiceDelegatesCancellationAndSchedulerKeepsPaymentGuard() throws Exception {
        String orderService = source("OrderService.java");
        String scheduler = source("OrderScheduler.java");
        assertTrue(orderService.contains("orderTransitionService.cancel("));
        assertTrue(scheduler.contains("false, \"UNPAID\"") || scheduler.contains("true, \"UNPAID\""));
    }

    @Test
    void allRoleServicesDelegateToCanonicalTransition() throws Exception {
        assertTrue(source("StaffOrderService.java").contains("transitionService.transition("));
        assertTrue(source("ShipperService.java").contains("transitionService.transition("));
    }

    private static String source(String file) throws Exception {
        return Files.readString(Path.of("src/main/java/service", file));
    }
}
