package service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    public List<Map<String, Object>> list(Integer userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            String jpql = "SELECT ws FROM WorkShift ws" + (userId == null ? " ORDER BY ws.shiftDate DESC, ws.startTime DESC" : " WHERE ws.user.userId = :userId ORDER BY ws.shiftDate DESC, ws.startTime DESC");
            var query = em.createQuery(jpql, WorkShift.class);
            if (userId != null) query.setParameter("userId", userId);
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
                shift.setCheckInAt(LocalDateTime.now());
                shift.setStatus("CHECKED_IN");
            } else {
                if (shift.getCheckInAt() == null || shift.getCheckOutAt() != null) throw new IllegalArgumentException("Cannot check out");
                shift.setCheckOutAt(LocalDateTime.now());
                shift.setStatus("COMPLETED");
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
            if (user == null || user.getRole() == null || (!"STAFF".equals(user.getRole().getRoleName()) && !"SHIPPER".equals(user.getRole().getRoleName()))) throw new IllegalArgumentException("Shift user must be STAFF or SHIPPER");
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
        result.put("role", shift.getUser().getRole().getRoleName());
        result.put("shiftDate", shift.getShiftDate());
        result.put("startTime", shift.getStartTime());
        result.put("endTime", shift.getEndTime());
        result.put("checkInAt", shift.getCheckInAt());
        result.put("checkOutAt", shift.getCheckOutAt());
        result.put("status", shift.getStatus());
        return result;
    }
}
