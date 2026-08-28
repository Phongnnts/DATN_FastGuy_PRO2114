package service;

import dao.OrdersDAO;
import entity.Orders;
import java.time.LocalDateTime;
import java.util.List;

public class OrderExpiryService {
    private final OrdersDAO ordersDAO;
    private final OrderTransitionService transitions;

    public OrderExpiryService() {
        this(new OrdersDAO(), new OrderTransitionService());
    }

    OrderExpiryService(OrdersDAO ordersDAO, OrderTransitionService transitions) {
        this.ordersDAO = ordersDAO;
        this.transitions = transitions;
    }

    public List<Orders> findCandidates(LocalDateTime now) {
        return ordersDAO.findExpiryCandidates(now.minusMinutes(10));
    }

    public OrderTransitionService.CancellationResult cancelExpired(int orderId, LocalDateTime now) {
        return transitions.cancelIfExpired(orderId, now);
    }

    public OrderTransitionService.CancellationResult cancelAtCutoff(int orderId, LocalDateTime now) {
        return transitions.cancelAtCutoff(orderId, now);
    }

    static boolean isAtCutoff(java.time.LocalTime time) {
        return time != null && !time.isBefore(java.time.LocalTime.of(20, 45));
    }

    public void cancelCutoffCandidates(LocalDateTime now) {
        if (!isAtCutoff(now.toLocalTime())) return;
        attempt(ordersDAO.findCutoffCandidates(), order -> cancelAtCutoff(order.getOrderId(), now));
    }

    public void cancelExpiredCandidates(LocalDateTime now) {
        attempt(findCandidates(now), order -> cancelExpired(order.getOrderId(), now));
    }

    private void attempt(List<Orders> candidates, java.util.function.Consumer<Orders> cancellation) {
        for (Orders order : candidates) {
            try { cancellation.accept(order); }
            catch (RuntimeException e) { e.printStackTrace(); }
        }
    }
}
