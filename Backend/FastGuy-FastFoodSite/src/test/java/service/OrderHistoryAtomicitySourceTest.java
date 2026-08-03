package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void allRoleServicesDelegateToCanonicalTransitionWithoutIndependentPrecheck() throws Exception {
        String staff = source("StaffOrderService.java");
        String shipper = source("ShipperService.java");
        String transition = source("OrderTransitionService.java");
        assertTrue(staff.contains("transitionService.transition("));
        assertTrue(shipper.contains("transitionService.transition("));
        int pickup = shipper.indexOf("public boolean pickUpOrder");
        int pickupTransition = shipper.indexOf("transitionService.transition(", pickup);
        int pickupLookup = shipper.indexOf("ordersDAO.findById(orderId)", pickup);
        int delivery = shipper.indexOf("public String deliverOrder");
        int deliveryTransition = shipper.indexOf("transitionService.transition(", delivery);
        int deliveryLookup = shipper.indexOf("ordersDAO.findById(orderId)", delivery);
        assertTrue(pickupTransition > pickup && pickupLookup > pickupTransition);
        assertTrue(deliveryTransition > delivery && deliveryLookup > deliveryTransition);
        int transaction = transition.indexOf("EntityManager em = DatabaseUtil.getEntityManager();", transition.indexOf("public boolean transition(int orderId"));
        int lock = transition.indexOf("em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE)", transaction);
        int shiftCheck = transition.indexOf("requireCheckedInShipper(em, actorUserId)", lock);
        int history = transition.indexOf("em.persist(new OrderStatusHistory", shiftCheck);
        int commit = transition.indexOf("em.getTransaction().commit();", history);
        assertTrue(transaction >= 0 && lock > transaction && shiftCheck > lock && history > shiftCheck && commit > history);
    }

    private static String source(String file) throws Exception {
        return Files.readString(Path.of("src/main/java/service", file));
    }
}
