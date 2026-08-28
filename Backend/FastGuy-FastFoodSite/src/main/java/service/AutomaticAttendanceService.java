package service;

import java.time.LocalDateTime;
import java.util.List;

import dao.OrdersDAO;
import entity.WorkShift;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

public class AutomaticAttendanceService {
    public int autoCheckIns(LocalDateTime now) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            List<WorkShift> shifts = em.createQuery("SELECT ws FROM WorkShift ws WHERE ws.user.role IN ('STAFF','SHIPPER') AND ws.user.status = 'ACTIVE' AND ws.status = 'SCHEDULED' AND ws.checkInAt IS NULL AND ws.shiftDate = :date AND ws.startTime <= :time ORDER BY ws.startTime, ws.shiftId", WorkShift.class)
                    .setParameter("date", now.toLocalDate()).setParameter("time", now.toLocalTime().minusMinutes(WorkShiftService.AUTO_MINUTES))
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
            shifts.stream().filter(shift -> canAutoCheckIn(shift, now)).forEach(shift -> WorkShiftService.autoCheckIn(shift, now));
            em.getTransaction().commit();
            return shifts.size();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally { em.close(); }
    }

    public int autoCheckOuts(LocalDateTime now) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            List<WorkShift> shifts = em.createQuery("SELECT ws FROM WorkShift ws WHERE ws.user.role IN ('STAFF','SHIPPER') AND ws.status = 'CHECKED_IN' AND ws.checkInAt IS NOT NULL AND ws.checkOutAt IS NULL AND ws.shiftDate = :date AND ws.endTime <= :time ORDER BY ws.endTime, ws.shiftId", WorkShift.class)
                    .setParameter("date", now.toLocalDate()).setParameter("time", now.toLocalTime().minusMinutes(WorkShiftService.AUTO_MINUTES))
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
            OrdersDAO orders = new OrdersDAO();
            int changed = 0;
            for (WorkShift shift : shifts) {
                long ownership = "STAFF".equals(shift.getUser().getRole()) ? orders.countActiveOwnership(em, shift.getShiftId()) : 0;
                if (canAutoCheckOut(shift, now, ownership)) { WorkShiftService.autoCheckOut(shift, now); changed++; }
            }
            em.getTransaction().commit();
            return changed;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally { em.close(); }
    }

    static boolean canAutoCheckIn(WorkShift shift, LocalDateTime now) {
        return shift != null && now != null && shift.getUser() != null && ("STAFF".equals(shift.getUser().getRole()) || "SHIPPER".equals(shift.getUser().getRole()))
                && "ACTIVE".equals(shift.getUser().getStatus()) && "SCHEDULED".equals(shift.getStatus()) && shift.getCheckInAt() == null
                && !now.isBefore(LocalDateTime.of(shift.getShiftDate(), shift.getStartTime()).plusMinutes(WorkShiftService.AUTO_MINUTES));
    }

    static boolean canAutoCheckOut(WorkShift shift, LocalDateTime now, long activeOwnership) {
        return shift != null && now != null && shift.getUser() != null && "CHECKED_IN".equals(shift.getStatus())
                && shift.getCheckInAt() != null && shift.getCheckOutAt() == null
                && !now.isBefore(LocalDateTime.of(shift.getShiftDate(), shift.getEndTime()).plusMinutes(WorkShiftService.AUTO_MINUTES))
                && (!"STAFF".equals(shift.getUser().getRole()) || activeOwnership == 0);
    }
}
