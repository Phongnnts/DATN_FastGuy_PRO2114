package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import entity.Orders;
import entity.User;
import entity.WorkShift;
import jakarta.persistence.EntityManager;
import service.OrderTransitionService;
import service.StaffOrderService;
import service.WorkShiftService;
import utils.DatabaseUtil;

class StaffShiftOwnershipHandoverIT {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final List<Integer> orderIds = new ArrayList<>();
    private final List<Integer> userIds = new ArrayList<>();
    private final List<Integer> shiftIds = new ArrayList<>();
    private int currentStaffId;
    private int currentShiftId;
    private int otherStaffId;
    private int otherShiftId;
    private int shipperId;

    @Test
    void staffShiftOwnershipAndHandoverUseRealDisposableTransactions() throws Throwable {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("FASTGUY_DISPOSABLE_DB")),
                "Set FASTGUY_DISPOSABLE_DB=true only for an approved disposable database");
        EntityManager em = DatabaseUtil.getEntityManager();
        Throwable failure = null;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            verifyTarget(em);
            seed(em);
            StaffOrderService staffOrders = new StaffOrderService();
            WorkShiftService shifts = new WorkShiftService();

            int pending = insertOrder(em, "PENDING", null, null);
            assertEquals(OrderTransitionService.MutationResult.SUCCESS,
                    staffOrders.updateStatus(pending, "CONFIRMED", currentStaffId, null, "PENDING"));
            assertOrder(em, pending, "CONFIRMED", currentShiftId);
            assertContains(staffOrders.getConfirmedOrders(currentStaffId), pending);
            assertFalse(contains(staffOrders.getHandoverOrders(currentStaffId), pending));
            assertContains(staffOrders.getHandoverOrders(otherStaffId), pending);

            int unowned = insertOrder(em, "CONFIRMED", null, null);
            assertEquals(OrderTransitionService.MutationResult.SUCCESS,
                    staffOrders.claimHandover(unowned, currentStaffId, "CONFIRMED", null));
            assertOrder(em, unowned, "CONFIRMED", currentShiftId);

            int otherOwned = insertOrder(em, "PREPARING", otherStaffId, otherShiftId);
            assertEquals(OrderTransitionService.MutationResult.SUCCESS,
                    staffOrders.claimHandover(otherOwned, currentStaffId, "PREPARING", otherShiftId));
            assertOrder(em, otherOwned, "PREPARING", currentShiftId);

            int raced = insertOrder(em, "READY", otherStaffId, otherShiftId);
            CountDownLatch start = new CountDownLatch(1);
            Future<OrderTransitionService.MutationResult> first = executor.submit(
                    () -> claimAfter(start, raced, currentStaffId, otherShiftId));
            Future<OrderTransitionService.MutationResult> second = executor.submit(
                    () -> claimAfter(start, raced, currentStaffId, otherShiftId));
            start.countDown();
            List<OrderTransitionService.MutationResult> results = List.of(
                    first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertEquals(1, results.stream().filter(OrderTransitionService.MutationResult.SUCCESS::equals).count());
            assertEquals(1, results.stream().filter(OrderTransitionService.MutationResult.CONFLICT::equals).count());
            assertOrder(em, raced, "READY", currentShiftId);

            WorkShiftService.ActiveOwnershipConflict conflict = assertThrows(
                    WorkShiftService.ActiveOwnershipConflict.class,
                    () -> shifts.check(currentShiftId, currentStaffId, false));
            assertTrue(conflict.getActiveOwnershipCount() > 0);
            assertShiftCheckedIn(em, currentShiftId);
            transferOwnedOrders(em, currentShiftId, otherShiftId);
            shifts.check(currentShiftId, currentStaffId, false);
            assertShiftCheckedOut(em, currentShiftId);

            int ready = insertOrder(em, "READY", otherStaffId, otherShiftId);
            assertEquals(OrderTransitionService.MutationResult.SUCCESS,
                    staffOrders.assignShipper(ready, shipperId, otherStaffId, "READY"));
            assertOrder(em, ready, "ASSIGNED", null);

            int failed = insertOrder(em, "DELIVERY_FAILED", null, null);
            setDeliveryFailureState(em, failed);
            assertEquals(OrderTransitionService.MutationResult.SUCCESS,
                    staffOrders.claimHandover(failed, otherStaffId, "DELIVERY_FAILED", null));
            assertOrder(em, failed, "DELIVERY_FAILED", otherShiftId);
            assertContains(staffOrders.getDeliveryFailureQueue(otherStaffId), failed);
            assertEquals(OrderTransitionService.MutationResult.SUCCESS,
                    staffOrders.retryDelivery(failed, otherStaffId, "DELIVERY_FAILED", shipperId,
                            "IMMEDIATE", null, "Disposable ownership recovery"));
            assertOrder(em, failed, "PICKED_UP", null);
        } catch (Throwable t) {
            failure = t;
            throw t;
        } finally {
            executor.shutdownNow();
            cleanupPreserving(em, failure);
            DatabaseUtil.close();
        }
    }

    private OrderTransitionService.MutationResult claimAfter(CountDownLatch start, int orderId,
                                                               int staffId, int ownerShiftId) throws InterruptedException {
        start.await();
        return new StaffOrderService().claimHandover(orderId, staffId, "READY", ownerShiftId);
    }

    private void verifyTarget(EntityManager em) {
        Object[] identity = (Object[]) em.createNativeQuery(
                "SELECT @@SERVERNAME,DB_NAME(),DATABASEPROPERTYEX(DB_NAME(),'Status'),CAST(compatibility_level AS int) FROM sys.databases WHERE name=DB_NAME()")
                .getSingleResult();
        String database = String.valueOf(identity[1]);
        assertEquals("FastGuyDB_Inventory054_Test", database, "Runtime DB_URL must target the approved disposable database");
        assertTrue(database.endsWith("_Test"), "Integration mutations require a *_Test database");
        assertEquals("ONLINE", identity[2]);
        assertTrue(((Number) identity[3]).intValue() >= 160, "Expected compatibility >= 160 but was " + identity[3]);
        Number column = (Number) em.createNativeQuery(
                "SELECT COUNT_BIG(*) FROM sys.columns WHERE object_id=OBJECT_ID('dbo.Orders') AND name='staff_shift_id'")
                .getSingleResult();
        assertEquals(1L, column.longValue(), "Migration 058 must be applied");
        System.out.println("StaffShiftOwnershipHandoverIT target verified: " + identity[0] + "/" + database);
    }

    private void seed(EntityManager em) {
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE).withNano(0);
        em.getTransaction().begin();
        currentStaffId = insertUser(em, "STAFF", "Current Staff");
        otherStaffId = insertUser(em, "STAFF", "Other Staff");
        shipperId = insertUser(em, "SHIPPER", "Shipper");
        currentShiftId = insertShift(em, currentStaffId, now);
        otherShiftId = insertShift(em, otherStaffId, now);
        insertShift(em, shipperId, now);
        em.getTransaction().commit();
    }

    private int insertUser(EntityManager em, String role, String name) {
        String token = Long.toUnsignedString(System.nanoTime()) + userIds.size();
        User user = new User();
        user.setRole(role);
        user.setEmail("ownership-" + token + "@test.local");
        user.setPhone("9" + token.substring(Math.max(0, token.length() - 9)));
        user.setPasswordHash("test");
        user.setFullName(name);
        user.setStatus("ACTIVE");
        em.persist(user);
        em.flush();
        userIds.add(user.getUserId());
        return user.getUserId();
    }

    private int insertShift(EntityManager em, int userId, LocalDateTime now) {
        WorkShift shift = new WorkShift();
        shift.setUser(em.getReference(User.class, userId));
        shift.setShiftDate(now.toLocalDate());
        shift.setStartTime(now.minusHours(2).toLocalTime());
        shift.setEndTime(now.minusMinutes(1).toLocalTime());
        shift.setCheckInAt(now.minusHours(1));
        shift.setStatus("CHECKED_IN");
        em.persist(shift);
        em.flush();
        shiftIds.add(shift.getShiftId());
        return shift.getShiftId();
    }

    private int insertOrder(EntityManager em, String status, Integer staffId, Integer staffShiftId) {
        boolean ownTransaction = !em.getTransaction().isActive();
        if (ownTransaction) em.getTransaction().begin();
        String token = Long.toUnsignedString(System.nanoTime()) + orderIds.size();
        Orders order = new Orders();
        order.setOrderCode("OWN-" + token);
        order.setCustomerName("Ownership Test");
        order.setCustomerPhone("000");
        order.setCustomerAddress("Disposable Test");
        order.setTotalAmount(BigDecimal.ONE);
        order.setShippingFee(BigDecimal.ZERO);
        order.setServiceFee(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFinalAmount(BigDecimal.ONE);
        order.setPaymentMethod("BANK_TRANSFER");
        order.setPaymentStatus("PAID");
        order.setOrderStatus(status);
        order.setCreatedAt(LocalDateTime.now(BUSINESS_ZONE).minusMinutes(5));
        if (staffId != null) order.setStaff(em.getReference(User.class, staffId));
        if (staffShiftId != null) order.setStaffShift(em.getReference(WorkShift.class, staffShiftId));
        em.persist(order);
        em.flush();
        orderIds.add(order.getOrderId());
        if (ownTransaction) em.getTransaction().commit();
        return order.getOrderId();
    }

    private void setDeliveryFailureState(EntityManager em, int orderId) {
        em.getTransaction().begin();
        em.createNativeQuery("UPDATE Orders SET delivery_attempt_count=1,delivery_attempt_limit=3,delivery_failure_code='CUSTOMER_UNREACHABLE',failure_reason=N'Disposable test',delivery_failed_at=SYSDATETIME() WHERE order_id=:id")
                .setParameter("id", orderId).executeUpdate();
        em.getTransaction().commit();
    }

    private void transferOwnedOrders(EntityManager em, int fromShiftId, int toShiftId) {
        em.getTransaction().begin();
        em.createNativeQuery("UPDATE Orders SET staff_shift_id=:toShift,staff_id=:staff WHERE staff_shift_id=:fromShift AND order_status IN ('CONFIRMED','PREPARING','READY','DELIVERY_FAILED')")
                .setParameter("toShift", toShiftId).setParameter("staff", otherStaffId)
                .setParameter("fromShift", fromShiftId).executeUpdate();
        em.getTransaction().commit();
    }

    private void assertOrder(EntityManager em, int orderId, String status, Integer shiftId) {
        em.clear();
        Object[] row = (Object[]) em.createNativeQuery(
                "SELECT order_status,staff_shift_id FROM Orders WHERE order_id=:id")
                .setParameter("id", orderId).getSingleResult();
        assertEquals(status, row[0]);
        if (shiftId == null) assertNull(row[1]);
        else assertEquals(shiftId.intValue(), ((Number) row[1]).intValue());
    }

    private void assertContains(List<Orders> orders, int orderId) {
        assertTrue(contains(orders, orderId), "Expected queue to contain order " + orderId);
    }

    private boolean contains(List<Orders> orders, int orderId) {
        return orders.stream().anyMatch(order -> order.getOrderId() == orderId);
    }

    private void assertShiftCheckedIn(EntityManager em, int shiftId) {
        Object[] row = (Object[]) em.createNativeQuery("SELECT status,check_out_at FROM WorkShift WHERE shift_id=:id")
                .setParameter("id", shiftId).getSingleResult();
        assertEquals("CHECKED_IN", row[0]);
        assertNull(row[1]);
    }

    private void assertShiftCheckedOut(EntityManager em, int shiftId) {
        Object[] row = (Object[]) em.createNativeQuery("SELECT status,check_out_at FROM WorkShift WHERE shift_id=:id")
                .setParameter("id", shiftId).getSingleResult();
        assertEquals("CHECKED_OUT", row[0]);
        assertTrue(row[1] != null);
    }

    private void cleanupPreserving(EntityManager em, Throwable original) {
        RuntimeException cleanupFailure = null;
        try {
            cleanup(em);
        } catch (RuntimeException e) {
            cleanupFailure = e;
            if (original != null) original.addSuppressed(e);
        }
        try {
            em.close();
        } catch (RuntimeException e) {
            if (original != null) original.addSuppressed(e);
            else if (cleanupFailure != null) cleanupFailure.addSuppressed(e);
            else throw e;
        }
        if (original == null && cleanupFailure != null) throw cleanupFailure;
    }

    private void cleanup(EntityManager em) {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
        em.getTransaction().begin();
        if (!orderIds.isEmpty()) {
            em.createNativeQuery("DELETE FROM OrderStatusHistory WHERE order_id IN (:ids)").setParameter("ids", orderIds).executeUpdate();
            em.createNativeQuery("DELETE FROM Orders WHERE order_id IN (:ids)").setParameter("ids", orderIds).executeUpdate();
        }
        if (!shiftIds.isEmpty()) em.createNativeQuery("DELETE FROM WorkShift WHERE shift_id IN (:ids)").setParameter("ids", shiftIds).executeUpdate();
        if (!userIds.isEmpty()) {
            em.createNativeQuery("DELETE FROM CartItem WHERE cart_id IN (SELECT cart_id FROM Cart WHERE user_id IN (:ids))").setParameter("ids", userIds).executeUpdate();
            em.createNativeQuery("DELETE FROM Cart WHERE user_id IN (:ids)").setParameter("ids", userIds).executeUpdate();
            em.createNativeQuery("DELETE FROM Users WHERE user_id IN (:ids)").setParameter("ids", userIds).executeUpdate();
        }
        em.getTransaction().commit();
        assertEquals(0L, remaining(em), "Integration cleanup must remove every fixture");
        System.out.println("StaffShiftOwnershipHandoverIT cleanup verified: 0 tracked rows");
    }

    private long remaining(EntityManager em) {
        long count = 0;
        if (!orderIds.isEmpty()) count += count(em, "Orders", "order_id", orderIds);
        if (!shiftIds.isEmpty()) count += count(em, "WorkShift", "shift_id", shiftIds);
        if (!userIds.isEmpty()) count += count(em, "Users", "user_id", userIds);
        return count;
    }

    private long count(EntityManager em, String table, String column, List<Integer> ids) {
        return ((Number) em.createNativeQuery("SELECT COUNT_BIG(*) FROM " + table + " WHERE " + column + " IN (:ids)")
                .setParameter("ids", ids).getSingleResult()).longValue();
    }
}
