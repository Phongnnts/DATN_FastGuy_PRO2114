package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dao.OrdersDAO;
import entity.Orders;
import entity.User;

class OrderSchedulerClosingPolicyTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 25, 22, 0);

    @Test
    void onlyReadyUnassignedOrdersAtOrAfterClosingAreCandidates() {
        Orders atClosing = order(1, "READY", NOW.toLocalDate().atTime(10, 0));
        Orders afterClosing = order(2, "READY", NOW.minusDays(1));
        Orders beforeClosing = order(3, "READY", NOW.plusDays(1).withHour(10));
        Orders assignedReady = order(4, "READY", NOW.minusDays(1));
        assignedReady.setShipper(new User());

        List<Orders> candidates = OrderScheduler.closingCandidates(
                List.of(atClosing, afterClosing, beforeClosing, assignedReady,
                        order(5, "ASSIGNED", NOW.minusDays(1)),
                        order(6, "PICKED_UP", NOW.minusDays(1)),
                        order(7, "DELIVERY_FAILED", NOW.minusDays(1)),
                        order(8, "CANCELLED", NOW.minusDays(1))),
                NOW, LocalTime.of(8, 0), LocalTime.of(22, 0));

        assertEquals(List.of(1, 2), candidates.stream().map(Orders::getOrderId).toList());
    }

    @Test
    void equalHoursAndInvalidConfigurationFailClosed() {
        Orders overdue = order(1, "READY", NOW.minusDays(1));

        assertTrue(OrderScheduler.closingCandidates(List.of(overdue), NOW, LocalTime.MIDNIGHT,
                LocalTime.MIDNIGHT).isEmpty());
        assertTrue(OrderScheduler.parseBusinessHours(Map.of(
                StoreConfigService.OPEN_TIME, "invalid",
                StoreConfigService.CLOSE_TIME, "22:00")).isEmpty());
        assertTrue(OrderScheduler.parseBusinessHours(Map.of(StoreConfigService.OPEN_TIME, "08:00")).isEmpty());
    }

    @Test
    void schedulerReadsConfigurationOnceAndContinuesAfterCancellationFailure() {
        CountingConfig config = new CountingConfig();
        StubOrdersDAO orders = new StubOrdersDAO(List.of(
                order(1, "READY", NOW.minusDays(1)),
                order(2, "READY", NOW.minusDays(1))));
        RecordingTransition transitions = new RecordingTransition();
        transitions.failureId = 1;
        OrderScheduler scheduler = new OrderScheduler(orders, config, transitions, () -> NOW);

        scheduler.cancelReadyOrdersAfterClosing();

        assertEquals(1, config.reads);
        assertEquals(List.of(1, 2), transitions.attempted);
    }

    @Test
    void atomicCancellationPredicateRechecksEveryBindingCondition() {
        Orders order = order(1, "READY", NOW.toLocalDate().atTime(10, 0));
        assertTrue(OrderTransitionService.canAutoCancelAfterClosing(order, NOW,
                LocalTime.of(8, 0), LocalTime.of(22, 0)));

        order.setOrderStatus("ASSIGNED");
        assertFalse(OrderTransitionService.canAutoCancelAfterClosing(order, NOW,
                LocalTime.of(8, 0), LocalTime.of(22, 0)));
        order.setOrderStatus("READY");
        order.setShipper(new User());
        assertFalse(OrderTransitionService.canAutoCancelAfterClosing(order, NOW,
                LocalTime.of(8, 0), LocalTime.of(22, 0)));
        order.setShipper(null);
        assertFalse(OrderTransitionService.canAutoCancelAfterClosing(order, NOW.minusSeconds(1),
                LocalTime.of(8, 0), LocalTime.of(22, 0)));
        assertFalse(OrderTransitionService.canAutoCancelAfterClosing(order, NOW,
                LocalTime.MIDNIGHT, LocalTime.MIDNIGHT));
    }

    private static Orders order(int id, String status, LocalDateTime createdAt) {
        Orders order = new Orders();
        order.setOrderId(id);
        order.setOrderStatus(status);
        order.setCreatedAt(createdAt);
        return order;
    }

    private static final class CountingConfig extends StoreConfigService {
        int reads;

        @Override
        public Map<String, String> getAll() {
            reads++;
            return Map.of(OPEN_TIME, "08:00", CLOSE_TIME, "22:00");
        }
    }

    private static final class StubOrdersDAO extends OrdersDAO {
        private final List<Orders> orders;

        StubOrdersDAO(List<Orders> orders) {
            this.orders = orders;
        }

        @Override
        public List<Orders> findReadyWithoutShipperForClosing() {
            return orders;
        }
    }

    private static final class RecordingTransition extends OrderTransitionService {
        final List<Integer> attempted = new ArrayList<>();
        int failureId;

        @Override
        public CancellationResult cancelReadyIfUnassignedAfterClosing(int orderId, LocalDateTime now,
                                                                       LocalTime open, LocalTime close) {
            attempted.add(orderId);
            if (orderId == failureId) throw new IllegalStateException("expected test failure");
            return null;
        }
    }
}
