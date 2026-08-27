package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import entity.Orders;
import entity.User;
import entity.WorkShift;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;
import utils.PasswordUtil;

class StaffOwnershipBrowserFixtureIT {
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
                case "race" -> assertSeeded(em, runId, "RACE");
                case "recovery-terminal" -> assertSeeded(em, runId, "RECOVERY");
                case "admin" -> assertSeeded(em, runId, "ADMIN-PENDING");
                case "transfer" -> transferCurrentOwnership(em, runId);
                case "verify-recovery" -> verifyUnowned(em, runId, "RECOVERY", "PICKED_UP");
                case "verify-terminal" -> verifyUnowned(em, runId, "TERMINAL", "CANCELLED");
                case "verify-admin" -> verifyUnowned(em, runId, "ADMIN-PENDING", "CONFIRMED");
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
        Object[] identity = (Object[]) em.createNativeQuery("SELECT @@SERVERNAME,DB_NAME(),DATABASEPROPERTYEX(DB_NAME(),'Status'),CAST(compatibility_level AS int) FROM sys.databases WHERE name=DB_NAME()").getSingleResult();
        assertEquals("DuckJo", identity[0]);
        assertEquals("FastGuyDB_Inventory054_Test", identity[1]);
        assertEquals(requiredEnv("FASTGUY_E2E_DB_NAME"), identity[1]);
        assertEquals("ONLINE", identity[2]);
        assertEquals(160, ((Number) identity[3]).intValue());
        assertEquals(1L, ((Number) em.createNativeQuery("SELECT COUNT_BIG(*) FROM sys.columns WHERE object_id=OBJECT_ID('dbo.Orders') AND name='staff_shift_id'").getSingleResult()).longValue());
        System.out.println("Staff ownership browser target verified: DuckJo/FastGuyDB_Inventory054_Test ONLINE compatibility 160 migration 058");
    }

    private void seed(EntityManager em, String runId) {
        cleanup(em, runId);
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE).withNano(0);
        String password = requiredEnv("FASTGUY_E2E_STAFF_PASSWORD");
        em.getTransaction().begin();
        User current = user(em, "STAFF", "ownership-current-" + runId + "@test.local", "Current Ownership Staff", password, "71" + digits(runId));
        User other = user(em, "STAFF", "ownership-other-" + runId + "@test.local", "Other Ownership Staff", password, "72" + digits(runId));
        User shipper = user(em, "SHIPPER", "ownership-shipper-" + runId + "@test.local", "Ownership Shipper", password, "73" + digits(runId));
        User admin = user(em, "ADMIN", "ownership-admin-" + runId + "@test.local", "Ownership Admin", password, "74" + digits(runId));
        WorkShift currentShift = shift(em, current, now);
        WorkShift otherShift = shift(em, other, now);
        shift(em, shipper, now);
        order(em, runId, "PENDING", "PENDING", null, null, null);
        order(em, runId, "OWN-CONFIRMED", "CONFIRMED", current, currentShift, null);
        order(em, runId, "UNOWNED-PREPARING", "PREPARING", null, null, null);
        order(em, runId, "OTHER-READY", "READY", other, otherShift, null);
        order(em, runId, "RACE", "READY", other, otherShift, null);
        order(em, runId, "RECOVERY", "DELIVERY_FAILED", other, otherShift, null);
        order(em, runId, "TERMINAL", "DELIVERY_FAILED", other, otherShift, null);
        order(em, runId, "ASSIGNED", "ASSIGNED", null, null, shipper);
        order(em, runId, "ADMIN-PENDING", "PENDING", null, null, null);
        em.getTransaction().commit();
        System.out.println("Staff ownership browser seed complete: " + runId);
    }

    private User user(EntityManager em, String role, String email, String name, String password, String phone) {
        User user = new User();
        user.setRole(role); user.setEmail(email); user.setFullName(name); user.setPhone(phone.substring(0, Math.min(10, phone.length())));
        user.setPasswordHash(PasswordUtil.hash(password)); user.setStatus("ACTIVE"); em.persist(user); return user;
    }

    private WorkShift shift(EntityManager em, User user, LocalDateTime now) {
        WorkShift shift = new WorkShift();
        shift.setUser(user); shift.setShiftDate(now.toLocalDate()); shift.setStartTime(now.minusHours(2).toLocalTime());
        shift.setEndTime(now.minusMinutes(1).toLocalTime()); shift.setCheckInAt(now.minusHours(1)); shift.setStatus("CHECKED_IN"); em.persist(shift); return shift;
    }

    private void order(EntityManager em, String runId, String suffix, String status, User staff, WorkShift staffShift, User shipper) {
        Orders order = new Orders();
        order.setOrderCode(prefix(runId) + suffix); order.setCustomerName("Ownership E2E"); order.setCustomerPhone("0900000000");
        order.setCustomerAddress("Disposable Test"); order.setTotalAmount(BigDecimal.TEN); order.setShippingFee(BigDecimal.ZERO);
        order.setServiceFee(BigDecimal.ZERO); order.setDiscountAmount(BigDecimal.ZERO); order.setFinalAmount(BigDecimal.TEN);
        order.setPaymentMethod("BANK_TRANSFER"); order.setPaymentStatus("PAID"); order.setOrderStatus(status);
        order.setCreatedAt(LocalDateTime.now(BUSINESS_ZONE).minusMinutes(10)); order.setStaff(staff); order.setStaffShift(staffShift);
        if (shipper != null) { order.setShipper(shipper); order.setAssignedAt(LocalDateTime.now(BUSINESS_ZONE).minusMinutes(5)); }
        if ("DELIVERY_FAILED".equals(status)) { order.setDeliveryFailureCode("CUSTOMER_UNREACHABLE"); order.setFailureReason("Ownership E2E"); order.setDeliveryFailedAt(LocalDateTime.now(BUSINESS_ZONE).minusMinutes(5)); order.setDeliveryAttemptCount(1); order.setDeliveryAttemptLimit(3); }
        em.persist(order);
    }

    private void assertSeeded(EntityManager em, String runId, String suffix) {
        assertEquals(1L, count(em, "Orders", "order_code", prefix(runId) + suffix));
        System.out.println("Staff ownership browser " + suffix.toLowerCase() + " fixture verified: " + runId);
    }

    private void transferCurrentOwnership(EntityManager em, String runId) {
        em.getTransaction().begin();
        int updated = em.createNativeQuery("UPDATE Orders SET staff_id=(SELECT user_id FROM Users WHERE email=:other),staff_shift_id=(SELECT shift_id FROM WorkShift WHERE user_id=(SELECT user_id FROM Users WHERE email=:other) AND status='CHECKED_IN') WHERE staff_shift_id=(SELECT shift_id FROM WorkShift WHERE user_id=(SELECT user_id FROM Users WHERE email=:current) AND status='CHECKED_IN') AND order_code LIKE :prefix")
                .setParameter("other", "ownership-other-" + runId + "@test.local")
                .setParameter("current", "ownership-current-" + runId + "@test.local")
                .setParameter("prefix", prefix(runId) + "%").executeUpdate();
        em.getTransaction().commit();
        assertTrue(updated > 0);
        System.out.println("Staff ownership browser transfer complete: " + runId);
    }

    private void verifyUnowned(EntityManager em, String runId, String suffix, String status) {
        Number count = (Number) em.createNativeQuery("SELECT COUNT_BIG(*) FROM Orders WHERE order_code=:code AND order_status=:status AND staff_shift_id IS NULL")
                .setParameter("code", prefix(runId) + suffix).setParameter("status", status).getSingleResult();
        assertEquals(1L, count.longValue());
        System.out.println("Staff ownership browser unowned state verified: " + suffix + " " + status);
    }

    private void cleanup(EntityManager em, String runId) {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
        String prefix = prefix(runId);
        List<Integer> orderIds = em.createNativeQuery("SELECT order_id FROM Orders WHERE order_code LIKE :prefix", Integer.class).setParameter("prefix", prefix + "%").getResultList();
        List<Integer> userIds = em.createNativeQuery("SELECT user_id FROM Users WHERE email LIKE :email", Integer.class).setParameter("email", "ownership-%-" + runId + "@test.local").getResultList();
        em.getTransaction().begin();
        if (!orderIds.isEmpty()) {
            em.createNativeQuery("DELETE FROM OrderStatusHistory WHERE order_id IN (:ids)").setParameter("ids", orderIds).executeUpdate();
            em.createNativeQuery("DELETE FROM Orders WHERE order_id IN (:ids)").setParameter("ids", orderIds).executeUpdate();
        }
        if (!userIds.isEmpty()) {
            em.createNativeQuery("DELETE FROM WorkShift WHERE user_id IN (:ids)").setParameter("ids", userIds).executeUpdate();
            em.createNativeQuery("DELETE FROM CartItem WHERE cart_id IN (SELECT cart_id FROM Cart WHERE user_id IN (:ids))").setParameter("ids", userIds).executeUpdate();
            em.createNativeQuery("DELETE FROM Cart WHERE user_id IN (:ids)").setParameter("ids", userIds).executeUpdate();
            em.createNativeQuery("DELETE FROM Users WHERE user_id IN (:ids)").setParameter("ids", userIds).executeUpdate();
        }
        em.getTransaction().commit();
        assertEquals(0L, count(em, "Orders", "order_code", prefix + "%", true));
        assertEquals(0L, count(em, "Users", "email", "ownership-%-" + runId + "@test.local", true));
        System.out.println("Staff ownership browser cleanup verified: Orders=0 Users=0 WorkShift=0 for " + runId);
    }

    private long count(EntityManager em, String table, String column, String value) { return count(em, table, column, value, false); }
    private long count(EntityManager em, String table, String column, String value, boolean like) {
        return ((Number) em.createNativeQuery("SELECT COUNT_BIG(*) FROM " + table + " WHERE " + column + (like ? " LIKE :value" : "=:value")).setParameter("value", value).getSingleResult()).longValue();
    }
    private String prefix(String runId) { return "E2E-OWN-" + runId + "-"; }
    private String digits(String runId) { return String.format("%08d", Math.floorMod(runId.hashCode(), 100_000_000)); }
    private String requiredEnv(String name) { String value = System.getenv(name); if (value == null || value.isBlank()) throw new IllegalStateException("Required env missing: " + name); return value; }
    private String requiredProperty(String name) { String value = System.getProperty(name); if (value == null || value.isBlank()) throw new IllegalStateException("Required property missing: " + name); return value; }
}
