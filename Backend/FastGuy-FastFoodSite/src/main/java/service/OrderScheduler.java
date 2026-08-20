package service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dao.OrdersDAO;
import entity.Orders;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

public class OrderScheduler {
    private static ScheduledExecutorService scheduler;
    private static final OrdersDAO ordersDAO = new OrdersDAO();
    private static final OrderService orderService = new OrderService();

    public static void start() {
        if (scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "order-scheduler");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(OrderScheduler::cancelUnpaidOrders, 1, 1, TimeUnit.MINUTES);
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
