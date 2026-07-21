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
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
            EntityManager em = DatabaseUtil.getEntityManager();
            List<Orders> staleOrders;
            try {
                staleOrders = em.createQuery(
                        "SELECT o FROM Orders o WHERE o.paymentMethod = 'BANK_TRANSFER' AND o.paymentStatus = 'UNPAID' " +
                                "AND (o.orderStatus = 'WAITING_STOCK_CONFIRM' OR o.orderStatus = 'PENDING') " +
                                "AND o.createdAt < :cutoff",
                        Orders.class)
                        .setParameter("cutoff", cutoff)
                        .getResultList();
            } finally {
                em.close();
            }

            for (Orders order : staleOrders) {
                try {
                    orderService.cancelOrder(order.getOrderId(), null, null, "Hết thời gian thanh toán (15 phút)", false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
