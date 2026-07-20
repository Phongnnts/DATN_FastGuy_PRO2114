package service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entity.User;
import entity.WorkShift;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

public class WorkShiftService {
    public List<Map<String, Object>> list(Integer userId, String role, String fromDate, String toDate) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT ws FROM WorkShift ws WHERE 1=1");
            List<String> conditions = new ArrayList<>();
            Map<String, Object> params = new HashMap<>();
            if (userId != null) { conditions.add("ws.user.userId = :userId"); params.put("userId", userId); }
            if (role != null && !role.isBlank()) { conditions.add("ws.user.role = :role"); params.put("role", role); }
            if (fromDate != null && !fromDate.isBlank()) { conditions.add("ws.shiftDate >= :fromDate"); params.put("fromDate", LocalDate.parse(fromDate)); }
            if (toDate != null && !toDate.isBlank()) { conditions.add("ws.shiftDate <= :toDate"); params.put("toDate", LocalDate.parse(toDate)); }
            for (String c : conditions) jpql.append(" AND ").append(c);
            jpql.append(" ORDER BY ws.shiftDate DESC, ws.startTime DESC");
            var query = em.createQuery(jpql.toString(), WorkShift.class);
            for (var e : params.entrySet()) query.setParameter(e.getKey(), e.getValue());
            List<Map<String, Object>> result = new ArrayList<>();
            for (WorkShift shift : query.getResultList()) result.add(toMap(shift));
            return result;
        } finally {
            em.close();
        }
    }

    public Map<String, Object> create(Map<String, Object> data) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            WorkShift shift = apply(em, new WorkShift(), data, true);
            shift.setCreatedAt(LocalDateTime.now());
            em.persist(shift);
            em.getTransaction().commit();
            return toMap(shift);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public Map<String, Object> update(int shiftId, Map<String, Object> data) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            WorkShift shift = em.find(WorkShift.class, shiftId, LockModeType.PESSIMISTIC_WRITE);
            if (shift == null) throw new IllegalArgumentException("Shift not found");
            apply(em, shift, data, false);
            em.getTransaction().commit();
            return toMap(shift);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void delete(int shiftId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            WorkShift shift = em.find(WorkShift.class, shiftId, LockModeType.PESSIMISTIC_WRITE);
            if (shift == null) throw new IllegalArgumentException("Shift not found");
            if (!"SCHEDULED".equals(shift.getStatus()) || shift.getCheckInAt() != null) throw new IllegalArgumentException("Only scheduled shifts can be deleted");
            em.remove(shift);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public Map<String, Object> check(int shiftId, int userId, boolean checkIn) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            WorkShift shift = em.find(WorkShift.class, shiftId, LockModeType.PESSIMISTIC_WRITE);
            if (shift == null || shift.getUser().getUserId() != userId) throw new IllegalArgumentException("Shift not found");
            if (checkIn) {
                if (shift.getCheckInAt() != null) throw new IllegalArgumentException("Already checked in");
                if (!"SCHEDULED".equals(shift.getStatus())) throw new IllegalArgumentException("Shift is not in scheduled status");
                LocalDate today = LocalDate.now();
                if (!shift.getShiftDate().equals(today)) throw new IllegalArgumentException("Shift is not for today");
                LocalTime now = LocalTime.now();
                LocalTime start = shift.getStartTime().minusMinutes(15);
                LocalTime end = shift.getEndTime().plusMinutes(15);
                if (now.isBefore(start) || now.isAfter(end)) throw new IllegalArgumentException("Outside shift time window");
                shift.setCheckInAt(LocalDateTime.now());
                shift.setStatus("CHECKED_IN");
            } else {
                if (shift.getCheckInAt() == null || shift.getCheckOutAt() != null) throw new IllegalArgumentException("Cannot check out");
                shift.setCheckOutAt(LocalDateTime.now());
                shift.setStatus("CHECKED_OUT");
            }
            em.getTransaction().commit();
            return toMap(shift);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    private WorkShift apply(EntityManager em, WorkShift shift, Map<String, Object> data, boolean creating) {
        if (data == null) throw new IllegalArgumentException("Invalid shift");
        if (creating || data.containsKey("userId")) {
            Object value = data.get("userId");
            if (!(value instanceof Number)) throw new IllegalArgumentException("Invalid userId");
            User user = em.find(User.class, ((Number) value).intValue());
            if (user == null || (!"STAFF".equals(user.getRole()) && !"SHIPPER".equals(user.getRole()))) throw new IllegalArgumentException("Shift user must be STAFF or SHIPPER");
            shift.setUser(user);
        }
        if (creating || data.containsKey("shiftDate")) shift.setShiftDate(LocalDate.parse(String.valueOf(data.get("shiftDate"))));
        if (creating || data.containsKey("startTime")) shift.setStartTime(LocalTime.parse(String.valueOf(data.get("startTime"))));
        if (creating || data.containsKey("endTime")) shift.setEndTime(LocalTime.parse(String.valueOf(data.get("endTime"))));
        if (shift.getEndTime() == null || shift.getStartTime() == null || !shift.getEndTime().isAfter(shift.getStartTime())) throw new IllegalArgumentException("Invalid shift time");
        if (data.containsKey("status")) shift.setStatus(String.valueOf(data.get("status")));
        else if (creating) shift.setStatus("SCHEDULED");
        return shift;
    }

    private Map<String, Object> toMap(WorkShift shift) {
        Map<String, Object> result = new HashMap<>();
        result.put("shiftId", shift.getShiftId());
        result.put("userId", shift.getUser().getUserId());
        result.put("userName", shift.getUser().getFullName());
        result.put("role", shift.getUser().getRole());
        result.put("shiftDate", shift.getShiftDate());
        result.put("startTime", shift.getStartTime());
        result.put("endTime", shift.getEndTime());
        result.put("checkInAt", shift.getCheckInAt());
        result.put("checkOutAt", shift.getCheckOutAt());
        result.put("status", shift.getStatus());
        return result;
    }
}
