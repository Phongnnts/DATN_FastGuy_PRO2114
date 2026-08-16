package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dao.CodSettlementDAO;
import entity.CodSettlement;
import entity.User;
import entity.WorkShift;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;

class CodSettlementTransactionFlowTest {
    @Test void submitFlushRaceRollsBackThenReturnsConflict() {
        List<String> events = new ArrayList<>();
        EntityTransaction transaction = transaction(events);
        EntityManager em = entityManager(events, transaction, null, "Violation of UNIQUE KEY constraint 'UQ_CodSettlement_ShipperShift'");
        WorkShift shift = currentShift();
        CodSettlementDAO dao = new CodSettlementDAO() {
            @Override public WorkShift findOwnedShiftForUpdate(EntityManager ignored, int shiftId, int shipperId, LocalDateTime now) {
                events.add("lock:" + now);
                return shift;
            }
            @Override public CodSettlement findByShipperAndShift(EntityManager ignored, int shipperId, int shiftId) { return null; }
            @Override public BigDecimal sumExpectedForShift(EntityManager ignored, int shipperId, WorkShift ignoredShift) { return new BigDecimal("100.00"); }
        };
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 1, 0);
        CodSettlementService service = new CodSettlementService(dao, () -> em, () -> now);

        assertThrows(CodSettlementService.SettlementConflictException.class,
                () -> service.submit(7, 9, new BigDecimal("100.00")));

        assertEquals(List.of("begin", "lock:" + now, "persist", "flush", "rollback", "close"), events);
    }

    @Test void verifySuccessfulFlowChecksAdminLocksSettlementFlushesAndCommits() {
        List<String> events = new ArrayList<>();
        User admin = admin("ADMIN", "ACTIVE");
        CodSettlement settlement = submittedSettlement();
        EntityTransaction transaction = transaction(events);
        EntityManager em = entityManager(events, transaction, admin, null);
        CodSettlementService service = serviceForVerify(events, em, settlement);

        service.verify(3, 11, "SUBMITTED", "SETTLED", new BigDecimal("100.00"), null);

        assertEquals(List.of("begin", "admin:PESSIMISTIC_READ", "settlement:PESSIMISTIC_WRITE", "flush", "commit", "close"), events);
        assertEquals("SETTLED", settlement.getStatus());
        assertEquals(admin, settlement.getReceivedBy());
    }

    @Test void verifyRejectsInactiveAdminBeforeSettlementLookupAndRollsBack() {
        List<String> events = new ArrayList<>();
        EntityTransaction transaction = transaction(events);
        EntityManager em = entityManager(events, transaction, admin("ADMIN", "INACTIVE"), null);
        CodSettlementService service = serviceForVerify(events, em, submittedSettlement());

        assertThrows(SecurityException.class,
                () -> service.verify(3, 11, "SUBMITTED", "SETTLED", new BigDecimal("100.00"), null));

        assertEquals(List.of("begin", "admin:PESSIMISTIC_READ", "rollback", "close"), events);
    }

    @Test void verifyRejectsTerminalSettlementThroughPublicFlowAndRollsBack() {
        List<String> events = new ArrayList<>();
        CodSettlement settlement = submittedSettlement();
        settlement.setStatus("SETTLED");
        EntityTransaction transaction = transaction(events);
        EntityManager em = entityManager(events, transaction, admin("ADMIN", "ACTIVE"), null);
        CodSettlementService service = serviceForVerify(events, em, settlement);

        assertThrows(CodSettlementService.SettlementConflictException.class,
                () -> service.verify(3, 11, "SETTLED", "SETTLED", new BigDecimal("100.00"), null));

        assertEquals(List.of("begin", "admin:PESSIMISTIC_READ", "settlement:PESSIMISTIC_WRITE", "rollback", "close"), events);
        assertEquals("SETTLED", settlement.getStatus());
    }

    @Test void verifyRejectsExpectedStatusMismatchBeforeFlushAndCommit() {
        List<String> events = new ArrayList<>();
        CodSettlement settlement = submittedSettlement();
        EntityTransaction transaction = transaction(events);
        EntityManager em = entityManager(events, transaction, admin("ADMIN", "ACTIVE"), null);
        CodSettlementService service = serviceForVerify(events, em, settlement);

        assertThrows(CodSettlementService.SettlementConflictException.class,
                () -> service.verify(3, 11, "SETTLED", "SETTLED", new BigDecimal("100.00"), null));

        assertEquals(List.of("begin", "admin:PESSIMISTIC_READ", "settlement:PESSIMISTIC_WRITE", "rollback", "close"), events);
        assertEquals("SUBMITTED", settlement.getStatus());
    }

    @Test void verifyFlushFailureRollsBack() {
        List<String> events = new ArrayList<>();
        EntityTransaction transaction = transaction(events);
        EntityManager em = entityManager(events, transaction, admin("ADMIN", "ACTIVE"), "flush failed");
        CodSettlementService service = serviceForVerify(events, em, submittedSettlement());

        assertThrows(PersistenceException.class,
                () -> service.verify(3, 11, "SUBMITTED", "SETTLED", new BigDecimal("100.00"), null));

        assertEquals(List.of("begin", "admin:PESSIMISTIC_READ", "settlement:PESSIMISTIC_WRITE", "flush", "rollback", "close"), events);
    }

    private static CodSettlementService serviceForVerify(List<String> events, EntityManager em, CodSettlement settlement) {
        CodSettlementDAO dao = new CodSettlementDAO() {
            @Override public CodSettlement findForUpdate(EntityManager ignored, int settlementId) {
                events.add("settlement:PESSIMISTIC_WRITE");
                return settlement;
            }
        };
        return new CodSettlementService(dao, () -> em, () -> LocalDateTime.of(2026, 8, 15, 1, 0));
    }

    private static EntityTransaction transaction(List<String> events) {
        boolean[] active = {false};
        return (EntityTransaction) Proxy.newProxyInstance(EntityTransaction.class.getClassLoader(), new Class<?>[] {EntityTransaction.class}, (proxy, method, args) -> switch (method.getName()) {
            case "begin" -> { active[0] = true; events.add("begin"); yield null; }
            case "isActive" -> active[0];
            case "commit" -> { active[0] = false; events.add("commit"); yield null; }
            case "rollback" -> { active[0] = false; events.add("rollback"); yield null; }
            default -> null;
        });
    }

    private static EntityManager entityManager(List<String> events, EntityTransaction transaction, User admin, String flushFailure) {
        return (EntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(), new Class<?>[] {EntityManager.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getTransaction" -> transaction;
            case "find" -> {
                if (args[0] == User.class) {
                    events.add("admin:" + args[2]);
                    if (args[2] != LockModeType.PESSIMISTIC_READ) throw new AssertionError("Admin lock mode");
                    yield admin;
                }
                yield null;
            }
            case "persist" -> { events.add("persist"); yield null; }
            case "flush" -> {
                events.add("flush");
                if (flushFailure != null) throw new PersistenceException(flushFailure);
                yield null;
            }
            case "close" -> { events.add("close"); yield null; }
            default -> null;
        });
    }

    private static User admin(String role, String status) {
        User admin = new User();
        admin.setRole(role);
        admin.setStatus(status);
        return admin;
    }

    private static CodSettlement submittedSettlement() {
        CodSettlement settlement = new CodSettlement();
        settlement.setShipper(currentShift().getUser());
        settlement.setShift(currentShift());
        settlement.setStatus("SUBMITTED");
        settlement.setSubmittedAmount(new BigDecimal("100.00"));
        return settlement;
    }

    private static WorkShift currentShift() {
        User shipper = new User();
        shipper.setUserId(7);
        shipper.setRole("SHIPPER");
        shipper.setStatus("ACTIVE");
        WorkShift shift = new WorkShift();
        shift.setShiftId(9);
        shift.setUser(shipper);
        shift.setShiftDate(LocalDate.of(2026, 8, 14));
        shift.setStartTime(LocalTime.of(22, 0));
        shift.setEndTime(LocalTime.of(6, 0));
        shift.setStatus("CHECKED_IN");
        shift.setCheckInAt(LocalDateTime.of(2026, 8, 14, 22, 0));
        return shift;
    }
}
