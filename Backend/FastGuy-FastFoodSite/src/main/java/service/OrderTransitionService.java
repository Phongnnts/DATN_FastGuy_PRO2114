package service;

import entity.Coupon;
import entity.CouponRedemption;
import entity.OrderItem;
import entity.OrderStatusHistory;
import entity.Orders;
import entity.ProductVariant;
import entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

import java.time.LocalDateTime;
import java.util.*;

public class OrderTransitionService {
    private final InventoryReservationService inventoryReservationService = new InventoryReservationService();
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "PENDING", Set.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED", Set.of("PREPARING", "CANCELLED"),
            "PREPARING", Set.of("READY", "CANCELLED"),
            "READY", Set.of("ASSIGNED", "CANCELLED"),
            "ASSIGNED", Set.of("PICKED_UP", "CANCELLED"),
            "PICKED_UP", Set.of("DELIVERED", "CANCELLED"),
            "DELIVERED", Set.of(),
            "CANCELLED", Set.of()
    );

    public boolean canTransition(String from, String to) {
        Set<String> allowed = TRANSITIONS.getOrDefault(from, Set.of());
        return allowed.contains(to);
    }

    static boolean canDeliver(String paymentMethod, String paymentStatus) {
        return "PAID".equals(paymentStatus);
    }

    static boolean canCancel(Orders order, Integer expectedUserId, String expectedPaymentStatus, boolean pendingOnly,
                             String actorRole) {
        if (order == null || !isActorRole(actorRole)) return false;
        if (expectedUserId != null && (order.getUser() == null || order.getUser().getUserId() != expectedUserId)) return false;
        if (expectedPaymentStatus != null && !expectedPaymentStatus.equals(order.getPaymentStatus())) return false;
        String status = order.getOrderStatus();
        if ("CANCELLED".equals(status) || "DELIVERED".equals(status)) return false;
        if (pendingOnly && !"PENDING".equals(status)) return false;
        return !("READY".equals(status) && ("USER".equals(actorRole) || "CUSTOMER".equals(actorRole)));
    }

    public CancellationResult cancel(int orderId, Integer expectedUserId, String expectedPaymentStatus,
                                     boolean pendingOnly, String actorRole, Integer actorUserId, String reason) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (!canCancel(order, expectedUserId, expectedPaymentStatus, pendingOnly, actorRole)) {
                em.getTransaction().rollback();
                return null;
            }
            String from = order.getOrderStatus();
            String orderCode = order.getOrderCode();
            Integer orderUserId = order.getUser() == null ? null : order.getUser().getUserId();
            if (!inventoryReservationService.cancel(em, order)) restoreStock(em, orderId);
            releaseCoupon(em, orderId);
            order.setOrderStatus("CANCELLED");
            order.setCancelledAt(LocalDateTime.now());
            order.setCancelledBy("USER".equals(actorRole) ? "CUSTOMER" : actorRole);
            if ("PAID".equals(order.getPaymentStatus())) order.setRefundStatus("PENDING");
            if (reason != null && !reason.isBlank()) order.setFailureReason(reason);
            User actor = actorUserId == null ? null : em.find(User.class, actorUserId);
            if (actor != null && ("STAFF".equals(actorRole) || "ADMIN".equals(actorRole))) order.setStaff(actor);
            em.persist(new OrderStatusHistory(orderId, actorUserId, actorRole, from, "CANCELLED",
                    reason != null ? reason : "Hủy đơn", LocalDateTime.now()));
            em.getTransaction().commit();
            return new CancellationResult(orderCode, orderUserId);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public record CancellationResult(String orderCode, Integer orderUserId) {}

    public static boolean canUseGenericTransition(String toStatus) {
        return !Set.of("ASSIGNED", "CANCELLED").contains(toStatus);
    }

    public Set<String> getAllowedActions(String currentStatus, String role, String paymentStatus) {
        if (currentStatus == null) return Set.of();
        Set<String> next = new HashSet<>(TRANSITIONS.getOrDefault(currentStatus, Set.of()));

        if ("USER".equals(role)) {
            next.retainAll(Set.of("CANCELLED"));
            if (!"PENDING".equals(currentStatus)) next.clear();
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
        if (!canUseGenericTransition(toStatus)) return false;
        return transition(orderId, toStatus, actorRole, actorUserId, note, null, null);
    }

    public boolean transition(int orderId, String toStatus, String actorRole, Integer actorUserId, String note,
                              Integer assignedShipperId, java.math.BigDecimal collectedAmount) {
        if (!isCanonicalStatus(toStatus) || !isActorRole(actorRole)) return false;
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null) { em.getTransaction().rollback(); return false; }

            String from = order.getOrderStatus();
            if (!canTransition(from, toStatus)) { em.getTransaction().rollback(); return false; }
            if ("SHIPPER".equals(actorRole) && (order.getShipper() == null || actorUserId == null
                    || order.getShipper().getUserId() != actorUserId || !requireCheckedInShipper(em, actorUserId))) { em.getTransaction().rollback(); return false; }
            if ("ASSIGNED".equals(toStatus)) {
                User shipper = assignedShipperId == null ? null : em.find(User.class, assignedShipperId);
                Long activeShifts = shipper == null ? 0L : em.createQuery("SELECT COUNT(ws) FROM WorkShift ws WHERE ws.user.userId = :shipperId AND ws.user.status = 'ACTIVE' AND ws.status = 'CHECKED_IN' AND ws.checkInAt IS NOT NULL AND ws.checkOutAt IS NULL", Long.class)
                        .setParameter("shipperId", assignedShipperId).getSingleResult();
                if (shipper == null || !"SHIPPER".equals(shipper.getRole()) || activeShifts == 0 || order.getShipper() != null) { em.getTransaction().rollback(); return false; }
                order.setShipper(shipper);
                order.setAssignedAt(LocalDateTime.now());
            }
            if ("CONFIRMED".equals(toStatus) && "BANK_TRANSFER".equals(order.getPaymentMethod())
                    && !"PAID".equals(order.getPaymentStatus())) { em.getTransaction().rollback(); return false; }
            if ("PREPARING".equals(toStatus) && !inventoryReservationService.transition(em, order, "CONSUMED")) {
                em.getTransaction().rollback(); return false;
            }
            if ("DELIVERED".equals(toStatus) && "COD".equals(order.getPaymentMethod())) {
                if (collectedAmount == null || order.getFinalAmount() == null || collectedAmount.compareTo(order.getFinalAmount()) != 0) {
                    em.getTransaction().rollback(); return false;
                }
                order.setCodCollectedAmount(collectedAmount);
                order.setCodCollectedAt(LocalDateTime.now());
                order.setPaymentStatus("PAID");
                order.setPaidAt(LocalDateTime.now());
            }
            if ("DELIVERED".equals(toStatus) && !canDeliver(order.getPaymentMethod(), order.getPaymentStatus())) {
                em.getTransaction().rollback(); return false;
            }

            if ("CONFIRMED".equals(toStatus)) order.setConfirmedAt(LocalDateTime.now());
            else if ("READY".equals(toStatus)) order.setReadyAt(LocalDateTime.now());
            else if ("PICKED_UP".equals(toStatus)) order.setPickedUpAt(LocalDateTime.now());
            else if ("DELIVERED".equals(toStatus)) order.setDeliveredAt(LocalDateTime.now());
            else if ("CANCELLED".equals(toStatus)) {
                inventoryReservationService.cancel(em, order);
                releaseCoupon(em, orderId);
                order.setCancelledAt(LocalDateTime.now());
                order.setCancelledBy("USER".equals(actorRole) ? "CUSTOMER" : actorRole);
                if ("PAID".equals(order.getPaymentStatus())) order.setRefundStatus("PENDING");
                if (note != null && !note.isBlank()) order.setFailureReason(note);
            }

            order.setOrderStatus(toStatus);
            User actor = actorUserId == null ? null : em.find(User.class, actorUserId);
            if (actor != null) {
                if ("STAFF".equals(actorRole)) order.setStaff(actor);
                else if ("SHIPPER".equals(actorRole)) order.setShipper(actor);
            }

            em.persist(new OrderStatusHistory(orderId, actorUserId, actorRole, from, toStatus, note, LocalDateTime.now()));
            if ("DELIVERED".equals(toStatus)) new LoyaltyService().awardForDelivery(em, order);
            em.getTransaction().commit();
            return true;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    private boolean requireCheckedInShipper(EntityManager em, int shipperId) {
        return em.createQuery("SELECT COUNT(ws) FROM WorkShift ws WHERE ws.user.userId = :shipperId AND ws.user.role = 'SHIPPER' AND ws.user.status = 'ACTIVE' AND ws.status = 'CHECKED_IN' AND ws.checkInAt IS NOT NULL AND ws.checkOutAt IS NULL", Long.class)
                .setParameter("shipperId", shipperId)
                .getSingleResult() > 0;
    }

    static boolean isCanonicalStatus(String status) {
        return TRANSITIONS.containsKey(status);
    }

    static boolean isActorRole(String role) {
        return Set.of("USER", "CUSTOMER", "STAFF", "ADMIN", "SHIPPER", "SYSTEM").contains(role);
    }

    private void restoreStock(EntityManager em, int orderId) {
        List<OrderItem> items = em.createQuery("SELECT oi FROM OrderItem oi WHERE oi.order.orderId = :orderId", OrderItem.class)
                .setParameter("orderId", orderId).getResultList();
        for (OrderItem item : items) {
            if (item.getVariant() == null) continue;
            ProductVariant variant = em.find(ProductVariant.class, item.getVariant().getVariantId(), LockModeType.PESSIMISTIC_WRITE);
            if (variant != null && variant.getQuantityAvailable() != null) {
                variant.setQuantityAvailable(variant.getQuantityAvailable() + item.getQuantity());
            }
        }
    }

    private void releaseCoupon(EntityManager em, int orderId) {
        CouponRedemption redemption = em.createQuery("SELECT cr FROM CouponRedemption cr WHERE cr.orderId = :orderId", CouponRedemption.class)
                .setParameter("orderId", orderId).setMaxResults(1).getResultStream().findFirst().orElse(null);
        if (redemption == null || redemption.getUsedAt() == null) return;
        Coupon coupon = em.find(Coupon.class, redemption.getCouponId(), LockModeType.PESSIMISTIC_WRITE);
        if (coupon != null && coupon.getUsedCount() > 0) coupon.setUsedCount(coupon.getUsedCount() - 1);
        redemption.setUsedAt(null);
        redemption.setOrderId(null);
    }

}
