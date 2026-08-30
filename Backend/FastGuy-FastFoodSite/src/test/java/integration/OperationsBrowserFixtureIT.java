package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import entity.User;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;
import utils.PasswordUtil;

class OperationsBrowserFixtureIT {
    private static final List<String> DATABASES = List.of("FastGuyDB_Operations060_Test", "FastGuyDB_Attendance061_Test", "FastGuyDB_PayRate062_Test", "FastGuyDB_ActivityLog063_Test");
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Test
    void runFixtureAction() {
        String action = requiredProperty("e2e.action");
        String runId = requiredEnv("FASTGUY_E2E_RUN_ID");
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            verifyTarget(em);
            if ("seed".equals(action)) seed(em, runId);
            else if ("cleanup".equals(action)) cleanup(em, runId);
            else throw new IllegalArgumentException("Unsupported e2e.action");
        } finally {
            em.close();
            DatabaseUtil.close();
        }
    }

    private void verifyTarget(EntityManager em) {
        assertEquals("true", requiredEnv("FASTGUY_DISPOSABLE_DB").toLowerCase());
        Object[] identity = (Object[]) em.createNativeQuery("SELECT @@SERVERNAME,DB_NAME(),DATABASEPROPERTYEX(DB_NAME(),'Status'),CAST(compatibility_level AS int) FROM sys.databases WHERE name=DB_NAME()").getSingleResult();
        assertTrue(DATABASES.contains(identity[1]));
        assertEquals(identity[1], requiredEnv("FASTGUY_E2E_DB_NAME"));
        assertEquals("ONLINE", identity[2]);
        assertTrue(((Number) identity[3]).intValue() >= 160);
        assertEquals(2L, ((Number) em.createNativeQuery("SELECT COUNT_BIG(*) FROM SchemaMigrationHistory WHERE migration_id IN ('059_shift_schedule_order_timeout','060_operating_finance')").getSingleResult()).longValue());
    }

    private void seed(EntityManager em, String runId) {
        cleanup(em, runId);
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE).withNano(0);
        String password = requiredEnv("FASTGUY_E2E_STAFF_PASSWORD");
        em.getTransaction().begin();
        User admin = user(em, "ADMIN", requiredEnv("FASTGUY_E2E_ADMIN_EMAIL"), "Admin Operations", password, runId, 1);
        User staff = user(em, "STAFF", requiredEnv("FASTGUY_E2E_STAFF_EMAIL"), "Staff Operations", password, runId, 2);
        user(em, "USER", requiredEnv("FASTGUY_E2E_USER_EMAIL"), "User Operations", password, runId, 3);
        LocalDate monday = now.toLocalDate().minusDays(now.getDayOfWeek().getValue() - 1L);
        for (int day = 0; day < 7; day++) for (int slot = 0; slot < 3; slot++) {
            LocalTime start = List.of(LocalTime.of(8, 0), LocalTime.of(12, 0), LocalTime.of(16, 0)).get(slot);
            LocalTime end = List.of(LocalTime.of(12, 0), LocalTime.of(16, 0), LocalTime.of(21, 0)).get(slot);
            LocalDate shiftDate = monday.plusDays(day);
            boolean current = shiftDate.equals(now.toLocalDate()) && !now.toLocalTime().isBefore(start) && now.toLocalTime().isBefore(end);
            String code = List.of("MORNING", "AFTERNOON", "EVENING").get(slot);
            int existing = ((Number) em.createNativeQuery("SELECT COUNT_BIG(*) FROM WorkShift WHERE shift_date=:date AND shift_code=:code AND staff_role_snapshot='STAFF'")
                    .setParameter("date", shiftDate).setParameter("code", code).getSingleResult()).intValue();
            if (current && existing > 0) {
                em.createNativeQuery("UPDATE WorkShift SET user_id=:user,status='CHECKED_IN',check_in_at=:checkInAt,check_in_source='MANUAL',check_out_at=NULL,check_out_source=NULL WHERE shift_date=:date AND shift_code=:code AND staff_role_snapshot='STAFF'")
                        .setParameter("user", staff.getUserId()).setParameter("checkInAt", now).setParameter("date", shiftDate).setParameter("code", code).executeUpdate();
            } else if (existing == 0) {
                em.createNativeQuery("INSERT INTO WorkShift(user_id,shift_date,start_time,end_time,shift_code,status,staff_role_snapshot,check_in_at,check_in_source) VALUES (:user,:date,:start,:end,:code,:status,'STAFF',:checkInAt,:checkInSource)")
                        .setParameter("user", staff.getUserId()).setParameter("date", shiftDate).setParameter("start", start).setParameter("end", end)
                        .setParameter("code", code).setParameter("status", current ? "CHECKED_IN" : "SCHEDULED")
                        .setParameter("checkInAt", current ? now : null).setParameter("checkInSource", current ? "MANUAL" : null).executeUpdate();
            }
        }
        em.createNativeQuery("INSERT INTO Orders(order_code,customer_name,customer_phone,customer_address,total_amount,shipping_fee,service_fee,discount_amount,final_amount,payment_method,payment_status,order_status,created_at,updated_at,status_entered_at,staff_id) VALUES (:code,'E2E','000','E2E',1,0,0,0,1,'COD','UNPAID','PENDING',:now,:now,:now,:staff)")
                .setParameter("code", "E2E-" + runId + "-TIMEOUT").setParameter("now", now).setParameter("staff", staff.getUserId()).executeUpdate();
        em.createNativeQuery("INSERT INTO Orders(order_code,customer_name,customer_phone,customer_address,total_amount,shipping_fee,service_fee,discount_amount,final_amount,payment_method,payment_status,order_status,created_at,updated_at,status_entered_at,confirmed_at,staff_id) VALUES (:code,'R4 Overdue','000','E2E',100000,0,0,0,100000,'BANK_TRANSFER','PAID','CONFIRMED',:created,:now,:entered,:created,:staff)")
                .setParameter("code", "E2E-" + runId + "-OVERDUE").setParameter("created", now.minusMinutes(30)).setParameter("now", now).setParameter("entered", now.minusMinutes(20)).setParameter("staff", staff.getUserId()).executeUpdate();
        em.createNativeQuery("INSERT INTO Orders(order_code,customer_name,customer_phone,customer_address,total_amount,shipping_fee,service_fee,discount_amount,final_amount,payment_method,payment_status,order_status,created_at,updated_at,status_entered_at,delivery_failed_at,refund_status,refund_amount,staff_id) VALUES (:code,'R4 Multi','000','E2E',90000,0,0,0,90000,'COD','UNPAID','DELIVERY_FAILED',:created,:now,:entered,:failed,'PENDING',90000,:staff)")
                .setParameter("code", "E2E-" + runId + "-MULTI").setParameter("created", now.minusMinutes(50)).setParameter("now", now).setParameter("entered", now.minusMinutes(40)).setParameter("failed", now.minusMinutes(35)).setParameter("staff", staff.getUserId()).executeUpdate();
        em.createNativeQuery("INSERT INTO Orders(order_code,customer_name,customer_phone,customer_address,total_amount,shipping_fee,service_fee,discount_amount,final_amount,payment_method,payment_status,order_status,created_at,updated_at,status_entered_at,cancelled_at,refund_status,refund_amount,cancelled_by,staff_id) VALUES (:code,'R4 Refund','000','E2E',80000,0,0,0,80000,'BANK_TRANSFER','PAID','CANCELLED',:created,:now,:entered,:cancelled,'PENDING',80000,'ADMIN',:staff)")
                .setParameter("code", "E2E-" + runId + "-REFUND").setParameter("created", now.minusMinutes(70)).setParameter("now", now).setParameter("entered", now.minusMinutes(60)).setParameter("cancelled", now.minusMinutes(55)).setParameter("staff", staff.getUserId()).executeUpdate();
        em.createNativeQuery("INSERT INTO OperatingExpense(expense_date,category,description,amount,created_by,created_at,updated_at) VALUES (:date,'SALARY',:description,120,:admin,:now,:now)")
                .setParameter("date", now.toLocalDate()).setParameter("description", "E2E-" + runId + "-SALARY").setParameter("admin", admin.getUserId()).setParameter("now", now).executeUpdate();
        em.createNativeQuery("INSERT INTO OperatingExpense(expense_date,category,description,amount,created_by,created_at,updated_at) VALUES (:date,'RENT',:description,999,:admin,:now,:now)")
                .setParameter("date", now.toLocalDate().minusYears(2)).setParameter("description", "E2E-" + runId + "-OLD").setParameter("admin", admin.getUserId()).setParameter("now", now).executeUpdate();
        em.getTransaction().commit();
        assertTrue(admin.getUserId() > 0);
    }

    private User user(EntityManager em, String role, String email, String name, String password, String runId, int suffix) {
        User user = new User();
        user.setRole(role); user.setEmail(email); user.setPhone("6" + String.format("%08d", Math.abs(runId.hashCode()) % 100000000) + suffix);
        user.setFullName(name); user.setPasswordHash(PasswordUtil.hash(password)); user.setStatus("ACTIVE"); em.persist(user); return user;
    }

    private void cleanup(EntityManager em, String runId) {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
        em.getTransaction().begin();
        String pattern = "%" + runId + "%";
        List<Integer> users = em.createNativeQuery("SELECT user_id FROM Users WHERE email LIKE :pattern", Integer.class).setParameter("pattern", pattern).getResultList();
        em.createNativeQuery("DELETE OperatingExpense WHERE description LIKE :pattern").setParameter("pattern", pattern).executeUpdate();
        em.createNativeQuery("DELETE FixedAsset WHERE asset_name LIKE :pattern").setParameter("pattern", pattern).executeUpdate();
        em.createNativeQuery("DELETE OrderStatusHistory WHERE order_id IN (SELECT order_id FROM Orders WHERE order_code LIKE :pattern)").setParameter("pattern", pattern).executeUpdate();
        em.createNativeQuery("DELETE Orders WHERE order_code LIKE :pattern").setParameter("pattern", pattern).executeUpdate();
        if (!users.isEmpty()) {
            if (hasTable(em,"ActivityLog")) em.createNativeQuery("DELETE ActivityLog WHERE actor_user_id IN (:ids)").setParameter("ids",users).executeUpdate();
            if (hasTable(em,"StaffPayRate")) em.createNativeQuery("DELETE StaffPayRate WHERE user_id IN (:ids) OR created_by IN (:ids)").setParameter("ids",users).executeUpdate();
            em.createNativeQuery("DELETE WorkShift WHERE user_id IN (:ids)").setParameter("ids", users).executeUpdate();
            em.createNativeQuery("DELETE CartItem WHERE cart_id IN (SELECT cart_id FROM Cart WHERE user_id IN (:ids))").setParameter("ids", users).executeUpdate();
            em.createNativeQuery("DELETE Cart WHERE user_id IN (:ids)").setParameter("ids", users).executeUpdate();
            em.createNativeQuery("DELETE Users WHERE user_id IN (:ids)").setParameter("ids", users).executeUpdate();
        }
        em.getTransaction().commit();
        Number remaining = (Number) em.createNativeQuery("SELECT (SELECT COUNT_BIG(*) FROM Users WHERE email LIKE :pattern)+(SELECT COUNT_BIG(*) FROM Orders WHERE order_code LIKE :pattern)+(SELECT COUNT_BIG(*) FROM OperatingExpense WHERE description LIKE :pattern)+(SELECT COUNT_BIG(*) FROM FixedAsset WHERE asset_name LIKE :pattern)").setParameter("pattern", pattern).getSingleResult();
        assertEquals(0L, remaining.longValue(), "Operations browser cleanup must remove every fixture row");
    }

    private boolean hasTable(EntityManager em,String name){return ((Number)em.createNativeQuery("SELECT COUNT_BIG(*) FROM sys.tables WHERE name=:name").setParameter("name",name).getSingleResult()).longValue()>0;}
    private String requiredEnv(String name) { String value = System.getenv(name); if (value == null || value.isBlank()) throw new IllegalStateException(name + " required"); return value; }
    private String requiredProperty(String name) { String value = System.getProperty(name); if (value == null || value.isBlank()) throw new IllegalStateException(name + " required"); return value; }
}
