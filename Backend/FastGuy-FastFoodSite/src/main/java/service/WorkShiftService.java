package service;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dao.StaffPayRateDAO;
import entity.StaffPayRate;
import entity.User;
import entity.WorkShift;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

public class WorkShiftService {
    static final long SHIFT_GRACE_MINUTES = 15;
    static final long AUTO_MINUTES = 5;
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static final Map<String, List<LocalTime>> STAFF_TEMPLATES = Map.of(
            "MORNING", List.of(LocalTime.of(8, 0), LocalTime.of(12, 0)),
            "AFTERNOON", List.of(LocalTime.of(12, 0), LocalTime.of(16, 0)),
            "EVENING", List.of(LocalTime.of(16, 0), LocalTime.of(21, 0)));

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
        return currentCheckedInShift(userId, null);
    }

    public WorkShift currentCheckedInShift(int userId, String role) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            LocalDateTime now = businessNow();
            String jpql = "SELECT ws FROM WorkShift ws WHERE ws.user.userId = :userId AND ws.user.role IN ('STAFF','SHIPPER') AND ws.user.status = 'ACTIVE' AND ws.shiftDate = :today AND ws.status = 'CHECKED_IN' AND ws.checkInAt IS NOT NULL AND ws.checkOutAt IS NULL";
            if (role != null) {
                if (!"STAFF".equals(role) && !"SHIPPER".equals(role)) throw new IllegalArgumentException("Invalid role");
                jpql += " AND ws.user.role = :role";
            }
            var query = em.createQuery(jpql + " ORDER BY ws.checkInAt DESC, ws.shiftId DESC", WorkShift.class)
                    .setParameter("userId", userId).setParameter("today", now.toLocalDate());
            if (role != null) query.setParameter("role", role);
            return query.getResultList().stream().filter(shift -> isValidCheckedInShift(shift, now)).findFirst().orElse(null);
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
                shift.setCheckInSource("MANUAL");
                shift.setStatus("CHECKED_IN");
            } else {
                if (!"CHECKED_IN".equals(shift.getStatus()) || shift.getCheckInAt() == null || shift.getCheckOutAt() != null) throw new IllegalArgumentException("Cannot check out");
                if (!canCheckOut(shift, now)) throw new IllegalArgumentException("Check-out is only allowed from shift end time");
                if ("STAFF".equals(shift.getUser().getRole())) {
                    long activeOwnershipCount = new dao.OrdersDAO().countActiveOwnership(em, shiftId);
                    if (activeOwnershipCount > 0) throw new ActiveOwnershipConflict(activeOwnershipCount);
                }
                completeAttendance(shift, now, "MANUAL");
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

    public static class ScheduleReferenceConflict extends RuntimeException {
        public ScheduleReferenceConflict() { super("Weekly schedule is referenced by orders"); }
    }

    public static class ActiveOwnershipConflict extends RuntimeException {
        private final long activeOwnershipCount;
        public ActiveOwnershipConflict(long activeOwnershipCount) { super("Active order ownership must be handed over before check-out"); this.activeOwnershipCount = activeOwnershipCount; }
        public long getActiveOwnershipCount() { return activeOwnershipCount; }
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
            shift.setStaffRoleSnapshot(roleSnapshot(user));
        }
        if (creating || data.containsKey("shiftDate")) shift.setShiftDate(parseDate(data.get("shiftDate"), "shiftDate"));
        if (creating || data.containsKey("shiftCode")) {
            String code = String.valueOf(data.get("shiftCode"));
            if (!STAFF_TEMPLATES.containsKey(code)) throw new IllegalArgumentException("Invalid shiftCode");
            shift.setShiftCode(code);
        }
        if ("STAFF".equals(shift.getUser().getRole())) {
            if (shift.getShiftCode() == null) throw new IllegalArgumentException("shiftCode is required for STAFF");
            shift.setStartTime(STAFF_TEMPLATES.get(shift.getShiftCode()).get(0));
            shift.setEndTime(STAFF_TEMPLATES.get(shift.getShiftCode()).get(1));
        } else {
            if (creating || data.containsKey("startTime")) shift.setStartTime(parseTime(data.get("startTime"), "startTime"));
            if (creating || data.containsKey("endTime")) shift.setEndTime(parseTime(data.get("endTime"), "endTime"));
            if (shift.getShiftCode() == null) shift.setShiftCode(codeFor(shift.getStartTime()));
        }
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

    public Map<String, Object> week(String weekStart, Integer userId) {
        LocalDate monday = validateWeekStart(weekStart, LocalDate.now(BUSINESS_ZONE).with(DayOfWeek.MONDAY));
        Map<String, Object> result = new HashMap<>();
        result.put("weekStart", monday);
        result.put("shifts", list(userId, "STAFF", monday.toString(), monday.plusDays(6).toString()));
        return result;
    }

    public Map<String, Object> replaceWeek(Map<String, Object> payload) {
        if (payload == null || !payload.keySet().equals(java.util.Set.of("weekStart", "slots")) || !(payload.get("slots") instanceof List<?> slots)) throw new IllegalArgumentException("Invalid weekly schedule payload");
        LocalDate monday = validateWeekStart(String.valueOf(payload.get("weekStart")), LocalDate.now(BUSINESS_ZONE).with(DayOfWeek.MONDAY));
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (Object value : slots) {
            if (!(value instanceof Map<?, ?> slot) || !slot.keySet().equals(java.util.Set.of("shiftDate", "shiftCode", "userId", "role")) || !"STAFF".equals(slot.get("role")) || !(slot.get("userId") instanceof Number)) throw new IllegalArgumentException("Invalid weekly schedule slot");
            LocalDate date = parseDate(slot.get("shiftDate"), "shiftDate");
            String code = String.valueOf(slot.get("shiftCode"));
            if (date.isBefore(monday) || date.isAfter(monday.plusDays(6)) || !STAFF_TEMPLATES.containsKey(code) || !keys.add(date + ":" + code)) throw new IllegalArgumentException("Invalid weekly schedule slot");
        }
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            List<WorkShift> existing = em.createQuery("SELECT ws FROM WorkShift ws WHERE ws.user.role = 'STAFF' AND ws.shiftDate BETWEEN :start AND :end", WorkShift.class).setParameter("start", monday).setParameter("end", monday.plusDays(6)).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
            if (existing.stream().anyMatch(s -> !"SCHEDULED".equals(s.getStatus()) || s.getCheckInAt() != null || s.getCheckOutAt() != null)) throw new IllegalStateException("Attended weekly schedule cannot be replaced");
            if (!existing.isEmpty() && em.createQuery("SELECT COUNT(o) FROM Orders o WHERE o.staffShift IN :shifts", Long.class).setParameter("shifts", existing).getSingleResult() > 0) throw new ScheduleReferenceConflict();
            existing.forEach(em::remove);
            for (Object value : slots) {
                Map<?, ?> slot = (Map<?, ?>) value;
                User user = em.find(User.class, ((Number) slot.get("userId")).intValue(), LockModeType.PESSIMISTIC_WRITE);
                if (user == null || !"STAFF".equals(user.getRole()) || !"ACTIVE".equals(user.getStatus())) throw new IllegalArgumentException("Shift user must be active STAFF");
                WorkShift shift = new WorkShift();
                shift.setUser(user); shift.setStaffRoleSnapshot("STAFF"); shift.setShiftDate(parseDate(slot.get("shiftDate"), "shiftDate")); shift.setShiftCode(String.valueOf(slot.get("shiftCode")));
                shift.setStartTime(STAFF_TEMPLATES.get(shift.getShiftCode()).get(0)); shift.setEndTime(STAFF_TEMPLATES.get(shift.getShiftCode()).get(1)); shift.setStatus("SCHEDULED"); em.persist(shift);
            }
            em.getTransaction().commit();
            return week(monday.toString(), null);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally { em.close(); }
    }

    public long countCoverageGaps() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            Number staffed = (Number) em.createNativeQuery("SELECT COUNT(DISTINCT shift_code) FROM WorkShift WHERE staff_role_snapshot='STAFF' AND shift_date=:date")
                    .setParameter("date", businessNow().toLocalDate()).getSingleResult();
            return Math.max(0, STAFF_TEMPLATES.size() - staffed.longValue());
        } finally {
            em.close();
        }
    }

    public List<Map<String, Object>> monitoring() {
        LocalDateTime now = businessNow();
        List<Map<String, Object>> result = new ArrayList<>();
        for (String code : List.of("MORNING", "AFTERNOON", "EVENING")) {
            List<Map<String, Object>> matches = list(null, "STAFF", now.toLocalDate().toString(), now.toLocalDate().toString()).stream().filter(s -> code.equals(s.get("shiftCode"))).toList();
            Map<String, Object> item = matches.isEmpty() ? missingMonitoring(now.toLocalDate(), code) : new HashMap<>(matches.get(0));
            if (!matches.isEmpty()) {
                WorkShift shift = findShift(((Number) item.get("shiftId")).intValue());
                long ownership = new dao.OrdersDAO().countActiveOwnership(shift.getShiftId());
                boolean missingNext = nextCode(code) != null && list(null, "STAFF", now.toLocalDate().toString(), now.toLocalDate().toString()).stream().noneMatch(s -> nextCode(code).equals(s.get("shiftCode")));
                List<String> state = monitoring(shift, now, missingNext, ownership);
                item.put("monitoringState", state.get(0)); item.put("alertSeverity", state.get(1)); item.put("activeOwnershipCount", ownership);
            }
            result.add(item);
        }
        return result;
    }

    static LocalDate validateWeekStart(String value, LocalDate currentMonday) {
        LocalDate date;
        try { date = LocalDate.parse(value); } catch (RuntimeException e) { throw new IllegalArgumentException("Invalid weekStart"); }
        if (date.getDayOfWeek() != DayOfWeek.MONDAY || date.isAfter(currentMonday)) throw new IllegalArgumentException("weekStart must be a Monday not after the current week");
        return date;
    }

    static List<String> monitoring(WorkShift shift, LocalDateTime now, boolean missingNext, long ownership) {
        LocalDateTime start = LocalDateTime.of(shift.getShiftDate(), shift.getStartTime());
        LocalDateTime end = LocalDateTime.of(shift.getShiftDate(), shift.getEndTime());
        if ("CHECKED_OUT".equals(shift.getStatus())) return List.of("AUTO".equals(shift.getCheckOutSource()) ? "COMPLETED_AUTO" : "COMPLETED_MANUAL", "INFO");
        if ("CHECKED_IN".equals(shift.getStatus())) {
            if (now.isAfter(end.plusMinutes(AUTO_MINUTES)) && ownership > 0) return List.of("ROLLOVER_BLOCKED", "CRITICAL");
            if (now.isAfter(end)) return List.of("CHECK_OUT_WINDOW", "WARNING");
            return List.of("AUTO".equals(shift.getCheckInSource()) ? "ACTIVE_AUTO" : "ACTIVE_MANUAL", "AUTO".equals(shift.getCheckInSource()) ? "WARNING" : "INFO");
        }
        if (missingNext && now.isAfter(end)) return List.of("MISSING_NEXT_SHIFT", "CRITICAL");
        if (now.isAfter(start.plusMinutes(AUTO_MINUTES))) return List.of("LATE", "WARNING");
        if (!now.isBefore(start.minusMinutes(SHIFT_GRACE_MINUTES))) return List.of("CHECK_IN_WINDOW", "INFO");
        return List.of("SCHEDULED", "INFO");
    }

    record Attendance(int actualMinutes, int overlapEligibleMinutes, int lateMinutes, int earlyLeaveMinutes, int potentialOvertimeMinutes) {}

    static Attendance attendance(WorkShift shift) {
        if (shift.getCheckInAt() == null || shift.getCheckOutAt() == null) return new Attendance(0, 0, 0, 0, 0);
        LocalDateTime scheduledStart = LocalDateTime.of(shift.getShiftDate(), shift.getStartTime());
        LocalDateTime scheduledEnd = LocalDateTime.of(shift.getShiftDate(), shift.getEndTime());
        int actual = nonnegativeMinutes(shift.getCheckInAt(), shift.getCheckOutAt());
        LocalDateTime overlapStart = shift.getCheckInAt().isAfter(scheduledStart) ? shift.getCheckInAt() : scheduledStart;
        LocalDateTime overlapEnd = shift.getCheckOutAt().isBefore(scheduledEnd) ? shift.getCheckOutAt() : scheduledEnd;
        return new Attendance(actual, nonnegativeMinutes(overlapStart, overlapEnd), nonnegativeMinutes(scheduledStart, shift.getCheckInAt()), nonnegativeMinutes(shift.getCheckOutAt(), scheduledEnd), nonnegativeMinutes(scheduledEnd, shift.getCheckOutAt()));
    }

    private static int nonnegativeMinutes(LocalDateTime from, LocalDateTime to) { return (int) Math.max(0, ChronoUnit.MINUTES.between(from, to)); }

    static void validateApproval(WorkShift shift, int approvedMinutes, int approvedOvertimeMinutes) {
        Attendance value = attendance(shift);
        if (approvedMinutes < 0 || approvedOvertimeMinutes < 0 || approvedMinutes > value.overlapEligibleMinutes() || approvedOvertimeMinutes > value.potentialOvertimeMinutes()) throw new IllegalArgumentException("Approved minutes exceed calculated bounds");
    }

    static void completeAttendance(WorkShift shift, LocalDateTime at, String source) {
        shift.setCheckOutAt(at); shift.setCheckOutSource(source); shift.setStatus("CHECKED_OUT"); shift.setAttendanceStatus("PENDING");
        shift.setApprovedMinutes(null); shift.setApprovedOvertimeMinutes(null); shift.setApprovedBy(null); shift.setApprovedAt(null);
    }
    public List<Map<String, Object>> attendance(String month, Integer userId, String status) {
        YearMonth period;
        try { period = YearMonth.parse(month); } catch (RuntimeException e) { throw new IllegalArgumentException("Invalid month"); }
        if (status != null && !status.isBlank() && !java.util.Set.of("PENDING", "APPROVED").contains(status)) throw new IllegalArgumentException("Invalid attendance status");
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            String jpql = "SELECT ws FROM WorkShift ws WHERE ws.staffRoleSnapshot = 'STAFF' AND ws.attendanceStatus IN ('PENDING','APPROVED') AND ws.shiftDate BETWEEN :start AND :end";
            if (userId != null) jpql += " AND ws.user.userId = :userId";
            if (status != null && !status.isBlank()) jpql += " AND ws.attendanceStatus = :status";
            var query = em.createQuery(jpql + " ORDER BY ws.shiftDate DESC, ws.startTime", WorkShift.class).setParameter("start", period.atDay(1)).setParameter("end", period.atEndOfMonth());
            if (userId != null) query.setParameter("userId", userId);
            if (status != null && !status.isBlank()) query.setParameter("status", status);
            return query.getResultList().stream().map(this::attendanceMap).toList();
        } finally { em.close(); }
    }

    private final ActivityLogService activityLogService = new ActivityLogService();

    public Map<String, Object> approveAttendance(int shiftId, int adminId, Map<String, Object> data) {
        if (data == null || !data.keySet().equals(java.util.Set.of("expectedUpdatedAt", "approvedMinutes", "approvedOvertimeMinutes", "attendanceNote")) && !data.keySet().equals(java.util.Set.of("expectedUpdatedAt", "approvedMinutes", "approvedOvertimeMinutes"))) throw new IllegalArgumentException("Invalid attendance approval payload");
        int minutes = exactInt(data.get("approvedMinutes"), "approvedMinutes");
        int overtime = exactInt(data.get("approvedOvertimeMinutes"), "approvedOvertimeMinutes");
        String note = data.get("attendanceNote") == null ? null : String.valueOf(data.get("attendanceNote")).trim();
        if (note != null && note.length() > 500) throw new IllegalArgumentException("attendanceNote is too long");
        LocalDateTime expected;
        try { expected = LocalDateTime.parse(String.valueOf(data.get("expectedUpdatedAt"))); } catch (RuntimeException e) { throw new IllegalArgumentException("Invalid expectedUpdatedAt"); }
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            WorkShift shift = em.find(WorkShift.class, shiftId, LockModeType.PESSIMISTIC_WRITE);
            if (shift == null) throw new AttendanceNotFound();
            validateApprovalEligibility(shift);
            if (shift.getUpdatedAt() == null || !shift.getUpdatedAt().equals(expected)) throw new StaleAttendanceConflict();
            validateApproval(shift, minutes, overtime);
            if (shift.getCheckOutAt() == null) throw new IllegalArgumentException("Attendance is not complete");
            StaffPayRate rate=new StaffPayRateDAO().effective(em,shift.getUser().getUserId(),shift.getShiftDate(),LockModeType.PESSIMISTIC_READ);
            if(rate==null)throw new StaffPayRateService.MissingRate();
            StaffPayRateService.Pay pay=StaffPayRateService.calculate(minutes,overtime,rate.getRegularHourlyRate(),rate.getOvertimeHourlyRate());
            shift.setApprovedMinutes(minutes); shift.setApprovedOvertimeMinutes(overtime); shift.setAttendanceNote(note == null || note.isBlank() ? null : note); shift.setApprovedBy(adminId); shift.setApprovedAt(businessNow()); shift.setAttendanceStatus("APPROVED");
            shift.setPaySnapshotStatus("CALCULATED");shift.setRegularHourlyRateSnapshot(rate.getRegularHourlyRate());shift.setOvertimeHourlyRateSnapshot(rate.getOvertimeHourlyRate());shift.setRegularPayAmount(pay.regular());shift.setOvertimePayAmount(pay.overtime());shift.setTotalPayAmount(pay.total());
            activityLogService.append(em,adminId,"ATTENDANCE_APPROVED","ATTENDANCE",shiftId,Map.of("approvedMinutes",minutes,"approvedOvertimeMinutes",overtime));
            em.getTransaction().commit();
            return attendanceMap(shift);
        } catch (RuntimeException e) { if (em.getTransaction().isActive()) em.getTransaction().rollback(); throw e; }
        finally { em.close(); }
    }

    static void validateApprovalEligibility(WorkShift shift) {
        if (!"STAFF".equals(shift.getStaffRoleSnapshot()) || !"PENDING".equals(shift.getAttendanceStatus())) throw new IllegalArgumentException("Only pending Staff attendance can be approved");
    }

    static int exactInt(Object value, String field) {
        if (!(value instanceof Number number)) throw new IllegalArgumentException("Invalid " + field);
        try { return new java.math.BigDecimal(number.toString()).intValueExact(); }
        catch (RuntimeException e) { throw new IllegalArgumentException("Invalid " + field); }
    }

    public static class AttendanceNotFound extends RuntimeException { public AttendanceNotFound() { super("Shift not found"); } }
    public static class StaleAttendanceConflict extends RuntimeException { public StaleAttendanceConflict() { super("Attendance changed; reload before approval"); } }

    private Map<String, Object> attendanceMap(WorkShift shift) {
        Map<String, Object> result = toMap(shift); Attendance value = attendance(shift);
        result.put("attendanceStatus", shift.getAttendanceStatus()); result.put("actualMinutes", value.actualMinutes()); result.put("overlapEligibleMinutes", value.overlapEligibleMinutes()); result.put("lateMinutes", value.lateMinutes()); result.put("earlyLeaveMinutes", value.earlyLeaveMinutes()); result.put("potentialOvertimeMinutes", value.potentialOvertimeMinutes()); result.put("approvedMinutes", shift.getApprovedMinutes()); result.put("approvedOvertimeMinutes", shift.getApprovedOvertimeMinutes()); result.put("attendanceNote", shift.getAttendanceNote()); result.put("approvedBy", shift.getApprovedBy()); result.put("approvedAt", shift.getApprovedAt());
        result.put("paySnapshotStatus",shift.getPaySnapshotStatus());result.put("regularHourlyRateSnapshot",shift.getRegularHourlyRateSnapshot());result.put("overtimeHourlyRateSnapshot",shift.getOvertimeHourlyRateSnapshot());result.put("regularPayAmount",shift.getRegularPayAmount());result.put("overtimePayAmount",shift.getOvertimePayAmount());result.put("totalPayAmount",shift.getTotalPayAmount());
        StaffPayRate effective="PENDING".equals(shift.getAttendanceStatus())?new StaffPayRateDAO().effective(shift.getUser().getUserId(),shift.getShiftDate()):null;result.put("effectiveRegularHourlyRate",effective==null?null:effective.getRegularHourlyRate());result.put("effectiveOvertimeHourlyRate",effective==null?null:effective.getOvertimeHourlyRate());StaffPayRateService.Pay preview=effective==null?null:StaffPayRateService.calculate(value.overlapEligibleMinutes(),value.potentialOvertimeMinutes(),effective.getRegularHourlyRate(),effective.getOvertimeHourlyRate());result.put("previewRegularPayAmount",preview==null?null:preview.regular());result.put("previewOvertimePayAmount",preview==null?null:preview.overtime());result.put("previewTotalPayAmount",preview==null?null:preview.total());result.put("updatedAt", shift.getUpdatedAt());
        result.remove("userName"); result.remove("role"); result.remove("status"); result.remove("checkInSource"); result.remove("checkOutSource");
        return result;
    }

    private static String nextCode(String code) { return "MORNING".equals(code) ? "AFTERNOON" : "AFTERNOON".equals(code) ? "EVENING" : null; }
    private static String codeFor(LocalTime start) { return start.isBefore(LocalTime.NOON) ? "MORNING" : start.isBefore(LocalTime.of(16, 0)) ? "AFTERNOON" : "EVENING"; }
    private static String roleSnapshot(User user) { return "STAFF".equals(user.getRole()) ? "STAFF" : "NON_STAFF"; }

    private WorkShift findShift(int id) { EntityManager em = DatabaseUtil.getEntityManager(); try { return em.find(WorkShift.class, id); } finally { em.close(); } }
    private Map<String, Object> missingMonitoring(LocalDate date, String code) { Map<String, Object> item = new HashMap<>(); item.put("shiftId", null); item.put("userId", null); item.put("userName", null); item.put("staffName", null); item.put("role", null); item.put("status", null); item.put("shiftDate", date); item.put("shiftCode", code); item.put("startTime", STAFF_TEMPLATES.get(code).get(0)); item.put("endTime", STAFF_TEMPLATES.get(code).get(1)); item.put("checkInAt", null); item.put("checkOutAt", null); item.put("checkInSource", null); item.put("checkOutSource", null); item.put("monitoringState", "MISSING_STAFF"); item.put("alertSeverity", "CRITICAL"); item.put("activeOwnershipCount", 0L); return item; }

    private Map<String, Object> toMap(WorkShift shift) {
        Map<String, Object> result = new HashMap<>();
        result.put("shiftId", shift.getShiftId());
        result.put("userId", shift.getUser().getUserId());
        result.put("userName", shift.getUser().getFullName());
        result.put("staffName", shift.getUser().getFullName());
        result.put("role", shift.getUser().getRole());
        result.put("shiftDate", shift.getShiftDate());
        result.put("shiftCode", shift.getShiftCode());
        result.put("startTime", shift.getStartTime());
        result.put("endTime", shift.getEndTime());
        result.put("checkInAt", shift.getCheckInAt());
        result.put("checkOutAt", shift.getCheckOutAt());
        result.put("checkInSource", shift.getCheckInSource());
        result.put("checkOutSource", shift.getCheckOutSource());
        result.put("status", shift.getStatus());
        return result;
    }
}
