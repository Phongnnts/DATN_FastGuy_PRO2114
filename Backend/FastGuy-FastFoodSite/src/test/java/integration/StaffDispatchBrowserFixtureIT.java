package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import entity.InventoryItem;
import entity.InventoryReservation;
import entity.InventoryReservationItem;
import entity.Orders;
import entity.User;
import entity.WorkShift;
import jakarta.persistence.EntityManager;
import service.OrderScheduler;
import service.OrderTransitionService;
import service.StaffOrderService;
import utils.DatabaseUtil;
import utils.PasswordUtil;

class StaffDispatchBrowserFixtureIT {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Test
    void runFixtureAction() {
        String action = requiredProperty("e2e.action");
        String runId = requiredEnv("FASTGUY_E2E_RUN_ID");
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            verifyTarget(em);
            switch (action) {
                case "seed" -> seed(em, runId);
                case "conflict" -> createAssignmentConflict(em, runId);
                case "scheduler" -> runScheduler(em, runId);
                case "cleanup" -> cleanup(em, runId);
                default -> throw new IllegalArgumentException("Unsupported e2e.action");
            }
        } finally {
            em.close();
            DatabaseUtil.close();
        }
    }

    private void verifyTarget(EntityManager em) {
        assertEquals("true", requiredEnv("FASTGUY_DISPOSABLE_DB").toLowerCase(), "Disposable guard required");
        Object[] identity = (Object[]) em.createNativeQuery(
                "SELECT @@SERVERNAME,DB_NAME(),DATABASEPROPERTYEX(DB_NAME(),'Status'),compatibility_level FROM sys.databases WHERE name=DB_NAME()")
                .getSingleResult();
        String database = requiredEnv("FASTGUY_E2E_DB_NAME");
        assertEquals("DuckJo", identity[0]);
        assertEquals(database, identity[1]);
        assertTrue(database.endsWith("_Test"));
        assertEquals("ONLINE", identity[2]);
        assertEquals(160, Byte.toUnsignedInt(((Number) identity[3]).byteValue()));
        System.out.println("Staff dispatch browser target verified: DuckJo/" + database + " ONLINE compatibility 160");
    }

    private void seed(EntityManager em, String runId) {
        cleanup(em, runId);
        String prefix = prefix(runId);
        em.getTransaction().begin();
        String originalOpen = config(em, "business_open_time");
        String originalClose = config(em, "business_close_time");
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE).withNano(0);
        FixtureTimeline timeline = timeline(now);
        updateConfig(em, "business_open_time", timeline.open().toString());
        updateConfig(em, "business_close_time", timeline.close().toString());
        String password = requiredEnv("FASTGUY_E2E_STAFF_PASSWORD");
        User staff = insertUser(em, "STAFF", "staff-" + runId + "@test.local", "7" + digits(runId), "E2E Staff", password);
        User shipper = insertUser(em, "SHIPPER", "shipper-" + runId + "@test.local", "8" + digits(runId), "E2E Shipper", password);
        insertShift(em, staff, now);
        insertShift(em, shipper, now);
        insertOrder(em, prefix + "PRIORITY-OLD", "READY", now.minusMinutes(45), now.minusMinutes(45), null, null,
                "E2E_CONFIG:" + originalOpen + "|" + originalClose);
        insertOrder(em, prefix + "PRIORITY-RACE", "READY", now.minusMinutes(40), now.minusMinutes(40), null, null, null);
        insertOrder(em, prefix + "NEW-RECENT", "READY", now.minusMinutes(1), now.minusMinutes(1), null, null, null);
        insertOrder(em, prefix + "NEW-OLDER", "READY", now.minusMinutes(3), now.minusMinutes(3), null, null, null);
        insertOrder(em, prefix + "REVIEW", "DELIVERY_FAILED", now.minusHours(2), now.minusHours(2), shipper,
                now.minusMinutes(10), null);
        Orders cancel = insertOrder(em, prefix + "CANCEL", "PENDING", timeline.cancellationCreatedAt(),
                timeline.cancellationCreatedAt(), null, null, null);
        insertReservation(em, cancel, runId);
        em.getTransaction().commit();
        var counts = new StaffOrderService().getDispatchOrders("PRIORITY").counts();
        Object[] diagnostic = (Object[]) em.createNativeQuery("SELECT order_status,created_at,ready_at,(SELECT config_value FROM ShippingConfig WHERE config_key='business_open_time'),(SELECT config_value FROM ShippingConfig WHERE config_key='business_close_time') FROM Orders WHERE order_code=:code")
                .setParameter("code", prefix + "PRIORITY-OLD").getSingleResult();
        assertEquals(2L, counts.get("priority"), "Seed must expose two Priority orders: " + java.util.Arrays.toString(diagnostic));
        assertEquals(2L, counts.get("new"), "Seed must expose two New orders");
        assertEquals(1L, counts.get("review"), "Seed must expose one Review order");
        System.out.println("Staff dispatch browser seed complete: " + runId);
    }

    static FixtureTimeline timeline(LocalDateTime now) {
        return new FixtureTimeline(now.minusHours(2).toLocalTime().withSecond(0).withNano(0),
                now.plusHours(2).toLocalTime().withSecond(0).withNano(0), now.minusDays(2));
    }

    record FixtureTimeline(LocalTime open, LocalTime close, LocalDateTime cancellationCreatedAt) {}

    private void createAssignmentConflict(EntityManager em, String runId) {
        String prefix = prefix(runId);
        Number orderId = (Number) em.createNativeQuery("SELECT order_id FROM Orders WHERE order_code=:code")
                .setParameter("code", prefix + "PRIORITY-RACE").getSingleResult();
        Number staffId = (Number) em.createNativeQuery("SELECT user_id FROM Users WHERE email=:email")
                .setParameter("email", "staff-" + runId + "@test.local").getSingleResult();
        Number shipperId = (Number) em.createNativeQuery("SELECT user_id FROM Users WHERE email=:email")
                .setParameter("email", "shipper-" + runId + "@test.local").getSingleResult();
        assertEquals(OrderTransitionService.MutationResult.SUCCESS,
                new StaffOrderService().assignShipper(orderId.intValue(), shipperId.intValue(), staffId.intValue(), "READY"));
        System.out.println("Staff dispatch browser conflict prepared: " + runId);
    }

    private void runScheduler(EntityManager em, String runId) {
        assertEquals(1L, orderCount(em, prefix(runId) + "CANCEL", "PENDING"));
        FixtureTimeline timeline = timeline(LocalDateTime.now(BUSINESS_ZONE));
        em.getTransaction().begin();
        em.createNativeQuery("UPDATE Orders SET order_status='READY',created_at=:createdAt,ready_at=:readyAt WHERE order_code=:code")
                .setParameter("createdAt", timeline.cancellationCreatedAt())
                .setParameter("readyAt", timeline.cancellationCreatedAt())
                .setParameter("code", prefix(runId) + "CANCEL")
                .executeUpdate();
        em.getTransaction().commit();
        new OrderScheduler().cancelReadyOrdersAfterClosing();
        assertEquals(1L, orderCount(em, prefix(runId) + "CANCEL", "CANCELLED"));
        System.out.println("Staff dispatch browser scheduler complete: " + runId);
    }

    private void cleanup(EntityManager em, String runId) {
        String prefix = prefix(runId);
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
        em.getTransaction().begin();
        List<?> notes = em.createNativeQuery("SELECT internal_note FROM Orders WHERE order_code=:code")
                .setParameter("code", prefix + "PRIORITY-OLD").getResultList();
        List<Integer> ids = em.createNativeQuery("SELECT order_id FROM Orders WHERE order_code LIKE :prefix", Integer.class)
                .setParameter("prefix", prefix + "%").getResultList();
        List<Integer> userIds = em.createNativeQuery("SELECT user_id FROM Users WHERE email IN (:staff,:shipper)", Integer.class)
                .setParameter("staff", "staff-" + runId + "@test.local").setParameter("shipper", "shipper-" + runId + "@test.local").getResultList();
        List<Integer> reservationIds = ids.isEmpty() ? List.of() : em.createNativeQuery(
                "SELECT reservation_id FROM InventoryReservation WHERE order_id IN (:ids)", Integer.class).setParameter("ids", ids).getResultList();
        List<Integer> cartIds = userIds.isEmpty() ? List.of() : em.createNativeQuery(
                "SELECT cart_id FROM Cart WHERE user_id IN (:ids)", Integer.class).setParameter("ids", userIds).getResultList();
        if (!ids.isEmpty()) {
            deleteByOrders(em, "InventoryTransaction", ids);
            deleteByOrders(em, "OrderStatusHistory", ids);
            deleteByOrders(em, "CouponRedemption", ids);
            em.createNativeQuery("DELETE FROM InventoryReservationItem WHERE reservation_id IN (SELECT reservation_id FROM InventoryReservation WHERE order_id IN (:ids))").setParameter("ids", ids).executeUpdate();
            deleteByOrders(em, "InventoryReservation", ids);
            em.createNativeQuery("DELETE FROM Orders WHERE order_id IN (:ids)").setParameter("ids", ids).executeUpdate();
        }
        em.createNativeQuery("DELETE FROM InventoryItem WHERE inventory_code=:code").setParameter("code", prefix + "INV").executeUpdate();
        String staffEmail = "staff-" + runId + "@test.local";
        String shipperEmail = "shipper-" + runId + "@test.local";
        em.createNativeQuery("DELETE FROM WorkShift WHERE user_id IN (SELECT user_id FROM Users WHERE email IN (:staff,:shipper))")
                .setParameter("staff", staffEmail).setParameter("shipper", shipperEmail).executeUpdate();
        em.createNativeQuery("DELETE FROM CartItem WHERE cart_id IN (SELECT cart_id FROM Cart WHERE user_id IN (SELECT user_id FROM Users WHERE email IN (:staff,:shipper)))")
                .setParameter("staff", staffEmail).setParameter("shipper", shipperEmail).executeUpdate();
        em.createNativeQuery("DELETE FROM Cart WHERE user_id IN (SELECT user_id FROM Users WHERE email IN (:staff,:shipper))")
                .setParameter("staff", staffEmail).setParameter("shipper", shipperEmail).executeUpdate();
        em.createNativeQuery("DELETE FROM Users WHERE email IN (:staff,:shipper)")
                .setParameter("staff", staffEmail).setParameter("shipper", shipperEmail).executeUpdate();
        if (!notes.isEmpty() && notes.get(0) instanceof String note && note.startsWith("E2E_CONFIG:")) {
            String[] values = note.substring("E2E_CONFIG:".length()).split("\\|", -1);
            updateConfig(em, "business_open_time", values[0]);
            updateConfig(em, "business_close_time", values[1]);
        }
        em.getTransaction().commit();
        assertZero(em, "Orders", "order_code LIKE :value", prefix + "%");
        assertZero(em, "Users", "email IN (:staff,:shipper)", staffEmail, shipperEmail);
        assertZero(em, "WorkShift", "user_id IN (:ids)", userIds);
        assertZero(em, "Cart", "user_id IN (:ids)", userIds);
        assertZero(em, "CartItem", "cart_id IN (:ids)", cartIds);
        assertZero(em, "InventoryItem", "inventory_code=:value", prefix + "INV");
        for (String table : List.of("InventoryTransaction", "OrderStatusHistory", "CouponRedemption", "InventoryReservation"))
            assertZero(em, table, "order_id IN (:ids)", ids);
        assertZero(em, "InventoryReservationItem", "reservation_id IN (:ids)", reservationIds);
        if (!notes.isEmpty() && notes.get(0) instanceof String note && note.startsWith("E2E_CONFIG:")) {
            String[] values = note.substring("E2E_CONFIG:".length()).split("\\|", -1);
            assertEquals(values[0], config(em, "business_open_time"));
            assertEquals(values[1], config(em, "business_close_time"));
        }
        System.out.println("Staff dispatch browser cleanup verified: Orders=0 Users=0 WorkShift=0 Cart=0 CartItem=0 InventoryItem=0 InventoryTransaction=0 OrderStatusHistory=0 CouponRedemption=0 InventoryReservation=0 InventoryReservationItem=0 ShippingConfig=restored for " + runId);
    }

    private void assertZero(EntityManager em, String table, String predicate, Object value) {
        if (value instanceof List<?> values && values.isEmpty()) return;
        var query = em.createNativeQuery("SELECT COUNT_BIG(*) FROM " + table + " WHERE " + predicate);
        if (predicate.contains(":ids")) query.setParameter("ids", value);
        else query.setParameter("value", value);
        assertEquals(0L, ((Number) query.getSingleResult()).longValue(), table);
    }

    private void assertZero(EntityManager em, String table, String predicate, String staff, String shipper) {
        Number count = (Number) em.createNativeQuery("SELECT COUNT_BIG(*) FROM " + table + " WHERE " + predicate)
                .setParameter("staff", staff).setParameter("shipper", shipper).getSingleResult();
        assertEquals(0L, count.longValue(), table);
    }

    private void deleteByOrders(EntityManager em, String table, List<Integer> ids) {
        em.createNativeQuery("DELETE FROM " + table + " WHERE order_id IN (:ids)").setParameter("ids", ids).executeUpdate();
    }

    private User insertUser(EntityManager em, String role, String email, String phone, String name, String password) {
        User user = new User();
        user.setRole(role); user.setEmail(email); user.setPhone(phone); user.setFullName(name);
        user.setPasswordHash(PasswordUtil.hash(password)); user.setStatus("ACTIVE");
        em.persist(user); em.flush(); return user;
    }

    private void insertShift(EntityManager em, User user, LocalDateTime now) {
        WorkShift shift = new WorkShift();
        shift.setUser(user); shift.setShiftDate(now.toLocalDate()); shift.setStartTime(now.minusHours(1).toLocalTime());
        shift.setEndTime(now.plusHours(1).toLocalTime()); shift.setCheckInAt(now.minusMinutes(1)); shift.setStatus("CHECKED_IN");
        em.persist(shift);
    }

    private Orders insertOrder(EntityManager em, String code, String status, LocalDateTime createdAt, LocalDateTime readyAt,
                               User shipper, LocalDateTime failedAt, String note) {
        Orders order = new Orders();
        order.setOrderCode(code); order.setCustomerName("E2E Customer"); order.setCustomerPhone("0900000000");
        order.setCustomerAddress("E2E Address"); order.setTotalAmount(BigDecimal.TEN); order.setShippingFee(BigDecimal.ZERO);
        order.setServiceFee(BigDecimal.ZERO); order.setDiscountAmount(BigDecimal.ZERO); order.setFinalAmount(BigDecimal.TEN);
        order.setPaymentMethod("BANK_TRANSFER"); order.setPaymentStatus("PAID"); order.setOrderStatus(status);
        order.setCreatedAt(createdAt); order.setReadyAt(readyAt); order.setInternalNote(note);
        if (shipper != null) { order.setShipper(shipper); order.setAssignedAt(createdAt); }
        if (failedAt != null) { order.setDeliveryFailedAt(failedAt); order.setDeliveryFailureCode("CUSTOMER_UNREACHABLE"); order.setFailureReason("E2E review"); }
        em.persist(order); em.flush(); return order;
    }

    private void insertReservation(EntityManager em, Orders order, String runId) {
        InventoryItem item = new InventoryItem();
        item.setName("E2E " + runId); item.setItemType("INGREDIENT"); item.setBaseUnit("PIECE");
        item.setInventoryCode(prefix(runId) + "INV"); item.setOnHandQuantity(BigDecimal.TEN); item.setReservedQuantity(BigDecimal.ONE);
        em.persist(item);
        InventoryReservation reservation = new InventoryReservation();
        reservation.setOrder(order); reservation.setStatus("RESERVED"); em.persist(reservation);
        InventoryReservationItem line = new InventoryReservationItem();
        line.setReservation(reservation); line.setInventoryItem(item); line.setQuantity(BigDecimal.ONE); em.persist(line);
        reservation.getItems().add(line);
    }

    private long orderCount(EntityManager em, String code, String status) {
        return ((Number) em.createNativeQuery("SELECT COUNT_BIG(*) FROM Orders WHERE order_code=:code AND order_status=:status")
                .setParameter("code", code).setParameter("status", status).getSingleResult()).longValue();
    }

    private String config(EntityManager em, String key) { return (String) em.createNativeQuery("SELECT config_value FROM ShippingConfig WHERE config_key=:key").setParameter("key", key).getSingleResult(); }
    private void updateConfig(EntityManager em, String key, String value) { em.createNativeQuery("UPDATE ShippingConfig SET config_value=:value WHERE config_key=:key").setParameter("value", value).setParameter("key", key).executeUpdate(); }
    private String prefix(String runId) { return "E2E-" + runId + "-"; }
    private String digits(String runId) { return String.format("%09d", Math.floorMod(runId.hashCode(), 1_000_000_000)); }
    private String requiredEnv(String name) { String value = System.getenv(name); if (value == null || value.isBlank()) throw new IllegalStateException("Required env missing: " + name); return value; }
    private String requiredProperty(String name) { String value = System.getProperty(name); if (value == null || value.isBlank()) throw new IllegalStateException("Required property missing: " + name); return value; }
}
