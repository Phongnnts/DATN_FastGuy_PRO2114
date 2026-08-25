package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import dao.OrdersDAO;
import service.OrderScheduler;
import service.OrderTransitionService;
import utils.DatabaseUtil;

class ReadyOrderClosingCancellationIT {
    private final List<Integer> orderIds = new ArrayList<>();
    private Integer shipperId;
    private Integer staffId;
    private Integer couponId;
    private Integer inventoryItemId;
    private String originalOpen;
    private String originalClose;

    @Test
    void cancelsOnlyOverdueReadyUnassignedOrderWithAllSideEffects() throws Throwable {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("FASTGUY_DISPOSABLE_DB")),
                "Set FASTGUY_DISPOSABLE_DB=true only for an approved disposable database");
        EntityManager em = DatabaseUtil.getEntityManager();
        Throwable failure = null;
        try {
            String database = (String) em.createNativeQuery("SELECT DB_NAME()").getSingleResult();
            assertTrue(database.endsWith("_Test"), "Integration mutations require a *_Test database");
            seed(em);
            assertTrue(new OrdersDAO().findReadyWithoutShipperForClosing().stream()
                    .noneMatch(order -> order.getOrderId() == orderIds.get(5)));

            new OrderScheduler().cancelReadyOrdersAfterClosing();

            em.clear();
            Object[] cancelled = (Object[]) em.createNativeQuery(
                    "SELECT order_status, cancelled_by, failure_reason, refund_status FROM Orders WHERE order_id = :id")
                    .setParameter("id", orderIds.get(0)).getSingleResult();
            assertEquals("CANCELLED", cancelled[0]);
            assertEquals("SYSTEM", cancelled[1]);
            assertEquals("Quá giờ đóng cửa chưa được điều phối", cancelled[2]);
            assertEquals("PENDING", cancelled[3]);
            assertEquals("RELEASED", em.createNativeQuery(
                    "SELECT status FROM InventoryReservation WHERE order_id = :id")
                    .setParameter("id", orderIds.get(0)).getSingleResult());
            Object[] redemption = (Object[]) em.createNativeQuery(
                    "SELECT order_id, used_at FROM CouponRedemption WHERE coupon_id = :couponId AND user_id = :userId")
                    .setParameter("couponId", couponId).setParameter("userId", shipperId).getSingleResult();
            assertNull(redemption[0]);
            assertNull(redemption[1]);
            assertEquals(0, ((Number) em.createNativeQuery("SELECT used_count FROM Coupon WHERE coupon_id = :id")
                    .setParameter("id", couponId).getSingleResult()).intValue());
            Object[] history = (Object[]) em.createNativeQuery(
                    "SELECT actor_role, note FROM OrderStatusHistory WHERE order_id = :id AND to_status = 'CANCELLED'")
                    .setParameter("id", orderIds.get(0)).getSingleResult();
            assertEquals("SYSTEM", history[0]);
            assertEquals("Quá giờ đóng cửa chưa được điều phối", history[1]);
            assertEquals(List.of("ASSIGNED", "PICKED_UP", "DELIVERY_FAILED", "CANCELLED", "READY"),
                    em.createNativeQuery("SELECT order_status FROM Orders WHERE order_id IN (:a,:p,:f,:c,:r) ORDER BY order_id")
                            .setParameter("a", orderIds.get(1)).setParameter("p", orderIds.get(2))
                            .setParameter("f", orderIds.get(3)).setParameter("c", orderIds.get(4))
                            .setParameter("r", orderIds.get(5)).getResultList());
        } catch (Throwable t) {
            failure = t;
            throw t;
        } finally {
            cleanupPreserving(em, failure);
        }
    }

    @Test
    void assignmentAndClosingCancellationHaveExactlyOneWinner() throws Throwable {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("FASTGUY_DISPOSABLE_DB")),
                "Set FASTGUY_DISPOSABLE_DB=true only for an approved disposable database");
        EntityManager em = DatabaseUtil.getEntityManager();
        Throwable failure = null;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            String database = (String) em.createNativeQuery("SELECT DB_NAME()").getSingleResult();
            assertTrue(database.endsWith("_Test"), "Integration mutations require a *_Test database");
            seed(em);
            int orderId = orderIds.get(0);
            CountDownLatch start = new CountDownLatch(1);
            Future<OrderTransitionService.MutationResult> assignment = executor.submit(() -> {
                start.await();
                return new OrderTransitionService().transition(orderId, "ASSIGNED", "STAFF", staffId,
                        "Task 4 race", shipperId, null, "READY");
            });
            Future<OrderTransitionService.CancellationResult> cancellation = executor.submit(() -> {
                start.await();
                return new OrderTransitionService().cancelReadyIfUnassignedAfterClosing(orderId,
                        LocalDateTime.now(), java.time.LocalTime.of(8, 0), java.time.LocalTime.of(22, 0));
            });

            start.countDown();
            OrderTransitionService.MutationResult assignmentResult = assignment.get();
            OrderTransitionService.CancellationResult cancellationResult = cancellation.get();
            em.clear();
            Object[] row = (Object[]) em.createNativeQuery(
                    "SELECT order_status, shipper_id, refund_status FROM Orders WHERE order_id = :id")
                    .setParameter("id", orderId).getSingleResult();
            long cancelledHistory = ((Number) em.createNativeQuery(
                    "SELECT COUNT_BIG(*) FROM OrderStatusHistory WHERE order_id = :id AND to_status = 'CANCELLED'")
                    .setParameter("id", orderId).getSingleResult()).longValue();

            if (assignmentResult == OrderTransitionService.MutationResult.SUCCESS) {
                assertNull(cancellationResult);
                assertEquals("ASSIGNED", row[0]);
                assertEquals(shipperId, ((Number) row[1]).intValue());
                assertNull(row[2]);
                assertEquals(0L, cancelledHistory);
                assertEquals("RESERVED", reservationStatus(em, orderId));
                assertEquals(orderId, redemptionOrderId(em));
            } else {
                assertEquals(OrderTransitionService.MutationResult.CONFLICT, assignmentResult);
                assertTrue(cancellationResult != null);
                assertEquals("CANCELLED", row[0]);
                assertNull(row[1]);
                assertEquals("PENDING", row[2]);
                assertEquals(1L, cancelledHistory);
                assertEquals("RELEASED", reservationStatus(em, orderId));
                assertNull(redemptionOrderId(em));
            }
        } catch (Throwable t) {
            failure = t;
            throw t;
        } finally {
            executor.shutdownNow();
            cleanupPreserving(em, failure);
        }
    }

    private void seed(EntityManager em) {
        em.getTransaction().begin();
        originalOpen = config(em, "business_open_time");
        originalClose = config(em, "business_close_time");
        updateConfig(em, "business_open_time", "08:00");
        updateConfig(em, "business_close_time", "22:00");
        String token = Long.toString(System.nanoTime());
        shipperId = insertedId(em, "INSERT INTO Users(role_name,phone,password_hash,full_name,status) OUTPUT INSERTED.user_id VALUES ('SHIPPER',:phone,'test',N'Task 4','ACTIVE')", "task4-shipper-" + token);
        staffId = insertedId(em, "INSERT INTO Users(role_name,phone,password_hash,full_name,status) OUTPUT INSERTED.user_id VALUES ('STAFF',:phone,'test',N'Task 4 Staff','ACTIVE')", "task4-staff-" + token);
        em.createNativeQuery("INSERT INTO WorkShift(user_id,shift_date,start_time,end_time,check_in_at,status) VALUES (:userId,CAST(SYSDATETIME() AS date),'00:00','23:59',SYSDATETIME(),'CHECKED_IN')")
                .setParameter("userId", shipperId).executeUpdate();
        couponId = insertedId(em, "INSERT INTO Coupon(code,type,value,min_order,max_uses,used_count,is_active,is_public) OUTPUT INSERTED.coupon_id VALUES (:value,'FIXED',1,0,1,1,1,0)", "TASK4-" + token);
        inventoryItemId = insertedId(em, "INSERT INTO InventoryItem(name,item_type,base_unit,inventory_code,on_hand_quantity,reserved_quantity,minimum_quantity,active) OUTPUT INSERTED.inventory_item_id VALUES (N'Task 4','INGREDIENT','PIECE',:value,10,1,0,1)", "TASK4-" + token);
        orderIds.add(insertOrder(em, token + "-ready", "READY", null));
        orderIds.add(insertOrder(em, token + "-assigned", "ASSIGNED", shipperId));
        orderIds.add(insertOrder(em, token + "-picked", "PICKED_UP", shipperId));
        orderIds.add(insertOrder(em, token + "-failed", "DELIVERY_FAILED", null));
        orderIds.add(insertOrder(em, token + "-cancelled", "CANCELLED", null));
        orderIds.add(insertOrder(em, token + "-ready-assigned", "READY", shipperId));
        int reservationId = ((Number) em.createNativeQuery("INSERT INTO InventoryReservation(order_id,status) OUTPUT INSERTED.reservation_id VALUES (:orderId,'RESERVED')")
                .setParameter("orderId", orderIds.get(0)).getSingleResult()).intValue();
        em.createNativeQuery("INSERT INTO InventoryReservationItem(reservation_id,inventory_item_id,quantity) VALUES (:reservationId,:itemId,1)")
                .setParameter("reservationId", reservationId).setParameter("itemId", inventoryItemId).executeUpdate();
        em.createNativeQuery("INSERT INTO CouponRedemption(coupon_id,user_id,order_id,used_at,discount_amount) VALUES (:couponId,:userId,:orderId,SYSDATETIME(),1)")
                .setParameter("couponId", couponId).setParameter("userId", shipperId)
                .setParameter("orderId", orderIds.get(0)).executeUpdate();
        em.getTransaction().commit();
    }

    private int insertOrder(EntityManager em, String code, String status, Integer shipperId) {
        return ((Number) em.createNativeQuery("INSERT INTO Orders(order_code,customer_name,customer_phone,customer_address,total_amount,final_amount,payment_method,payment_status,order_status,shipper_id,ready_at,created_at) OUTPUT INSERTED.order_id VALUES (:code,N'Task 4','000',N'Test',1,1,'BANK_TRANSFER','PAID',:status,:shipperId,DATEADD(DAY,-1,SYSDATETIME()),DATEADD(DAY,-1,SYSDATETIME()))")
                .setParameter("code", code).setParameter("status", status).setParameter("shipperId", shipperId)
                .getSingleResult()).intValue();
    }

    private int insertedId(EntityManager em, String sql, String value) {
        return ((Number) em.createNativeQuery(sql).setParameter(sql.contains(":phone") ? "phone" : "value", value)
                .getSingleResult()).intValue();
    }

    private String config(EntityManager em, String key) {
        return (String) em.createNativeQuery("SELECT config_value FROM ShippingConfig WHERE config_key = :key")
                .setParameter("key", key).getSingleResult();
    }

    private void updateConfig(EntityManager em, String key, String value) {
        em.createNativeQuery("UPDATE ShippingConfig SET config_value = :value WHERE config_key = :key")
                .setParameter("value", value).setParameter("key", key).executeUpdate();
    }

    private String reservationStatus(EntityManager em, int orderId) {
        return (String) em.createNativeQuery("SELECT status FROM InventoryReservation WHERE order_id = :id")
                .setParameter("id", orderId).getSingleResult();
    }

    private Object redemptionOrderId(EntityManager em) {
        return em.createNativeQuery("SELECT order_id FROM CouponRedemption WHERE coupon_id = :couponId AND user_id = :userId")
                .setParameter("couponId", couponId).setParameter("userId", shipperId).getSingleResult();
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
        } catch (RuntimeException closeFailure) {
            if (original != null) original.addSuppressed(closeFailure);
            else if (cleanupFailure != null) cleanupFailure.addSuppressed(closeFailure);
            else throw closeFailure;
        }
        if (original == null && cleanupFailure != null) throw cleanupFailure;
    }

    private void cleanup(EntityManager em) {
        if (em == null) return;
        try {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.getTransaction().begin();
            if (couponId != null) {
                em.createNativeQuery("DELETE FROM CouponRedemption WHERE coupon_id = :id").setParameter("id", couponId).executeUpdate();
            }
            if (!orderIds.isEmpty()) {
                em.createNativeQuery("DELETE FROM InventoryTransaction WHERE order_id IN (:ids)").setParameter("ids", orderIds).executeUpdate();
                em.createNativeQuery("DELETE FROM OrderStatusHistory WHERE order_id IN (:ids)").setParameter("ids", orderIds).executeUpdate();
                em.createNativeQuery("DELETE FROM InventoryReservationItem WHERE reservation_id IN (SELECT reservation_id FROM InventoryReservation WHERE order_id IN (:ids))").setParameter("ids", orderIds).executeUpdate();
                em.createNativeQuery("DELETE FROM InventoryReservation WHERE order_id IN (:ids)").setParameter("ids", orderIds).executeUpdate();
                em.createNativeQuery("DELETE FROM Orders WHERE order_id IN (:ids)").setParameter("ids", orderIds).executeUpdate();
            }
            if (couponId != null) {
                em.createNativeQuery("DELETE FROM Coupon WHERE coupon_id = :id").setParameter("id", couponId).executeUpdate();
            }
            if (inventoryItemId != null) em.createNativeQuery("DELETE FROM InventoryItem WHERE inventory_item_id = :id").setParameter("id", inventoryItemId).executeUpdate();
            if (shipperId != null) {
                em.createNativeQuery("DELETE FROM WorkShift WHERE user_id = :id").setParameter("id", shipperId).executeUpdate();
                em.createNativeQuery("DELETE FROM Users WHERE user_id = :id").setParameter("id", shipperId).executeUpdate();
            }
            if (staffId != null) em.createNativeQuery("DELETE FROM Users WHERE user_id = :id").setParameter("id", staffId).executeUpdate();
            if (originalOpen != null) updateConfig(em, "business_open_time", originalOpen);
            if (originalClose != null) updateConfig(em, "business_close_time", originalClose);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        }
    }
}
