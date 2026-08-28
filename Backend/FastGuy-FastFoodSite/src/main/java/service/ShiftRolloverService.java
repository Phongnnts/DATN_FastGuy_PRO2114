package service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import dao.OrdersDAO;
import entity.OrderStatusHistory;
import entity.Orders;
import entity.WorkShift;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

public class ShiftRolloverService {
    static final Set<String> ROLLOVER_STATUSES = Set.of("CONFIRMED", "PREPARING", "READY", "DELIVERY_FAILED");

    public void rolloverEnded(LocalDateTime now) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Integer> ids = em.createQuery("SELECT ws.shiftId FROM WorkShift ws WHERE ws.user.role = 'STAFF' AND ws.status = 'CHECKED_IN' AND ws.shiftDate = :date AND ws.shiftCode IN ('MORNING','AFTERNOON') AND ws.endTime <= :time ORDER BY ws.endTime, ws.shiftId", Integer.class)
                    .setParameter("date", now.toLocalDate()).setParameter("time", now.toLocalTime()).getResultList();
            for (Integer id : ids) {
                try { rollover(id, now); } catch (RuntimeException e) { e.printStackTrace(); }
            }
        } finally { em.close(); }
    }

    public int rollover(int shiftId, LocalDateTime now) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            WorkShift current = em.find(WorkShift.class, shiftId, LockModeType.PESSIMISTIC_WRITE);
            if (current == null || !"STAFF".equals(current.getUser().getRole())) throw new IllegalArgumentException("Staff shift not found");
            String nextCode = nextShiftCode(current.getShiftCode());
            if (nextCode == null) { em.getTransaction().commit(); return 0; }
            List<WorkShift> next = em.createQuery("SELECT ws FROM WorkShift ws WHERE ws.user.role = 'STAFF' AND ws.shiftDate = :date AND ws.shiftCode = :code AND ws.status IN ('SCHEDULED','CHECKED_IN') ORDER BY CASE WHEN ws.status = 'CHECKED_IN' THEN 0 ELSE 1 END, ws.shiftId", WorkShift.class)
                    .setParameter("date", current.getShiftDate()).setParameter("code", nextCode).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
            if (next.size() != 1) { em.getTransaction().commit(); return 0; }
            WorkShift target = next.get(0);
            List<Orders> orders = new OrdersDAO().lockActiveOwnership(em, shiftId);
            for (Orders order : orders) {
                String status = order.getOrderStatus();
                order.setStaff(target.getUser()); order.setStaffShift(target);
                em.persist(new OrderStatusHistory(order.getOrderId(), null, "SYSTEM", status, status, "Tự động chuyển quyền sở hữu sang ca " + nextCode, now));
            }
            em.getTransaction().commit();
            return orders.size();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally { em.close(); }
    }

    static String nextShiftCode(String code) { return "MORNING".equals(code) ? "AFTERNOON" : "AFTERNOON".equals(code) ? "EVENING" : null; }
    static boolean sameDate(LocalDate first, LocalDate second) { return first != null && first.equals(second); }
}
