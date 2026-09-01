package service;

import entity.Coupon;
import entity.CouponRedemption;
import entity.OrderStatusHistory;
import entity.Orders;
import entity.User;
import entity.WorkShift;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Predicate;

public class OrderTransitionService {
    private final ActivityLogService activityLogService = new ActivityLogService();
    private final InventoryReservationService inventoryReservationService = new InventoryReservationService();
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "PENDING", Set.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED", Set.of("PREPARING", "CANCELLED"),
            "PREPARING", Set.of("READY", "CANCELLED"),
            "READY", Set.of("ASSIGNED", "CANCELLED"),
            "ASSIGNED", Set.of("PICKED_UP"),
            "PICKED_UP", Set.of("DELIVERED", "DELIVERY_FAILED"),
            "DELIVERY_FAILED", Set.of("PICKED_UP", "RETURNED_TO_STORE"),
            "RETURNED_TO_STORE", Set.of(),
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
        if (Set.of("ASSIGNED", "PICKED_UP", "CANCELLED", "DELIVERED", "RETURNED_TO_STORE").contains(status)) return false;
        if (pendingOnly && !"PENDING".equals(status)) return false;
        return !("READY".equals(status) && ("USER".equals(actorRole) || "CUSTOMER".equals(actorRole)));
    }

    public CancellationResult cancel(int orderId, Integer expectedUserId, String expectedPaymentStatus,
                                     boolean pendingOnly, String actorRole, Integer actorUserId, String reason) {
        return cancel(orderId, order -> canCancel(order, expectedUserId, expectedPaymentStatus, pendingOnly, actorRole),
                actorRole, actorUserId, reason, WorkShiftService.businessNow());
    }

    public CancellationResult cancelReadyIfUnassignedAfterClosing(int orderId, LocalDateTime now,
                                                                   LocalTime open, LocalTime close) {
        return cancel(orderId, order -> canAutoCancelAfterClosing(order, now, open, close), "SYSTEM", null,
                "Quá giờ đóng cửa chưa được điều phối", now);
    }

    static boolean canAutoCancelAfterClosing(Orders order, LocalDateTime now, LocalTime open, LocalTime close) {
        if (order == null || now == null || !"READY".equals(order.getOrderStatus()) || order.getShipper() != null) {
            return false;
        }
        LocalDateTime closing = new DispatchOrderPolicy().closingAt(order.getCreatedAt(), open, close);
        return closing != null && !closing.isAfter(now);
    }

    private CancellationResult cancel(int orderId, Predicate<Orders> precondition, String actorRole,
                                      Integer actorUserId, String reason, LocalDateTime now) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (!precondition.test(order)) {
                em.getTransaction().rollback();
                return null;
            }
            String from = order.getOrderStatus();
            String orderCode = order.getOrderCode();
            Integer orderUserId = order.getUser() == null ? null : order.getUser().getUserId();
            if (!applyCancellation(em, order, actorRole, actorUserId, reason, now)) {
                em.getTransaction().rollback();
                return null;
            }
            if(actorUserId!=null)activityLogService.append(em,actorUserId,"ORDER_CANCELLED","ORDER",orderId,Map.of("orderCode",orderCode,"reason",reason==null?"":reason));
            em.getTransaction().commit();
            return new CancellationResult(orderCode, orderUserId);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public CancellationResult cancelIfExpired(int orderId, LocalDateTime now) {
        return cancel(orderId, order -> OrderExpiryPolicy.isExpired(order, now), "SYSTEM", null, "Quá thời gian xử lý", now);
    }

    public CancellationResult cancelAtCutoff(int orderId, LocalDateTime now) {
        return cancel(orderId, OrderExpiryPolicy::isCutoffCancellationCandidate, "SYSTEM", null, "Hết giờ nhận đơn", now);
    }

    public record CancellationResult(String orderCode, Integer orderUserId) {}

    public static boolean canUseGenericTransition(String toStatus) {
        return !Set.of("ASSIGNED", "PICKED_UP", "RETURNED_TO_STORE").contains(toStatus);
    }

    static boolean canUseDetailedTransition(String toStatus) {
        return !"RETURNED_TO_STORE".equals(toStatus);
    }

    static boolean canAssignOrder(String actorRole, Integer actorUserId, String expectedStatus, boolean checkedInStaff) {
        return "STAFF".equals(actorRole) && actorUserId != null && "READY".equals(expectedStatus) && checkedInStaff;
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
            next.retainAll(Set.of("PICKED_UP", "DELIVERED", "DELIVERY_FAILED", "CANCELLED"));
        } else if ("ADMIN".equals(role)) {
            next.removeIf(status -> !"CANCELLED".equals(status) && !canUseGenericTransition(status));
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
        if (!canUseGenericTransition(toStatus)) return false;
        return transition(orderId, toStatus, actorRole, actorUserId, note, assignedShipperId, collectedAmount, null) == MutationResult.SUCCESS;
    }

    public MutationResult transition(int orderId, String toStatus, String actorRole, Integer actorUserId, String note,
                                     Integer assignedShipperId, java.math.BigDecimal collectedAmount, String expectedStatus) {
        if (!canUseDetailedTransition(toStatus) || !isCanonicalStatus(toStatus) || !isActorRole(actorRole)) return MutationResult.INVALID;
        if ("ASSIGNED".equals(toStatus) && !canAssignOrder(actorRole, actorUserId, expectedStatus, true)) return MutationResult.INVALID;
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            WorkShift currentStaffShift = "STAFF".equals(actorRole) && actorUserId != null ? currentActiveShift(em, em.find(User.class, actorUserId), "STAFF") : null;
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);

            if (order == null) { em.getTransaction().rollback(); return MutationResult.NOT_FOUND; }
            if (expectedStatus != null && !matchesExpectedStatus(order, expectedStatus)) { em.getTransaction().rollback(); return MutationResult.CONFLICT; }

            String from = order.getOrderStatus();
            if (!canTransition(from, toStatus)) { em.getTransaction().rollback(); return MutationResult.INVALID; }
            if ("STAFF".equals(actorRole) && !canStaffMutateOwnedOrder(order, currentStaffShift, toStatus)) { em.getTransaction().rollback(); return MutationResult.CONFLICT; }
            if ("SHIPPER".equals(actorRole) && (order.getShipper() == null || actorUserId == null
                    || order.getShipper().getUserId() != actorUserId || !requireCheckedInShipper(em, actorUserId))) { em.getTransaction().rollback(); return MutationResult.INVALID; }
            if ("ASSIGNED".equals(toStatus)) {
                if (!canAssignOrder(actorRole, actorUserId, expectedStatus, requireCheckedInStaff(em, actorUserId))) {
                    em.getTransaction().rollback();
                    return MutationResult.INVALID;
                }
                User shipper = assignedShipperId == null ? null : em.find(User.class, assignedShipperId);
                if (shipper == null || !"SHIPPER".equals(shipper.getRole()) || order.getShipper() != null) {
                    em.getTransaction().rollback();
                    return MutationResult.INVALID;
                }
                if (currentActiveShift(em, shipper, "SHIPPER") == null) {
                    em.getTransaction().rollback();
                    return MutationResult.UNPROCESSABLE;
                }
                order.setShipper(shipper);
                order.setAssignedAt(LocalDateTime.now());
            }
            if ("CONFIRMED".equals(toStatus) && "BANK_TRANSFER".equals(order.getPaymentMethod())
                    && !"PAID".equals(order.getPaymentStatus())) { em.getTransaction().rollback(); return MutationResult.INVALID; }
            if ("PREPARING".equals(toStatus) && !inventoryReservationService.transition(em, order, "CONSUMED")) {
                em.getTransaction().rollback(); return MutationResult.INVALID;
            }
            if ("DELIVERED".equals(toStatus) && "COD".equals(order.getPaymentMethod())) {
                if (collectedAmount == null || order.getFinalAmount() == null || collectedAmount.compareTo(order.getFinalAmount()) != 0) {
                    em.getTransaction().rollback(); return MutationResult.INVALID;
                }
                order.setCodCollectedAmount(collectedAmount);
                order.setCodCollectedAt(LocalDateTime.now());
                order.setPaymentStatus("PAID");
                order.setPaidAt(LocalDateTime.now());
            }
            if ("DELIVERED".equals(toStatus) && !canDeliver(order.getPaymentMethod(), order.getPaymentStatus())) {
                em.getTransaction().rollback(); return MutationResult.INVALID;
            }

            if ("CONFIRMED".equals(toStatus)) order.setConfirmedAt(LocalDateTime.now());
            else if ("READY".equals(toStatus)) order.setReadyAt(LocalDateTime.now());
            else if ("PICKED_UP".equals(toStatus)) order.setPickedUpAt(LocalDateTime.now());
            else if ("DELIVERED".equals(toStatus)) order.setDeliveredAt(LocalDateTime.now());
            else if ("CANCELLED".equals(toStatus)) {
                if (!applyCancellation(em, order, actorRole, actorUserId, note, LocalDateTime.now())) {
                    em.getTransaction().rollback(); return MutationResult.INVALID;
                }
            }

            if ("CONFIRMED".equals(toStatus) && !canActorConfirm(actorRole, currentStaffShift)) { em.getTransaction().rollback(); return MutationResult.INVALID; }
            applyActorOwnership(order, toStatus, actorRole, currentStaffShift);
            if (!"CANCELLED".equals(toStatus)) applyStatus(order, toStatus, WorkShiftService.businessNow());
            User actor = actorUserId == null ? null : em.find(User.class, actorUserId);
            if (actor != null) {
                if ("STAFF".equals(actorRole)) order.setStaff(actor);
                else if ("SHIPPER".equals(actorRole)) order.setShipper(actor);
            }

            if (!"CANCELLED".equals(toStatus)) {
                em.persist(new OrderStatusHistory(orderId, actorUserId, actorRole, from, toStatus, note, LocalDateTime.now()));
            }
            if ("DELIVERED".equals(toStatus)) new LoyaltyService().awardForDelivery(em, order);
            em.getTransaction().commit();
            return MutationResult.SUCCESS;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public MutationResult appendAdminNote(int orderId, int adminId, String expectedStatus, String note) {
        if (note == null || note.isBlank()) return MutationResult.INVALID;
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null) { em.getTransaction().rollback(); return MutationResult.NOT_FOUND; }
            if (!matchesExpectedStatus(order, expectedStatus)) { em.getTransaction().rollback(); return MutationResult.CONFLICT; }
            String existing = order.getInternalNote();
            String normalized = note.trim();
            order.setInternalNote(existing != null && !existing.isBlank() ? existing + "\n---\n[Admin] " + normalized : "[Admin] " + normalized);
            em.persist(new OrderStatusHistory(orderId, adminId, "ADMIN", order.getOrderStatus(), order.getOrderStatus(), normalized, LocalDateTime.now()));
            em.getTransaction().commit();
            return MutationResult.SUCCESS;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally { em.close(); }
    }

    public MutationResult reportDeliveryFailure(int orderId, int shipperId, String expectedStatus, String reasonCode, String note) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null) { em.getTransaction().rollback(); return MutationResult.INVALID; }
            if (!matchesExpectedStatus(order, expectedStatus)) { em.getTransaction().rollback(); return MutationResult.CONFLICT; }
            if (!"PICKED_UP".equals(order.getOrderStatus()) || order.getShipper() == null
                    || order.getShipper().getUserId() != shipperId || !requireCheckedInShipper(em, shipperId)
                    || !DeliveryFailurePolicy.isValidFailure(reasonCode, note)) { em.getTransaction().rollback(); return MutationResult.INVALID; }
            if (order.getDeliveryAttemptCount() >= order.getDeliveryAttemptLimit()) { em.getTransaction().rollback(); return MutationResult.UNPROCESSABLE; }
            order.setDeliveryAttemptCount(order.getDeliveryAttemptCount() + 1);
            applyStatus(order, "DELIVERY_FAILED", WorkShiftService.businessNow());
            order.setDeliveryFailureCode(reasonCode);
            order.setFailureReason(note.trim());
            order.setDeliveryFailedAt(LocalDateTime.now());
            order.setRetryScheduledAt(null);
            em.persist(new OrderStatusHistory(orderId, shipperId, "SHIPPER", "PICKED_UP", "DELIVERY_FAILED", note.trim(), LocalDateTime.now()));
            em.getTransaction().commit();
            return MutationResult.SUCCESS;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally { em.close(); }
    }

    public MutationResult retryDelivery(int orderId, int staffId, String expectedStatus, int shipperId, String retryMode, LocalDateTime scheduledAt, String note) {
        String normalizedNote = DeliveryFailurePolicy.normalizeNote(note);
        if (normalizedNote == null) return MutationResult.INVALID;
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            WorkShift staffShift = currentActiveShift(em, em.find(User.class, staffId), "STAFF");
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null) { em.getTransaction().rollback(); return MutationResult.INVALID; }
            if (!matchesExpectedStatus(order, expectedStatus)) { em.getTransaction().rollback(); return MutationResult.CONFLICT; }
            User shipper = em.find(User.class, shipperId);
            if (!"DELIVERY_FAILED".equals(order.getOrderStatus()) || !isOwnedBy(order, staffShift)
                    || shipper == null || !"SHIPPER".equals(shipper.getRole()) || !requireCheckedInShipper(em, shipperId)) { em.getTransaction().rollback(); return MutationResult.INVALID; }
            if (!DeliveryFailurePolicy.canRetry(order.getDeliveryAttemptCount(), order.getDeliveryAttemptLimit())
                    || !validRetrySchedule(em, retryMode, scheduledAt)) { em.getTransaction().rollback(); return MutationResult.UNPROCESSABLE; }
            assignRetryShipper(order, shipper, LocalDateTime.now());
            order.setStaff(em.find(User.class, staffId));
            order.setRetryScheduledAt(scheduledAt);
            String to = "IMMEDIATE".equals(retryMode) ? "PICKED_UP" : "DELIVERY_FAILED";
            applyStatus(order, to, WorkShiftService.businessNow());
            clearOwnershipAfterRecovery(order, to);
            if ("PICKED_UP".equals(to)) order.setPickedUpAt(LocalDateTime.now());
            em.persist(new OrderStatusHistory(orderId, staffId, "STAFF", "DELIVERY_FAILED", to, normalizedNote, LocalDateTime.now()));
            em.getTransaction().commit();
            return MutationResult.SUCCESS;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally { em.close(); }
    }

    public MutationResult startScheduledRetry(int orderId, int staffId, String expectedStatus) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            WorkShift staffShift = currentActiveShift(em, em.find(User.class, staffId), "STAFF");
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null) { em.getTransaction().rollback(); return MutationResult.INVALID; }
            if (!matchesExpectedStatus(order, expectedStatus)) { em.getTransaction().rollback(); return MutationResult.CONFLICT; }
            WorkShift shipperShift = currentActiveShift(em, order.getShipper(), "SHIPPER");
            if (!"DELIVERY_FAILED".equals(order.getOrderStatus()) || !isOwnedBy(order, staffShift)
                    || !canStartScheduledRetry(order, shipperShift, WorkShiftService.businessNow())) { em.getTransaction().rollback(); return MutationResult.INVALID; }
            if (order.getRetryScheduledAt() == null || order.getRetryScheduledAt().isAfter(WorkShiftService.businessNow())) { em.getTransaction().rollback(); return MutationResult.UNPROCESSABLE; }
            applyStatus(order, "PICKED_UP", WorkShiftService.businessNow());
            clearOwnershipAfterRecovery(order, "PICKED_UP");
            order.setPickedUpAt(LocalDateTime.now());
            order.setRetryScheduledAt(null);
            em.persist(new OrderStatusHistory(orderId, staffId, "STAFF", "DELIVERY_FAILED", "PICKED_UP", "Bắt đầu giao lại theo lịch", LocalDateTime.now()));
            em.getTransaction().commit();
            return MutationResult.SUCCESS;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally { em.close(); }
    }

    public MutationResult returnToStore(int orderId, int staffId, String expectedStatus, String note) {
        String normalizedNote = DeliveryFailurePolicy.normalizeNote(note);
        if (normalizedNote == null) return MutationResult.INVALID;
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            WorkShift staffShift = currentActiveShift(em, em.find(User.class, staffId), "STAFF");
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null) { em.getTransaction().rollback(); return MutationResult.INVALID; }
            if (!matchesExpectedStatus(order, expectedStatus)) { em.getTransaction().rollback(); return MutationResult.CONFLICT; }
            if (!"DELIVERY_FAILED".equals(order.getOrderStatus()) || !isOwnedBy(order, staffShift)) { em.getTransaction().rollback(); return MutationResult.INVALID; }
            inventoryReservationService.cancel(em, order);
            releaseCoupon(em, orderId);
            applyStatus(order, "RETURNED_TO_STORE", WorkShiftService.businessNow());
            clearOwnershipAfterRecovery(order, "RETURNED_TO_STORE");
            order.setReturnedToStoreAt(LocalDateTime.now());
            order.setRetryScheduledAt(null);
            order.setStaff(em.find(User.class, staffId));
            if ("PAID".equals(order.getPaymentStatus())) order.setRefundStatus("PENDING");
            em.persist(new OrderStatusHistory(orderId, staffId, "STAFF", "DELIVERY_FAILED", "RETURNED_TO_STORE", normalizedNote, LocalDateTime.now()));
            em.getTransaction().commit();
            return MutationResult.SUCCESS;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally { em.close(); }
    }

    public MutationResult overrideDeliveryAttemptLimit(int orderId, int adminId, String expectedStatus, String note) {
        String normalizedNote = DeliveryFailurePolicy.normalizeNote(note);
        if (normalizedNote == null) return MutationResult.INVALID;
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null) { em.getTransaction().rollback(); return MutationResult.INVALID; }
            if (!matchesExpectedStatus(order, expectedStatus)) { em.getTransaction().rollback(); return MutationResult.CONFLICT; }
            User admin = em.find(User.class, adminId);
            if (admin == null || !"ADMIN".equals(admin.getRole()) || !"ACTIVE".equals(admin.getStatus())) { em.getTransaction().rollback(); return MutationResult.INVALID; }
            order.setDeliveryAttemptLimit(order.getDeliveryAttemptLimit() + 1);
            String status = order.getOrderStatus();
            em.persist(new OrderStatusHistory(orderId, adminId, "ADMIN", status, status, normalizedNote, LocalDateTime.now()));
            activityLogService.append(em,adminId,"DELIVERY_ATTEMPT_OVERRIDDEN","DELIVERY_ATTEMPT",orderId,Map.of("deliveryAttemptLimit",order.getDeliveryAttemptLimit()));
            em.getTransaction().commit();
            return MutationResult.SUCCESS;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally { em.close(); }
    }

    static void applyStatus(Orders order, String status, LocalDateTime now) {
        if (!Objects.equals(order.getOrderStatus(), status)) {
            order.setOrderStatus(status);
            order.setStatusEnteredAt(now);
        }
    }

    static void assignRetryShipper(Orders order, User shipper, LocalDateTime assignedAt) {
        order.setShipper(shipper);
        order.setAssignedAt(assignedAt);
    }

    static void clearOwnershipAfterRecovery(Orders order, String toStatus) {
        if ("PICKED_UP".equals(toStatus) || "RETURNED_TO_STORE".equals(toStatus)) order.setStaffShift(null);
    }

    static boolean isOwnedBy(Orders order, WorkShift shift) {
        return order != null && shift != null && order.getStaffShift() != null && order.getStaffShift().getShiftId() == shift.getShiftId();
    }

    static boolean canStaffMutateOwnedOrder(Orders order, WorkShift currentShift, String toStatus) {
        return order != null && currentShift != null && ("PENDING".equals(order.getOrderStatus()) && "CONFIRMED".equals(toStatus) || isOwnedBy(order, currentShift));
    }

    static boolean canActorConfirm(String actorRole, WorkShift currentStaffShift) {
        return !"STAFF".equals(actorRole) || currentStaffShift != null;
    }

    static void applyActorOwnership(Orders order, String toStatus, String actorRole, WorkShift currentStaffShift) {
        if ("STAFF".equals(actorRole)) applyStaffShiftOwnership(order, toStatus, currentStaffShift);
    }

    static void applyStaffShiftOwnership(Orders order, String toStatus, WorkShift currentShift) {
        if ("CONFIRMED".equals(toStatus)) order.setStaffShift(currentShift);
        else if (Set.of("ASSIGNED", "PICKED_UP", "DELIVERED", "CANCELLED", "RETURNED_TO_STORE", "DELIVERY_FAILED").contains(toStatus)) order.setStaffShift(null);
    }

    static boolean canClaimHandover(Orders order, WorkShift currentShift, String expectedStatus, Integer expectedOwnerShiftId) {
        if (order == null || currentShift == null || !Set.of("CONFIRMED", "PREPARING", "READY", "DELIVERY_FAILED").contains(order.getOrderStatus())
                || !Objects.equals(order.getOrderStatus(), expectedStatus)) return false;
        Integer ownerId = order.getStaffShift() == null ? null : order.getStaffShift().getShiftId();
        return Objects.equals(ownerId, expectedOwnerShiftId) && !Objects.equals(ownerId, currentShift.getShiftId());
    }

    public MutationResult claimHandover(int orderId, int staffId, String expectedStatus, Integer expectedOwnerShiftId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            User staff = em.find(User.class, staffId);
            WorkShift receiving = currentActiveShift(em, staff, "STAFF");
            if (receiving == null || !isCurrentActiveShift(receiving, staff, "STAFF", WorkShiftService.businessNow())) {
                em.getTransaction().rollback();
                return MutationResult.INVALID;
            }
            Orders order = em.find(Orders.class, orderId, LockModeType.PESSIMISTIC_WRITE);
            if (order == null) { em.getTransaction().rollback(); return MutationResult.NOT_FOUND; }
            if (!canClaimHandover(order, receiving, expectedStatus, expectedOwnerShiftId)) { em.getTransaction().rollback(); return MutationResult.CONFLICT; }
            String source = order.getStaffShift() == null ? "unowned" : String.valueOf(order.getStaffShift().getShiftId());
            order.setStaffShift(receiving);
            order.setStaff(staff);
            em.persist(new OrderStatusHistory(orderId, staffId, "STAFF", order.getOrderStatus(), order.getOrderStatus(),
                    "Handover from shift " + source + " to shift " + receiving.getShiftId() + " by Staff " + staffId, LocalDateTime.now()));
            em.getTransaction().commit();
            return MutationResult.SUCCESS;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally { em.close(); }
    }

    static boolean matchesExpectedStatus(Orders order, String expectedStatus) {
        return order != null && expectedStatus != null && expectedStatus.equals(order.getOrderStatus());
    }

    public enum MutationResult { SUCCESS, CONFLICT, INVALID, UNPROCESSABLE, NOT_FOUND }

    private boolean requireCheckedInStaff(EntityManager em, int staffId) {
        User staff = em.find(User.class, staffId);
        return currentActiveShift(em, staff, "STAFF") != null;
    }

    private boolean validRetrySchedule(EntityManager em, String retryMode, LocalDateTime scheduledAt) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("SELECT config_key, config_value FROM ShippingConfig WHERE config_key IN ('business_open_time', 'business_close_time')").getResultList();
        Map<String, String> config = new HashMap<>();
        for (Object[] row : rows) config.put((String) row[0], (String) row[1]);
        return DeliveryFailurePolicy.isValidSchedule(retryMode, scheduledAt, WorkShiftService.businessNow(),
                java.time.LocalTime.parse(config.getOrDefault(StoreConfigService.OPEN_TIME, "00:00")),
                java.time.LocalTime.parse(config.getOrDefault(StoreConfigService.CLOSE_TIME, "00:00")));
    }

    private boolean requireCheckedInShipper(EntityManager em, int shipperId) {
        User shipper = em.find(User.class, shipperId);
        return currentActiveShift(em, shipper, "SHIPPER") != null;
    }

    private WorkShift currentActiveShift(EntityManager em, User user, String role) {
        if (user == null) return null;
        LocalDateTime now = WorkShiftService.businessNow();
        List<WorkShift> shifts = em.createQuery("SELECT ws FROM WorkShift ws WHERE ws.user.userId = :userId AND ws.shiftDate = :today AND ws.status = 'CHECKED_IN' AND ws.checkInAt IS NOT NULL AND ws.checkOutAt IS NULL ORDER BY ws.checkInAt DESC, ws.shiftId DESC", WorkShift.class)
                .setParameter("userId", user.getUserId()).setParameter("today", now.toLocalDate())
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        return shifts.stream().filter(shift -> isCurrentActiveShift(shift, user, role, now)).findFirst().orElse(null);
    }

    static boolean isCurrentActiveShift(WorkShift shift, User user, String role, LocalDateTime now) {
        return user != null && role.equals(user.getRole()) && "ACTIVE".equals(user.getStatus())
                && WorkShiftService.isValidCheckedInShift(shift, now);
    }

    static boolean canStartScheduledRetry(Orders order, WorkShift shipperShift, LocalDateTime now) {
        return order != null && order.getShipper() != null
                && isCurrentActiveShift(shipperShift, order.getShipper(), "SHIPPER", now);
    }

    public static boolean isCanonicalStatus(String status) {
        return TRANSITIONS.containsKey(status);
    }

    static boolean isActorRole(String role) {
        return Set.of("USER", "CUSTOMER", "STAFF", "ADMIN", "SHIPPER", "SYSTEM").contains(role);
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

    private boolean applyCancellation(EntityManager em, Orders order, String actorRole, Integer actorUserId,
                                      String reason, LocalDateTime now) {
        if (!inventoryReservationService.cancel(em, order)) return false;
        String from = order.getOrderStatus();
        releaseCoupon(em, order.getOrderId());
        applyStatus(order, "CANCELLED", now);
        order.setCancelledAt(now);
        order.setCancelledBy("USER".equals(actorRole) ? "CUSTOMER" : actorRole);
        if ("PAID".equals(order.getPaymentStatus())) order.setRefundStatus("PENDING");
        if (reason != null && !reason.isBlank()) order.setFailureReason(reason);
        User actor = actorUserId == null ? null : em.find(User.class, actorUserId);
        if (actor != null && ("STAFF".equals(actorRole) || "ADMIN".equals(actorRole))) order.setStaff(actor);
        em.persist(new OrderStatusHistory(order.getOrderId(), actorUserId, actorRole, from, "CANCELLED",
                reason != null ? reason : "Hủy đơn", now));
        return true;
    }

}
