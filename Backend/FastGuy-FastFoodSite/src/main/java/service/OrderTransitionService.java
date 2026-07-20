package service;

import entity.OrderStatusHistory;
import entity.Orders;
import entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

import java.time.LocalDateTime;
import java.util.*;

public class OrderTransitionService {
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "WAITING_STOCK_CONFIRM", Set.of("PENDING", "CANCELLED"),
            "PENDING", Set.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED", Set.of("PREPARING", "CANCELLED"),
            "PREPARING", Set.of("READY", "CANCELLED"),
            "READY", Set.of("READY", "PICKED_UP", "CANCELLED"),
            "PICKED_UP", Set.of("DELIVERED", "CANCELLED"),
            "DELIVERED", Set.of(),
            "CANCELLED", Set.of()
    );

    public boolean canTransition(String from, String to) {
        Set<String> allowed = TRANSITIONS.getOrDefault(from, Set.of());
        return allowed.contains(to);
    }

    public Set<String> getAllowedActions(String currentStatus, String role, String paymentStatus) {
        if (currentStatus == null) return Set.of();
        Set<String> next = new HashSet<>(TRANSITIONS.getOrDefault(currentStatus, Set.of()));

        if ("USER".equals(role)) {
            next.retainAll(Set.of("CANCELLED"));
            if (!"PENDING".equals(currentStatus) && !"WAITING_STOCK_CONFIRM".equals(currentStatus)) next.clear();
        } else if ("STAFF".equals(role)) {
            next.remove("PICKED_UP");
            next.remove("DELIVERED");
            if ("PENDING".equals(currentStatus) && "UNPAID".equals(paymentStatus)) {
                next.remove("CONFIRMED");
            }
        } else if ("SHIPPER".equals(role)) {
            next.retainAll(Set.of("PICKED_UP", "DELIVERED", "CANCELLED"));
        } else {
            next.clear();
        }
        return next;
    }

    public Map<String, Object> toAllowedActionsMap(Orders order, String role) {
        Map<String, Object> result = new HashMap<>();
        result.put("currentStatus", order.getOrderStatus());
        result.put("allowedActions", getAllowedActions(order.getOrderStatus(), role, order.getPaymentStatus()));
        return result;
    }

    public boolean transition(int orderId, String toStatus, String actorRole, int actorUserId, String note) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null) { em.getTransaction().rollback(); return false; }

            String from = order.getOrderStatus();
            if (!canTransition(from, toStatus)) { em.getTransaction().rollback(); return false; }

            if ("CONFIRMED".equals(toStatus)) order.setConfirmedAt(LocalDateTime.now());
            else if ("READY".equals(toStatus)) order.setReadyAt(LocalDateTime.now());
            else if ("PICKED_UP".equals(toStatus)) order.setPickedUpAt(LocalDateTime.now());
            else if ("DELIVERED".equals(toStatus)) order.setDeliveredAt(LocalDateTime.now());
            else if ("CANCELLED".equals(toStatus)) {
                order.setCancelledAt(LocalDateTime.now());
                order.setCancelledBy(actorRole);
                if (note != null) order.setFailureReason(note);
            }

            order.setOrderStatus(toStatus);
            User actor = em.find(User.class, actorUserId);
            if (actor != null) {
                if ("STAFF".equals(actorRole)) order.setStaff(actor);
                else if ("SHIPPER".equals(actorRole)) order.setShipper(actor);
            }

            em.persist(new OrderStatusHistory(orderId, actorUserId, actorRole, from, toStatus, note, LocalDateTime.now()));
            em.getTransaction().commit();
            return true;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean confirmStockAndTransition(int orderId, int staffId, OrderService orderService, PayOSPaymentService payOSPaymentService) {
        boolean confirmed = orderService.confirmStock(orderId, staffId);
        if (!confirmed) return false;
        if (payOSPaymentService.isConfigured()) {
            if (payOSPaymentService.createPaymentLink(orderId) != null) return true;
            orderService.revertStockConfirmation(orderId, staffId, "Không thể tạo link PayOS");
            return false;
        }
        return true;
    }
}
