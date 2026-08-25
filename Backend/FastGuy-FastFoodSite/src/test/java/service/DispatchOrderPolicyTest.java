package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import entity.Orders;
import dao.OrdersDAO;

class DispatchOrderPolicyTest {
    private final DispatchOrderPolicy policy = new DispatchOrderPolicy();

    @Test
    void calculatesSameDayOvernightAndAlwaysOpenClosingTimes() {
        assertEquals(LocalDateTime.of(2026, 8, 25, 22, 0),
                policy.closingAt(LocalDateTime.of(2026, 8, 25, 10, 0), LocalTime.of(8, 0), LocalTime.of(22, 0)));
        assertEquals(LocalDateTime.of(2026, 8, 26, 2, 0),
                policy.closingAt(LocalDateTime.of(2026, 8, 25, 23, 0), LocalTime.of(18, 0), LocalTime.of(2, 0)));
        assertEquals(LocalDateTime.of(2026, 8, 25, 2, 0),
                policy.closingAt(LocalDateTime.of(2026, 8, 25, 1, 0), LocalTime.of(18, 0), LocalTime.of(2, 0)));
        assertNull(policy.closingAt(LocalDateTime.of(2026, 8, 25, 10, 0), LocalTime.MIDNIGHT, LocalTime.MIDNIGHT));
    }

    @Test
    void classifiesReadyBoundaryOldAndClosingOrders() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 25, 21, 30);
        assertEquals("NEW", policy.classify(ready(now.minusMinutes(15), now.minusHours(1)), now,
                LocalTime.of(8, 0), LocalTime.of(23, 0)));
        assertEquals("PRIORITY", policy.classify(ready(now.minusMinutes(5), now.minusHours(1)), now,
                LocalTime.of(8, 0), LocalTime.of(22, 0)));
        assertEquals("PRIORITY", policy.classify(ready(now.minusMinutes(16), now.minusHours(1)), now,
                LocalTime.of(8, 0), LocalTime.of(23, 0)));
        assertNull(policy.classify(ready(now.minusMinutes(5), now.minusHours(2)), now,
                LocalTime.of(8, 0), LocalTime.of(21, 0)));
    }

    @Test
    void classifiesDeliveryFailureForReview() {
        Orders order = new Orders();
        order.setOrderStatus("DELIVERY_FAILED");
        assertEquals("REVIEW", policy.classify(order, LocalDateTime.of(2026, 8, 25, 12, 0),
                LocalTime.of(8, 0), LocalTime.of(22, 0)));
    }

    @Test
    void buildsOverlappingCountsAndDeterministicListsFromOneSnapshot() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 25, 21, 30);
        Orders old = ready(3, now.minusMinutes(20), now.minusHours(1));
        Orders closingNew = ready(2, now.minusMinutes(5), now.minusHours(1));
        Orders newest = ready(4, now.minusMinutes(2), now.minusHours(1));
        Orders reviewLater = failed(9, now.minusMinutes(3));
        Orders reviewEarlier = failed(8, now.minusMinutes(10));
        AtomicInteger configReads = new AtomicInteger();
        AtomicInteger clockReads = new AtomicInteger();
        OrdersDAO dao = new OrdersDAO() {
            @Override public List<Orders> findDispatchCandidates() {
                return List.of(newest, reviewLater, old, closingNew, reviewEarlier);
            }
        };
        StoreConfigService config = new StoreConfigService() {
            @Override public Map<String, String> getAll() {
                configReads.incrementAndGet();
                return Map.of(OPEN_TIME, "08:00", CLOSE_TIME, "22:00");
            }
        };
        StaffOrderService service = new StaffOrderService(dao, config, policy, () -> {
            clockReads.incrementAndGet();
            return now;
        });

        StaffOrderService.DispatchResult priority = service.getDispatchOrders("PRIORITY");

        assertEquals(Map.of("priority", 3L, "new", 2L, "review", 2L), priority.counts());
        assertEquals(List.of(3, 2, 4), priority.items().stream().map(item -> item.order().getOrderId()).toList());
        assertEquals(1, configReads.get());
        assertEquals(1, clockReads.get());

        StaffOrderService.DispatchResult recent = service.getDispatchOrders("NEW");
        assertEquals(List.of(4, 2), recent.items().stream().map(item -> item.order().getOrderId()).toList());
        StaffOrderService.DispatchResult review = service.getDispatchOrders("REVIEW");
        assertEquals(List.of(8, 9), review.items().stream().map(item -> item.order().getOrderId()).toList());
    }

    @Test
    void rejectsMissingOrInvalidBusinessTimesInsteadOfGuessingAlwaysOpen() {
        OrdersDAO dao = candidates(List.of());

        for (Map<String, String> config : List.of(
                Map.of(StoreConfigService.OPEN_TIME, "08:00"),
                Map.of(StoreConfigService.CLOSE_TIME, "22:00"),
                Map.of(StoreConfigService.OPEN_TIME, "invalid", StoreConfigService.CLOSE_TIME, "22:00"))) {
            StaffOrderService service = service(dao, config, LocalDateTime.of(2026, 8, 25, 12, 0));
            assertThrows(IllegalArgumentException.class, () -> service.getDispatchOrders("PRIORITY"));
        }
    }

    @Test
    void classifiesAlwaysOpenReadyOrdersWithNullClosingMinutes() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 25, 12, 0);
        Orders old = ready(1, now.minusMinutes(16), now.minusHours(1));
        Orders recent = ready(2, now.minusMinutes(5), now.minusHours(1));
        StaffOrderService service = service(candidates(List.of(recent, old)),
                Map.of(StoreConfigService.OPEN_TIME, "00:00", StoreConfigService.CLOSE_TIME, "00:00"), now);

        StaffOrderService.DispatchResult priority = service.getDispatchOrders("PRIORITY");
        StaffOrderService.DispatchResult newest = service.getDispatchOrders("NEW");

        assertEquals(Map.of("priority", 1L, "new", 1L, "review", 0L), priority.counts());
        assertEquals(List.of(1), priority.items().stream().map(item -> item.order().getOrderId()).toList());
        assertNull(priority.items().get(0).minutesUntilClose());
        assertEquals(List.of(2), newest.items().stream().map(item -> item.order().getOrderId()).toList());
        assertNull(newest.items().get(0).minutesUntilClose());
    }

    private OrdersDAO candidates(List<Orders> orders) {
        return new OrdersDAO() {
            @Override public List<Orders> findDispatchCandidates() { return orders; }
        };
    }

    private StaffOrderService service(OrdersDAO dao, Map<String, String> values, LocalDateTime now) {
        StoreConfigService config = new StoreConfigService() {
            @Override public Map<String, String> getAll() { return values; }
        };
        return new StaffOrderService(dao, config, policy, () -> now);
    }

    private Orders ready(LocalDateTime readyAt, LocalDateTime createdAt) {
        Orders order = new Orders();
        order.setOrderStatus("READY");
        order.setReadyAt(readyAt);
        order.setCreatedAt(createdAt);
        return order;
    }

    private Orders ready(int id, LocalDateTime readyAt, LocalDateTime createdAt) {
        Orders order = ready(readyAt, createdAt);
        order.setOrderId(id);
        return order;
    }

    private Orders failed(int id, LocalDateTime failedAt) {
        Orders order = new Orders();
        order.setOrderId(id);
        order.setOrderStatus("DELIVERY_FAILED");
        order.setDeliveryFailedAt(failedAt);
        return order;
    }
}
