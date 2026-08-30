package integration;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import service.*;
import utils.DatabaseUtil;

class OperationsFinanceIT {
    private static final String DATABASE = "FastGuyDB_Operations060_Test";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final String token = "OPS060-" + Long.toUnsignedString(System.nanoTime());
    private final List<Integer> users = new ArrayList<>(), shifts = new ArrayList<>(), orders = new ArrayList<>(), items = new ArrayList<>();
    private final List<Integer> expenses = new ArrayList<>(), assets = new ArrayList<>();

    @Test
    void disposableOperationsAndFinanceUseRealTransactions() throws Throwable {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("FASTGUY_DISPOSABLE_DB")),
                "Set FASTGUY_DISPOSABLE_DB=true only for the approved disposable database");
        EntityManager em = DatabaseUtil.getEntityManager();
        Throwable failure = null;
        try {
            verifyTarget(em);
            LocalDateTime businessNow = LocalDateTime.now(BUSINESS_ZONE).withNano(0);
            Assumptions.assumeTrue(!businessNow.toLocalTime().isBefore(LocalTime.of(0, 2))
                            && businessNow.toLocalTime().isBefore(LocalTime.of(23, 58)),
                    "This real-transaction fixture must not cross the business date during manual check-in");
            LocalDate today = businessNow.toLocalDate();
            int manualUser = user(em, "SHIPPER", "Manual");
            LocalTime[] manualWindow = manualCheckInWindow(businessNow.toLocalTime());
            int manual = shift(em, manualUser, today, manualWindow[0], manualWindow[1], "EVENING", "SCHEDULED", null);
            new WorkShiftService().check(manual, manualUser, true);
            assertShift(em, manual, "CHECKED_IN", "MANUAL");

            int morningUser = user(em, "STAFF", "Morning");
            int afternoonUser = user(em, "STAFF", "Afternoon");
            int missedUser = user(em, "STAFF", "Missed");
            int admin = user(em, "ADMIN", "Finance");

            int morning = shift(em, morningUser, today, LocalTime.of(8, 0), LocalTime.NOON, "MORNING", "CHECKED_IN", "MANUAL");
            int afternoon = shift(em, afternoonUser, today, LocalTime.NOON, LocalTime.of(16, 0), "AFTERNOON", "CHECKED_IN", "AUTO");
            int missed = shift(em, missedUser, today.minusDays(1), LocalTime.of(8, 0), LocalTime.NOON, "MORNING", "SCHEDULED", null);
            assertScheduledWithoutCheckIn(em, missed);

            int owned = order(em, "PREPARING", "PAID", LocalDateTime.now(), morningUser, morning);
            assertEquals(1, new ShiftRolloverService().rollover(morning, LocalDateTime.now()));
            assertOwnership(em, owned, afternoonUser, afternoon);
            assertEquals(1L, count(em, "OrderStatusHistory", "order_id=:id AND from_status='PREPARING' AND to_status='PREPARING'", owned));
            assertEquals(0, new ShiftRolloverService().rollover(morning, LocalDateTime.now()));
            assertEquals(1L, count(em, "OrderStatusHistory", "order_id=:id AND from_status='PREPARING' AND to_status='PREPARING'", owned));

            LocalDate isolatedDate = today.plusYears(20);
            int isolatedMorning = shift(em, morningUser, isolatedDate, LocalTime.of(8, 0), LocalTime.NOON, "MORNING", "CHECKED_IN", "MANUAL");
            int keepsOwner = order(em, "CONFIRMED", "PAID", LocalDateTime.now(), morningUser, isolatedMorning);
            assertEquals(0, new ShiftRolloverService().rollover(isolatedMorning, LocalDateTime.now()));
            assertOwnership(em, keepsOwner, morningUser, isolatedMorning);

            int inventory = inventory(em);
            int expired = order(em, "PENDING", "UNPAID", LocalDateTime.now().minusMinutes(20), null, null);
            reservation(em, expired, inventory, "RESERVED");
            assertNotNull(new OrderExpiryService().cancelExpired(expired, LocalDateTime.now()));
            assertOrder(em, expired, "CANCELLED");
            assertEquals("RELEASED", scalar(em, "SELECT status FROM InventoryReservation WHERE order_id=:id", expired));

            for (String status : List.of("PREPARING", "READY")) {
                int consumed = order(em, status, "PAID", LocalDateTime.now(), morningUser, morning);
                reservation(em, consumed, inventory, "CONSUMED");
                BigDecimal before = quantity(em, inventory);
                assertNotNull(new OrderTransitionService().cancel(consumed, null, null, false, "ADMIN", admin, token));
                assertEquals("WASTED", scalar(em, "SELECT status FROM InventoryReservation WHERE order_id=:id", consumed));
                assertEquals(0, before.compareTo(quantity(em, inventory)));
                assertEquals(1L, count(em, "InventoryTransaction", "order_id=:id AND transaction_type='WASTE'", consumed));
                assertNull(new OrderTransitionService().cancel(consumed, null, null, false, "ADMIN", admin, token));
                assertEquals(1L, count(em, "InventoryTransaction", "order_id=:id AND transaction_type='WASTE'", consumed));
            }

            int freshReady = cancellable(em, "READY", inventory, LocalDateTime.now());
            int assigned = cancellable(em, "ASSIGNED", inventory, LocalDateTime.now().minusHours(1));
            int picked = cancellable(em, "PICKED_UP", inventory, LocalDateTime.now().minusHours(1));
            assertNotNull(new OrderExpiryService().cancelAtCutoff(freshReady, LocalDateTime.now()));
            assertNull(new OrderExpiryService().cancelAtCutoff(assigned, LocalDateTime.now()));
            assertNull(new OrderExpiryService().cancelAtCutoff(picked, LocalDateTime.now()));
            for (int id : List.of(assigned, picked)) for (String actor : List.of("USER", "CUSTOMER", "STAFF", "ADMIN", "SHIPPER", "SYSTEM"))
                assertNull(new OrderTransitionService().cancel(id, null, null, false, actor, admin, token), actor);
            assertOrder(em, assigned, "ASSIGNED");
            assertOrder(em, picked, "PICKED_UP");

            LocalDate reportDate = LocalDate.of(2090, 1, 15);
            OperatingFinanceService finance = new OperatingFinanceService();
            Map<String,Object> expense = finance.createExpense(reportDate, "RENT", token, new BigDecimal("120.00"), admin);
            Map<String,Object> asset = finance.createAsset(token, new BigDecimal("1200.00"), BigDecimal.ZERO, LocalDate.of(2090, 1, 1), 12, admin);
            expenses.add((Integer) expense.get("expenseId"));
            assets.add((Integer) asset.get("assetId"));
            assertEquals(new BigDecimal("120.00"), scalar(em, "SELECT amount FROM OperatingExpense WHERE expense_id=:id", expenses.get(0)));
            assertEquals(new BigDecimal("1200.00"), scalar(em, "SELECT acquisition_cost FROM FixedAsset WHERE asset_id=:id", assets.get(0)));
            Map<String,Object> report = finance.operatingProfit(LocalDate.of(2090, 1, 1), LocalDate.of(2090, 1, 31));
            assertEquals(new BigDecimal("120.00"), report.get("operatingExpenses"));
            assertEquals(new BigDecimal("101.92"), report.get("depreciation"));
            assertEquals(new BigDecimal("-221.92"), report.get("operatingProfit"));
            assertEquals(Boolean.TRUE, report.get("costComplete"));
        } catch (Throwable t) {
            failure = t;
            throw t;
        } finally {
            cleanupPreserving(em, failure);
            DatabaseUtil.close();
        }
    }

    static LocalTime[] manualCheckInWindow(LocalTime current) {
        if (current.isBefore(LocalTime.of(0, 20))) return new LocalTime[] { LocalTime.of(0, 15), LocalTime.of(0, 45) };
        LocalTime lateEnd = LocalTime.MAX.minusMinutes(15);
        if (current.isAfter(lateEnd.minusMinutes(30))) return new LocalTime[] { lateEnd.minusMinutes(30), lateEnd };
        return new LocalTime[] { current.minusMinutes(5), current.plusMinutes(30) };
    }

    private void verifyTarget(EntityManager em) {
        Object[] row = (Object[]) em.createNativeQuery("SELECT @@SERVERNAME,DB_NAME(),DATABASEPROPERTYEX(DB_NAME(),'Status'),CAST(compatibility_level AS int) FROM sys.databases WHERE name=DB_NAME()").getSingleResult();
        assertEquals(DATABASE, row[1]);
        assertEquals("ONLINE", row[2]);
        assertTrue(((Number) row[3]).intValue() >= 160);
        assertEquals(2L, ((Number) em.createNativeQuery("SELECT COUNT_BIG(*) FROM SchemaMigrationHistory WHERE migration_id IN ('059_shift_schedule_order_timeout','060_operating_finance')").getSingleResult()).longValue());
        assertEquals(2L, ((Number) em.createNativeQuery("SELECT COUNT_BIG(*) FROM sys.tables WHERE object_id IN (OBJECT_ID('dbo.OperatingExpense'),OBJECT_ID('dbo.FixedAsset'))").getSingleResult()).longValue());
        System.out.println("OperationsFinanceIT target verified: " + row[0] + "/" + row[1]);
    }

    private int user(EntityManager em, String role, String name) {
        tx(em);
        int id = id(em, "INSERT INTO Users(role_name,email,phone,password_hash,full_name,status,favorite_ids_json,created_at,updated_at) OUTPUT INSERTED.user_id VALUES (:role,:email,:phone,'test',:name,'ACTIVE',N'[]',SYSDATETIME(),SYSDATETIME())",
                Map.of("role", role, "email", token + users.size() + "@test.local", "phone", "7" + String.format("%09d", users.size()), "name", token + name));
        commit(em); users.add(id); return id;
    }

    private int shift(EntityManager em, int user, LocalDate date, LocalTime start, LocalTime end, String code, String status, String source) {
        tx(em);
        Map<String, Object> values = new HashMap<>();
        values.put("user", user); values.put("date", date); values.put("start", start); values.put("end", end);
        values.put("code", code); values.put("status", status); values.put("source", source);
        int id = id(em, "INSERT INTO WorkShift(user_id,shift_date,start_time,end_time,shift_code,check_in_at,check_in_source,status) OUTPUT INSERTED.shift_id VALUES (:user,:date,:start,:end,:code,CASE WHEN :status='CHECKED_IN' THEN SYSDATETIME() END,:source,:status)", values);
        commit(em); shifts.add(id); return id;
    }

    private int order(EntityManager em, String status, String payment, LocalDateTime entered, Integer staff, Integer shift) {
        tx(em);
        String sql = "DECLARE @ids TABLE(id int); INSERT INTO Orders(order_code,customer_name,customer_phone,customer_address,total_amount,shipping_fee,service_fee,discount_amount,final_amount,payment_method,payment_status,order_status,created_at,updated_at,status_entered_at,staff_id,staff_shift_id) OUTPUT INSERTED.order_id INTO @ids VALUES (:code,'Test','000','Test',1,0,0,0,1,'BANK_TRANSFER',:payment,:status,:created,:created,:entered,:staff,:shift); SELECT id FROM @ids";
        Map<String,Object> p = new HashMap<>(); p.put("code", token + "-" + orders.size()); p.put("payment", payment); p.put("status", status); p.put("created", entered); p.put("entered", entered); p.put("staff", staff); p.put("shift", shift);
        int id = id(em, sql, p); commit(em); orders.add(id); return id;
    }

    private int cancellable(EntityManager em, String status, int inventory, LocalDateTime entered) { int id=order(em,status,"PAID",entered,null,null); reservation(em,id,inventory,"RESERVED"); return id; }

    private int inventory(EntityManager em) {
        tx(em); int id=id(em,"INSERT INTO InventoryItem(name,item_type,base_unit,inventory_code,on_hand_quantity,reserved_quantity,minimum_quantity,active,average_unit_cost) OUTPUT INSERTED.inventory_item_id VALUES (:name,'INGREDIENT','PIECE',:code,100,10,0,1,2)",Map.of("name",token,"code",token)); commit(em); items.add(id); return id;
    }

    private void reservation(EntityManager em, int order, int item, String status) {
        tx(em); int reservation=id(em,"INSERT INTO InventoryReservation(order_id,status) OUTPUT INSERTED.reservation_id VALUES (:order,:status)",Map.of("order",order,"status",status));
        em.createNativeQuery("INSERT INTO InventoryReservationItem(reservation_id,inventory_item_id,quantity) VALUES (:reservation,:item,1)").setParameter("reservation",reservation).setParameter("item",item).executeUpdate(); commit(em);
    }

    private int id(EntityManager em, String sql, Map<String,?> values) { var q=em.createNativeQuery(sql); values.forEach(q::setParameter); return ((Number)q.getSingleResult()).intValue(); }
    private Object scalar(EntityManager em, String sql, int id) { em.clear(); return em.createNativeQuery(sql).setParameter("id",id).getSingleResult(); }
    private BigDecimal quantity(EntityManager em, int id) { return (BigDecimal) scalar(em,"SELECT on_hand_quantity FROM InventoryItem WHERE inventory_item_id=:id",id); }
    private long count(EntityManager em,String table,String where,int id){return ((Number)em.createNativeQuery("SELECT COUNT_BIG(*) FROM "+table+" WHERE "+where).setParameter("id",id).getSingleResult()).longValue();}
    private void assertOrder(EntityManager em,int id,String status){assertEquals(status,scalar(em,"SELECT order_status FROM Orders WHERE order_id=:id",id));}
    private void assertShift(EntityManager em,int id,String status,String source){Object[] r=(Object[])em.createNativeQuery("SELECT status,check_in_source FROM WorkShift WHERE shift_id=:id").setParameter("id",id).getSingleResult();assertEquals(status,r[0]);assertEquals(source,r[1]);}
    private void assertScheduledWithoutCheckIn(EntityManager em,int id){Object[] r=(Object[])em.createNativeQuery("SELECT status,check_in_at,check_in_source FROM WorkShift WHERE shift_id=:id").setParameter("id",id).getSingleResult();assertEquals("SCHEDULED",r[0]);assertNull(r[1]);assertNull(r[2]);}
    private void assertOwnership(EntityManager em,int id,int user,int shift){Object[] r=(Object[])em.createNativeQuery("SELECT staff_id,staff_shift_id FROM Orders WHERE order_id=:id").setParameter("id",id).getSingleResult();assertEquals(user,((Number)r[0]).intValue());assertEquals(shift,((Number)r[1]).intValue());}
    private void deleteShift(EntityManager em,int id){tx(em);em.createNativeQuery("DELETE WorkShift WHERE shift_id=:id").setParameter("id",id).executeUpdate();commit(em);shifts.remove(Integer.valueOf(id));}
    private boolean hasTable(EntityManager em,String name){return ((Number)em.createNativeQuery("SELECT COUNT_BIG(*) FROM sys.tables WHERE name=:name").setParameter("name",name).getSingleResult()).longValue()>0;}
    private void tx(EntityManager em){em.getTransaction().begin();}
    private void commit(EntityManager em){em.getTransaction().commit();}

    private void cleanupPreserving(EntityManager em, Throwable original) {
        RuntimeException cleanupFailure=null;
        try { cleanup(em); } catch(RuntimeException e){cleanupFailure=e;if(original!=null)original.addSuppressed(e);}
        try { em.close(); } catch(RuntimeException e){if(original!=null)original.addSuppressed(e);else if(cleanupFailure!=null)cleanupFailure.addSuppressed(e);else throw e;}
        if(original==null&&cleanupFailure!=null)throw cleanupFailure;
    }

    private void cleanup(EntityManager em) {
        if(em.getTransaction().isActive())em.getTransaction().rollback(); tx(em);
        if(!expenses.isEmpty())em.createNativeQuery("DELETE OperatingExpense WHERE expense_id IN (:ids)").setParameter("ids",expenses).executeUpdate();
        if(!assets.isEmpty())em.createNativeQuery("DELETE FixedAsset WHERE asset_id IN (:ids)").setParameter("ids",assets).executeUpdate();
        if(!orders.isEmpty()){
            em.createNativeQuery("DELETE InventoryTransaction WHERE order_id IN (:ids)").setParameter("ids",orders).executeUpdate();
            em.createNativeQuery("DELETE OrderStatusHistory WHERE order_id IN (:ids)").setParameter("ids",orders).executeUpdate();
            em.createNativeQuery("DELETE InventoryReservationItem WHERE reservation_id IN (SELECT reservation_id FROM InventoryReservation WHERE order_id IN (:ids))").setParameter("ids",orders).executeUpdate();
            em.createNativeQuery("DELETE InventoryReservation WHERE order_id IN (:ids)").setParameter("ids",orders).executeUpdate();
            em.createNativeQuery("DELETE Orders WHERE order_id IN (:ids)").setParameter("ids",orders).executeUpdate();
        }
        if(!items.isEmpty())em.createNativeQuery("DELETE InventoryItem WHERE inventory_item_id IN (:ids)").setParameter("ids",items).executeUpdate();
        if(!shifts.isEmpty())em.createNativeQuery("DELETE WorkShift WHERE shift_id IN (:ids)").setParameter("ids",shifts).executeUpdate();
        if(!users.isEmpty()){em.createNativeQuery("DELETE CartItem WHERE cart_id IN (SELECT cart_id FROM Cart WHERE user_id IN (:ids))").setParameter("ids",users).executeUpdate();em.createNativeQuery("DELETE Cart WHERE user_id IN (:ids)").setParameter("ids",users).executeUpdate();if(hasTable(em,"ActivityLog"))em.createNativeQuery("DELETE ActivityLog WHERE actor_user_id IN (:ids)").setParameter("ids",users).executeUpdate();em.createNativeQuery("DELETE Users WHERE user_id IN (:ids)").setParameter("ids",users).executeUpdate();}
        commit(em);
        long remaining=((Number)em.createNativeQuery("SELECT (SELECT COUNT_BIG(*) FROM Users WHERE email LIKE :token)+(SELECT COUNT_BIG(*) FROM Orders WHERE order_code LIKE :token)+(SELECT COUNT_BIG(*) FROM InventoryItem WHERE inventory_code=:exact)+(SELECT COUNT_BIG(*) FROM OperatingExpense WHERE description=:exact)+(SELECT COUNT_BIG(*) FROM FixedAsset WHERE asset_name=:exact)").setParameter("token",token+"%").setParameter("exact",token).getSingleResult()).longValue();
        assertEquals(0L,remaining,"Integration cleanup must remove every fixture");
        System.out.println("OperationsFinanceIT cleanup verified: 0 tracked rows");
    }
}
