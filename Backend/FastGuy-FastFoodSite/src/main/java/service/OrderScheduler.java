package service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import dao.OrdersDAO;
import entity.Orders;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

public class OrderScheduler {
    private static ScheduledExecutorService scheduler;
    private static final OrdersDAO ordersDAO = new OrdersDAO();
    private static final OrderService orderService = new OrderService();
    private static final OrderScheduler INSTANCE = new OrderScheduler(ordersDAO, new StoreConfigService(),
            new OrderTransitionService(), LocalDateTime::now);
    private final OrdersDAO closingOrdersDAO;
    private final StoreConfigService configService;
    private final OrderTransitionService transitionService;
    private final Supplier<LocalDateTime> clock;

    public OrderScheduler() {
        this(ordersDAO, new StoreConfigService(), new OrderTransitionService(), LocalDateTime::now);
    }

    OrderScheduler(OrdersDAO closingOrdersDAO, StoreConfigService configService,
                   OrderTransitionService transitionService, Supplier<LocalDateTime> clock) {
        this.closingOrdersDAO = closingOrdersDAO;
        this.configService = configService;
        this.transitionService = transitionService;
        this.clock = clock;
    }

    public static void start() {
        if (scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "order-scheduler");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(OrderScheduler::runCancellationTick, 1, 1, TimeUnit.MINUTES);
    }

    public static void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private static void cancelUnpaidOrders() {
        try {
            LocalDateTime now = LocalDateTime.now();
            cancelOrders(findStaleOrders("BANK_TRANSFER", now.minusMinutes(15)), "Hết thời gian thanh toán (15 phút)");
            cancelOrders(findStaleOrders("COD", now.minusHours(3)).stream()
                    .filter(order -> isStaleCodPending(order, now))
                    .toList(), "Quá 3 giờ chưa được xác nhận");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runCancellationTick() {
        cancelUnpaidOrders();
        INSTANCE.cancelReadyOrdersAfterClosing();
    }

    public void cancelReadyOrdersAfterClosing() {
        LocalDateTime now = clock.get();
        Optional<BusinessHours> hours = parseBusinessHours(configService.getAll());
        if (hours.isEmpty() || hours.get().open().equals(hours.get().close())) return;
        for (Orders order : closingCandidates(closingOrdersDAO.findReadyWithoutShipperForClosing(), now,
                hours.get().open(), hours.get().close())) {
            try {
                transitionService.cancelReadyIfUnassignedAfterClosing(order.getOrderId(), now,
                        hours.get().open(), hours.get().close());
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    static List<Orders> closingCandidates(List<Orders> orders, LocalDateTime now, LocalTime open, LocalTime close) {
        return orders.stream()
                .filter(order -> OrderTransitionService.canAutoCancelAfterClosing(order, now, open, close)).toList();
    }

    static Optional<BusinessHours> parseBusinessHours(Map<String, String> config) {
        try {
            String open = config == null ? null : config.get(StoreConfigService.OPEN_TIME);
            String close = config == null ? null : config.get(StoreConfigService.CLOSE_TIME);
            if (open == null || close == null) return Optional.empty();
            return Optional.of(new BusinessHours(LocalTime.parse(open), LocalTime.parse(close)));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    record BusinessHours(LocalTime open, LocalTime close) {}

    private static List<Orders> findStaleOrders(String paymentMethod, LocalDateTime cutoff) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT o FROM Orders o WHERE o.paymentMethod = :paymentMethod AND o.paymentStatus = 'UNPAID' " +
                                    "AND o.orderStatus = 'PENDING' AND o.createdAt < :cutoff",
                            Orders.class)
                    .setParameter("paymentMethod", paymentMethod)
                    .setParameter("cutoff", cutoff)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    private static void cancelOrders(List<Orders> orders, String reason) {
        for (Orders order : orders) {
            try {
                orderService.cancelOrder(order.getOrderId(), null, null, reason, true, "UNPAID");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static boolean isStaleCodPending(Orders order, LocalDateTime now) {
        return order != null
                && "PENDING".equals(order.getOrderStatus())
                && "COD".equals(order.getPaymentMethod())
                && "UNPAID".equals(order.getPaymentStatus())
                && order.getCreatedAt() != null
                && order.getCreatedAt().isBefore(now.minusHours(3));
    }
}
