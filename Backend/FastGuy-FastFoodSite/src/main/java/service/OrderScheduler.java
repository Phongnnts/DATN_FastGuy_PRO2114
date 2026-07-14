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
    private static final NotificationService notificationService = new NotificationService();
    private static final OrderStatusHistoryService historyService = new OrderStatusHistoryService();

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
                    boolean ok = orderService.cancelOrder(order.getOrderId(), null, null, "Hết thời gian thanh toán (15 phút)", false);
                    if (ok) {
                        if (order.getUser() != null) {
                            notificationService.notifyUser(order.getUser().getUserId(),
                                    "Đơn hàng đã bị hủy",
                                    "Đơn " + order.getOrderCode() + " đã bị hủy do chưa thanh toán sau 15 phút",
                                    "ORDER_CANCELLED",
                                    "/account/orders/" + order.getOrderId());
                        }
                        notificationService.notifyRole("STAFF",
                                "Đơn hàng hết hạn thanh toán",
                                "Đơn " + order.getOrderCode() + " đã tự hủy do chưa thanh toán",
                                "ORDER_CANCELLED",
                                "/staff/orders/" + order.getOrderId());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
