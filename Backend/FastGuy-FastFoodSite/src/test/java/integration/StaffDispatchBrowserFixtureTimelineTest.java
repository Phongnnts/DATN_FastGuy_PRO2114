package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import entity.Orders;
import entity.User;
import entity.WorkShift;
import service.DispatchOrderPolicy;
import service.WorkShiftService;

class StaffDispatchBrowserFixtureTimelineTest {
    private final DispatchOrderPolicy policy = new DispatchOrderPolicy();

    @ParameterizedTest
    @ValueSource(strings = { "00:05", "05:59", "13:59", "23:55" })
    void timelineKeepsActiveOrdersOpenAndCancellationCandidateOverdue(String wallTime) {
        LocalDateTime now = LocalDate.of(2026, 8, 26).atTime(LocalTime.parse(wallTime));
        var timeline = StaffDispatchBrowserFixtureIT.timeline(now);
        List<Orders> active = List.of(
                order("READY", now.minusMinutes(45)),
                order("READY", now.minusMinutes(40)),
                order("READY", now.minusMinutes(1)),
                order("READY", now.minusMinutes(3)));

        assertEquals(2, active.stream().filter(order -> "PRIORITY".equals(policy.classify(order, now, timeline.open(), timeline.close()))).count());
        assertEquals(2, active.stream().filter(order -> "NEW".equals(policy.classify(order, now, timeline.open(), timeline.close()))).count());
        assertTrue(policy.closingAt(timeline.cancellationCreatedAt(), timeline.open(), timeline.close()).isBefore(now));
    }

    @ParameterizedTest
    @ValueSource(strings = { "00:05", "05:59", "13:59", "23:55", "23:59:59.999999999" })
    void staffAndShipperShiftsRemainValidAtClockBoundaries(String wallTime) throws Exception {
        LocalDateTime now = LocalDate.of(2026, 8, 26).atTime(LocalTime.parse(wallTime));
        Method productionHelper = WorkShiftService.class.getDeclaredMethod("isValidCheckedInShift", WorkShift.class, LocalDateTime.class);
        productionHelper.setAccessible(true);

        for (String role : List.of("STAFF", "SHIPPER")) {
            User user = new User();
            user.setRole(role);
            WorkShift shift = StaffDispatchBrowserFixtureIT.shift(user, now);
            assertEquals(true, productionHelper.invoke(null, shift, now), role + " shift at " + wallTime);
        }
    }

    private Orders order(String status, LocalDateTime timestamp) {
        Orders order = new Orders();
        order.setOrderStatus(status);
        order.setCreatedAt(timestamp);
        order.setReadyAt(timestamp);
        return order;
    }
}
