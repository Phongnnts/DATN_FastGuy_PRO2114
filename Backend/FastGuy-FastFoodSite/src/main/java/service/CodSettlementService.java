package service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import dao.CodSettlementDAO;
import entity.CodSettlement;
import entity.User;
import entity.WorkShift;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

public class CodSettlementService {
    private final CodSettlementDAO dao;
    private final Supplier<EntityManager> entityManagers;
    private final Supplier<LocalDateTime> businessNow;

    public CodSettlementService() {
        this(new CodSettlementDAO(), DatabaseUtil::getEntityManager, () -> LocalDateTime.now(WorkShiftService.BUSINESS_ZONE));
    }

    CodSettlementService(CodSettlementDAO dao, Supplier<EntityManager> entityManagers, Supplier<LocalDateTime> businessNow) {
        this.dao = dao;
        this.entityManagers = entityManagers;
        this.businessNow = businessNow;
    }

    public static final class SettlementConflictException extends RuntimeException {
        public SettlementConflictException(String message) { super(message); }
    }

    public static final class SettlementNotFoundException extends RuntimeException {
        public SettlementNotFoundException(String message) { super(message); }
    }

    public static void validateSubmission(BigDecimal submittedAmount) {
        normalizeAmount(submittedAmount, "Số tiền thực nộp không hợp lệ");
    }

    public static BigDecimal normalizeAmount(BigDecimal amount, String message) {
        if (amount == null || amount.signum() < 0) throw new IllegalArgumentException(message);
        try {
            BigDecimal normalized = amount.setScale(2, RoundingMode.UNNECESSARY);
            if (normalized.precision() > 18) throw new IllegalArgumentException(message);
            return normalized;
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(message);
        }
    }

    static void validateVerification(String status, BigDecimal submittedAmount, BigDecimal verifiedAmount, String reason) {
        if (status == null || !Set.of("SETTLED", "SHORT", "OVER").contains(status)) throw new IllegalArgumentException("Trạng thái xác nhận không hợp lệ");
        normalizeAmount(submittedAmount, "Số tiền đã nộp không hợp lệ");
        normalizeAmount(verifiedAmount, "Số tiền kiểm đếm không hợp lệ");
        String normalizedReason = reason == null ? null : reason.trim();
        if (normalizedReason != null && normalizedReason.length() > 500) throw new IllegalArgumentException("Lý do không được vượt quá 500 ký tự");
        int comparison = verifiedAmount.compareTo(submittedAmount);
        if ("SETTLED".equals(status) && comparison != 0) throw new IllegalArgumentException("Số tiền khớp phải bằng số đã nộp");
        if ("SHORT".equals(status) && (comparison >= 0 || normalizedReason == null || normalizedReason.isEmpty())) throw new IllegalArgumentException("Thiếu tiền cần số kiểm đếm thấp hơn và lý do");
        if ("OVER".equals(status) && (comparison <= 0 || normalizedReason == null || normalizedReason.isEmpty())) throw new IllegalArgumentException("Thừa tiền cần số kiểm đếm cao hơn và lý do");
    }

    static boolean isCurrentShift(WorkShift shift, LocalDateTime now) {
        if (shift == null || !"CHECKED_IN".equals(shift.getStatus()) || shift.getCheckInAt() == null || shift.getCheckOutAt() != null) return false;
        CodSettlementDAO.ShiftWindow window = CodSettlementDAO.shiftWindow(shift);
        return !now.isBefore(window.start()) && now.isBefore(window.end());
    }

    static WorkShift selectCurrentShift(List<WorkShift> shifts, LocalDateTime now) {
        return shifts.stream().filter(shift -> isCurrentShift(shift, now)).findFirst().orElse(null);
    }

    static boolean isDuplicateSettlementFailure(RuntimeException failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && (message.contains("UQ_CodSettlement_ShipperShift") || message.contains("shipper_id") && message.contains("shift_id") && message.toLowerCase().contains("unique"))) return true;
        }
        return false;
    }

    public Map<String, Object> getShipperCurrent(int shipperId) {
        EntityManager em = entityManagers.get();
        try {
            LocalDateTime now = businessNow.get();
            List<WorkShift> shifts = em.createQuery("SELECT ws FROM WorkShift ws WHERE ws.user.userId = :shipperId AND ws.user.role = 'SHIPPER' AND ws.user.status = 'ACTIVE' AND ws.shiftDate IN :shiftDates AND ws.status = 'CHECKED_IN' AND ws.checkInAt IS NOT NULL AND ws.checkOutAt IS NULL ORDER BY ws.checkInAt DESC, ws.shiftId DESC", WorkShift.class)
                    .setParameter("shipperId", shipperId)
                    .setParameter("shiftDates", List.of(now.toLocalDate(), now.toLocalDate().minusDays(1)))
                    .getResultList();
            WorkShift shift = selectCurrentShift(shifts, now);
            Map<String, Object> result = new HashMap<>();
            if (shift == null) {
                result.put("state", "NO_ACTIVE_SHIFT");
                result.put("shift", null);
                result.put("settlement", null);
                return result;
            }
            BigDecimal expectedAmount = dao.sumExpectedForShift(em, shipperId, shift);
            Map<String, Object> shiftMap = new HashMap<>();
            shiftMap.put("shiftId", shift.getShiftId());
            shiftMap.put("shiftDate", shift.getShiftDate());
            shiftMap.put("startTime", shift.getStartTime());
            shiftMap.put("endTime", shift.getEndTime());
            shiftMap.put("expectedAmount", expectedAmount);
            CodSettlement settlement = dao.findByShipperAndShift(em, shipperId, shift.getShiftId());
            result.put("state", settlement == null ? "READY_TO_SUBMIT" : settlement.getStatus());
            result.put("shift", shiftMap);
            result.put("settlement", settlement == null ? null : toMap(settlement));
            return result;
        } finally {
            em.close();
        }
    }

    public Map<String, Object> submit(int shipperId, int shiftId, BigDecimal submittedAmount) {
        submittedAmount = normalizeAmount(submittedAmount, "Số tiền thực nộp không hợp lệ");
        EntityManager em = entityManagers.get();
        LocalDateTime now = businessNow.get();
        try {
            em.getTransaction().begin();
            WorkShift shift = dao.findOwnedShiftForUpdate(em, shiftId, shipperId, now);
            if (!isCurrentShift(shift, now)) throw new IllegalArgumentException("Không tìm thấy ca hợp lệ");
            if (dao.findByShipperAndShift(em, shipperId, shiftId) != null) throw new SettlementConflictException("Ca này đã gửi bàn giao COD");
            CodSettlement settlement = new CodSettlement();
            settlement.setShipper(shift.getUser());
            settlement.setShift(shift);
            settlement.setStatus("SUBMITTED");
            settlement.setExpectedAmount(dao.sumExpectedForShift(em, shipperId, shift));
            settlement.setSubmittedAmount(submittedAmount);
            em.persist(settlement);
            em.flush();
            em.getTransaction().commit();
            return toMap(settlement);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            if (isDuplicateSettlementFailure(e)) throw new SettlementConflictException("Ca này đã gửi bàn giao COD");
            throw e;
        } finally {
            em.close();
        }
    }

    public List<Map<String, Object>> listForShipper(int shipperId) {
        return dao.listByShipper(shipperId).stream().map(this::toMap).toList();
    }

    public List<Map<String, Object>> listForAdmin(String status) {
        return dao.listByStatus(status).stream().map(this::toMap).toList();
    }

    static void applyVerification(CodSettlement settlement, String expectedStatus, String status, BigDecimal verifiedAmount, String reason, User admin, LocalDateTime now) {
        if (!settlement.getStatus().equals(expectedStatus) || !"SUBMITTED".equals(settlement.getStatus())) throw new SettlementConflictException("Bàn giao COD đã thay đổi trạng thái");
        validateVerification(status, settlement.getSubmittedAmount(), verifiedAmount, reason);
        settlement.setStatus(status);
        settlement.setVerifiedAmount(normalizeAmount(verifiedAmount, "Số tiền kiểm đếm không hợp lệ"));
        settlement.setReason("SETTLED".equals(status) ? null : reason.trim());
        settlement.setReceivedBy(admin);
        settlement.setVerifiedAt(now);
    }

    public Map<String, Object> verify(int adminId, int settlementId, String expectedStatus, String status, BigDecimal verifiedAmount, String reason) {
        if (expectedStatus == null || expectedStatus.isBlank()) throw new IllegalArgumentException("Trạng thái dự kiến không hợp lệ");
        EntityManager em = entityManagers.get();
        try {
            em.getTransaction().begin();
            User admin = em.find(User.class, adminId, LockModeType.PESSIMISTIC_READ);
            if (admin == null || !"ADMIN".equals(admin.getRole()) || !"ACTIVE".equals(admin.getStatus())) throw new SecurityException("Không có quyền xác nhận bàn giao COD");
            CodSettlement settlement = dao.findForUpdate(em, settlementId);
            if (settlement == null) throw new SettlementNotFoundException("Không tìm thấy bàn giao COD");
            applyVerification(settlement, expectedStatus, status, verifiedAmount, reason, admin, businessNow.get());
            em.flush();
            em.getTransaction().commit();
            return toMap(settlement);
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    static BigDecimal difference(BigDecimal submitted, BigDecimal expected) {
        return submitted.subtract(expected).setScale(2);
    }

    Map<String, Object> toMap(CodSettlement settlement) {
        Map<String, Object> result = new HashMap<>();
        result.put("settlementId", settlement.getSettlementId());
        result.put("shipperId", settlement.getShipper().getUserId());
        result.put("shipperName", settlement.getShipper().getFullName());
        result.put("shiftId", settlement.getShift().getShiftId());
        result.put("shiftDate", settlement.getShift().getShiftDate());
        result.put("startTime", settlement.getShift().getStartTime());
        result.put("endTime", settlement.getShift().getEndTime());
        result.put("status", settlement.getStatus());
        result.put("expectedAmount", settlement.getExpectedAmount());
        result.put("submittedAmount", settlement.getSubmittedAmount());
        result.put("differenceAmount", difference(settlement.getSubmittedAmount(), settlement.getExpectedAmount()));
        result.put("verifiedAmount", settlement.getVerifiedAmount());
        result.put("reason", settlement.getReason());
        result.put("receivedByName", settlement.getReceivedBy() == null ? null : settlement.getReceivedBy().getFullName());
        result.put("submittedAt", settlement.getSubmittedAt());
        result.put("verifiedAt", settlement.getVerifiedAt());
        return result;
    }
}
