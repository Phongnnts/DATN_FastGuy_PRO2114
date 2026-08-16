package dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import entity.CodSettlement;
import entity.WorkShift;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

public class CodSettlementDAO {
    public record ShiftWindow(LocalDateTime start, LocalDateTime end) {}

    public static ShiftWindow shiftWindow(WorkShift shift) {
        LocalDateTime start = LocalDateTime.of(shift.getShiftDate(), shift.getStartTime());
        LocalDateTime end = LocalDateTime.of(shift.getShiftDate(), shift.getEndTime());
        if (!shift.getEndTime().isAfter(shift.getStartTime())) end = end.plusDays(1);
        return new ShiftWindow(start, end);
    }

    public WorkShift findOwnedShiftForUpdate(EntityManager em, int shiftId, int shipperId) {
        return findOwnedShiftForUpdate(em, shiftId, shipperId, LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
    }

    public WorkShift findOwnedShiftForUpdate(EntityManager em, int shiftId, int shipperId, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        List<WorkShift> shifts = em.createQuery("SELECT ws FROM WorkShift ws WHERE ws.shiftId = :shiftId AND ws.user.userId = :shipperId AND ws.user.role = 'SHIPPER' AND ws.user.status = 'ACTIVE' AND ws.status = 'CHECKED_IN' AND ws.checkInAt IS NOT NULL AND ws.checkOutAt IS NULL AND ws.shiftDate IN :shiftDates", WorkShift.class)
                .setParameter("shiftId", shiftId)
                .setParameter("shipperId", shipperId)
                .setParameter("shiftDates", List.of(today, today.minusDays(1)))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        return shifts.isEmpty() ? null : shifts.get(0);
    }

    public CodSettlement findByShipperAndShift(EntityManager em, int shipperId, int shiftId) {
        List<CodSettlement> settlements = em.createQuery("SELECT cs FROM CodSettlement cs WHERE cs.shipper.userId = :shipperId AND cs.shift.shiftId = :shiftId", CodSettlement.class)
                .setParameter("shipperId", shipperId)
                .setParameter("shiftId", shiftId)
                .getResultList();
        return settlements.isEmpty() ? null : settlements.get(0);
    }

    public CodSettlement findForUpdate(EntityManager em, int settlementId) {
        return em.find(CodSettlement.class, settlementId, LockModeType.PESSIMISTIC_WRITE);
    }

    public BigDecimal sumExpectedForShift(EntityManager em, int shipperId, WorkShift shift) {
        ShiftWindow window = shiftWindow(shift);
        BigDecimal result = em.createQuery(
                "SELECT SUM(o.codCollectedAmount) FROM Orders o WHERE o.shipper.userId = :shipperId " +
                "AND o.paymentMethod = 'COD' AND o.orderStatus = 'DELIVERED' AND o.codCollectedAmount IS NOT NULL " +
                "AND o.deliveredAt >= :start AND o.deliveredAt < :end", BigDecimal.class)
                .setParameter("shipperId", shipperId)
                .setParameter("start", window.start())
                .setParameter("end", window.end())
                .getSingleResult();
        return result == null ? BigDecimal.ZERO : result;
    }

    public List<CodSettlement> listByShipper(int shipperId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT cs FROM CodSettlement cs WHERE cs.shipper.userId = :shipperId ORDER BY cs.submittedAt DESC, cs.settlementId DESC", CodSettlement.class)
                    .setParameter("shipperId", shipperId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<CodSettlement> listByStatus(String status) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            String jpql = "SELECT cs FROM CodSettlement cs" + (status == null || status.isBlank() ? "" : " WHERE cs.status = :status") + " ORDER BY cs.submittedAt DESC, cs.settlementId DESC";
            var query = em.createQuery(jpql, CodSettlement.class);
            if (status != null && !status.isBlank()) query.setParameter("status", status);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    public BigDecimal sumPendingAmount() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            BigDecimal result = em.createQuery("SELECT SUM(cs.submittedAmount) FROM CodSettlement cs WHERE cs.status = 'SUBMITTED'", BigDecimal.class).getSingleResult();
            return result == null ? BigDecimal.ZERO : result;
        } finally {
            em.close();
        }
    }

    public long countPending() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(cs) FROM CodSettlement cs WHERE cs.status = 'SUBMITTED'", Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }
}
