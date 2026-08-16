package service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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
    static final long SHIFT_GRACE_MINUTES = 15;
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    static LocalDateTime businessNow() {
        return LocalDateTime.now(BUSINESS_ZONE);
    }

    public List<Map<String, Object>> list(Integer userId, String role, String fromDate, String toDate) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT ws FROM WorkShift ws WHERE 1=1");
            List<String> conditions = new ArrayList<>();
            Map<String, Object> params = new HashMap<>();
            if (userId != null) { conditions.add("ws.user.userId = :userId"); params.put("userId", userId); }
            if (role != null && !role.isBlank()) {
                if (!"STAFF".equals(role) && !"SHIPPER".equals(role)) throw new IllegalArgumentException("Invalid role");
                conditions.add("ws.user.role = :role");
                params.put("role", role);
            }
            LocalDate from = fromDate != null && !fromDate.isBlank() ? parseDate(fromDate, "fromDate") : null;
            LocalDate to = toDate != null && !toDate.isBlank() ? parseDate(toDate, "toDate") : null;
            if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("fromDate must not be after toDate");
            if (from != null) { conditions.add("ws.shiftDate >= :fromDate"); params.put("fromDate", from); }
            if (to != null) { conditions.add("ws.shiftDate <= :toDate"); params.put("toDate", to); }
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
            lockShiftUser(em, data);
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
            em.lock(shift.getUser(), LockModeType.PESSIMISTIC_WRITE);
            if (data != null && data.containsKey("userId") && ((Number) data.get("userId")).intValue() != shift.getUser().getUserId()) lockShiftUser(em, data);
            if (!"SCHEDULED".equals(shift.getStatus()) || shift.getCheckInAt() != null || shift.getCheckOutAt() != null) throw new IllegalArgumentException("Only unattended scheduled shifts can be updated");
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

    public WorkShift currentCheckedInShift(int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
            List<WorkShift> shifts = em.createQuery("SELECT ws FROM WorkShift ws WHERE ws.user.userId = :userId AND ws.user.role = 'STAFF' AND ws.user.status = 'ACTIVE' AND ws.shiftDate = :today AND ws.status = 'CHECKED_IN' AND ws.checkInAt IS NOT NULL AND ws.checkOutAt IS NULL ORDER BY ws.checkInAt DESC, ws.shiftId DESC", WorkShift.class)
                    .setParameter("userId", userId)
                    .setParameter("today", now.toLocalDate())
                    .getResultList();
            return shifts.stream().filter(shift -> isValidCheckedInShift(shift, now)).findFirst().orElse(null);
        } finally {
            em.close();
        }
    }

    public Map<String, Object> current(int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
            List<WorkShift> shifts = em.createQuery("SELECT ws FROM WorkShift ws WHERE ws.user.userId = :userId AND ws.shiftDate = :today ORDER BY ws.startTime, ws.shiftId", WorkShift.class)
                    .setParameter("userId", userId).setParameter("today", now.toLocalDate()).getResultList();
            CurrentShift current = current(shifts, now.toLocalTime());
            Map<String, Object> result = new HashMap<>();
            result.put("state", current.state());
            result.put("shift", current.shift() == null ? null : toMap(current.shift()));
            return result;
        } finally {
            em.close();
        }
    }

    record CurrentShift(String state, WorkShift shift) {}

    static CurrentShift current(List<WorkShift> shifts, LocalTime now) {
        WorkShift selected = shifts.stream().filter(s -> isCheckedInWithinGrace(s, now)).findFirst().orElse(null);
        if (selected != null) return new CurrentShift("CHECKED_IN", selected);
        if (!shifts.isEmpty() && shifts.stream().allMatch(s -> s.getCheckOutAt() != null)) return new CurrentShift("CHECKED_OUT", shifts.get(shifts.size() - 1));
        selected = shifts.stream().filter(s -> s.getCheckOutAt() == null && !now.isAfter(s.getEndTime().plusMinutes(SHIFT_GRACE_MINUTES))).findFirst().orElse(null);
        if (selected == null) return new CurrentShift("NONE", null);
        if (now.isBefore(selected.getStartTime().minusMinutes(SHIFT_GRACE_MINUTES))) return new CurrentShift("UPCOMING", selected);
        return new CurrentShift("CHECK_IN_ALLOWED", selected);
    }

    static boolean canCheckOut(LocalTime now, LocalTime endTime) {
        return !now.isBefore(endTime);
    }

    static boolean isCheckedInWithinGrace(WorkShift shift, LocalTime now) {
        return "CHECKED_IN".equals(shift.getStatus()) && shift.getCheckInAt() != null && shift.getCheckOutAt() == null
                && !now.isAfter(shift.getEndTime().plusMinutes(SHIFT_GRACE_MINUTES));
    }

    static boolean isValidCheckedInShift(WorkShift shift, LocalDateTime now) {
        return shift != null && shift.getShiftDate().equals(now.toLocalDate()) && isCheckedInWithinGrace(shift, now.toLocalTime());
    }

    static boolean canCheckOut(WorkShift shift, LocalDateTime now) {
        LocalDateTime end = LocalDateTime.of(shift.getShiftDate(), shift.getEndTime());
        return "CHECKED_IN".equals(shift.getStatus()) && shift.getCheckInAt() != null && shift.getCheckOutAt() == null
                && !now.isBefore(end);
    }

    public Map<String, Object> check(int shiftId, int userId, boolean checkIn) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            WorkShift shift = em.find(WorkShift.class, shiftId, LockModeType.PESSIMISTIC_WRITE);
            if (shift == null || shift.getUser().getUserId() != userId) throw new IllegalArgumentException("Shift not found");
            LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
            if (checkIn) {
                if (shift.getCheckInAt() != null) throw new IllegalArgumentException("Already checked in");
                if (!"SCHEDULED".equals(shift.getStatus())) throw new IllegalArgumentException("Shift is not in scheduled status");
                if (!shift.getShiftDate().equals(now.toLocalDate())) throw new IllegalArgumentException("Shift is not for today");
                LocalTime currentTime = now.toLocalTime();
                LocalTime start = shift.getStartTime().minusMinutes(SHIFT_GRACE_MINUTES);
                LocalTime end = shift.getEndTime().plusMinutes(SHIFT_GRACE_MINUTES);
                if (currentTime.isBefore(start) || currentTime.isAfter(end)) throw new IllegalArgumentException("Outside shift time window");
                shift.setCheckInAt(now);
                shift.setStatus("CHECKED_IN");
            } else {
                if (!"CHECKED_IN".equals(shift.getStatus()) || shift.getCheckInAt() == null || shift.getCheckOutAt() != null) throw new IllegalArgumentException("Cannot check out");
                if (!canCheckOut(shift, now)) throw new IllegalArgumentException("Check-out is only allowed from shift end time");
                shift.setCheckOutAt(now);
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

    private void lockShiftUser(EntityManager em, Map<String, Object> data) {
        if (data == null || !(data.get("userId") instanceof Number value)) throw new IllegalArgumentException("Invalid userId");
        User user = em.find(User.class, value.intValue(), LockModeType.PESSIMISTIC_WRITE);
        if (user == null) throw new IllegalArgumentException("Shift user must be STAFF or SHIPPER");
    }

    private WorkShift apply(EntityManager em, WorkShift shift, Map<String, Object> data, boolean creating) {
        if (data == null) throw new IllegalArgumentException("Invalid shift");
        if (creating || data.containsKey("userId")) {
            Object value = data.get("userId");
            if (!(value instanceof Number)) throw new IllegalArgumentException("Invalid userId");
            User user = em.find(User.class, ((Number) value).intValue());
            if (user == null || (!"STAFF".equals(user.getRole()) && !"SHIPPER".equals(user.getRole()))) throw new IllegalArgumentException("Shift user must be STAFF or SHIPPER");
            if (!"ACTIVE".equals(user.getStatus())) throw new IllegalArgumentException("Shift user must be active");
            shift.setUser(user);
        }
        if (creating || data.containsKey("shiftDate")) shift.setShiftDate(parseDate(data.get("shiftDate"), "shiftDate"));
        if (creating || data.containsKey("startTime")) shift.setStartTime(parseTime(data.get("startTime"), "startTime"));
        if (creating || data.containsKey("endTime")) shift.setEndTime(parseTime(data.get("endTime"), "endTime"));
        if (shift.getEndTime() == null || shift.getStartTime() == null || !shift.getEndTime().isAfter(shift.getStartTime())) throw new IllegalArgumentException("End time must be after start time");
        shift.setStatus(creating ? "SCHEDULED" : shift.getStatus());
        Long overlaps = em.createQuery("SELECT COUNT(ws) FROM WorkShift ws WHERE ws.user.userId = :userId AND ws.shiftDate = :shiftDate AND ws.shiftId <> :shiftId AND ws.startTime < :endTime AND ws.endTime > :startTime", Long.class)
                .setParameter("userId", shift.getUser().getUserId()).setParameter("shiftDate", shift.getShiftDate())
                .setParameter("shiftId", shift.getShiftId()).setParameter("endTime", shift.getEndTime())
                .setParameter("startTime", shift.getStartTime()).getSingleResult();
        if (overlaps > 0) throw new IllegalArgumentException("User already has an overlapping shift");
        return shift;
    }

    private LocalDate parseDate(Object value, String field) {
        try { return LocalDate.parse(String.valueOf(value)); }
        catch (RuntimeException e) { throw new IllegalArgumentException("Invalid " + field); }
    }

    private LocalTime parseTime(Object value, String field) {
        try { return LocalTime.parse(String.valueOf(value)); }
        catch (RuntimeException e) { throw new IllegalArgumentException("Invalid " + field); }
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
