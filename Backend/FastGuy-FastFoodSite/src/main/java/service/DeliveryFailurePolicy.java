package service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

public final class DeliveryFailurePolicy {
    public static final Set<String> REASON_CODES = Set.of(
            "CUSTOMER_UNREACHABLE",
            "INVALID_ADDRESS",
            "CUSTOMER_RESCHEDULED",
            "CUSTOMER_REJECTED",
            "SHIPPER_INCIDENT",
            "PRODUCT_INCIDENT"
    );

    private DeliveryFailurePolicy() {
    }

    public static boolean isValidFailure(String reasonCode, String note) {
        return reasonCode != null && REASON_CODES.contains(reasonCode) && note != null
                && !note.trim().isEmpty() && note.trim().length() <= 500;
    }

    public static String normalizeNote(String note) {
        if (note == null) return null;
        String normalized = note.trim();
        return normalized.isEmpty() || normalized.length() > 500 ? null : normalized;
    }

    public static boolean canRetry(int attemptCount, int attemptLimit) {
        return attemptCount >= 0 && attemptLimit > 0 && attemptCount < attemptLimit;
    }

    public static boolean isValidSchedule(String retryMode, LocalDateTime scheduledAt, LocalDateTime now,
                                          LocalTime openTime, LocalTime closeTime) {
        if ("IMMEDIATE".equals(retryMode)) return scheduledAt == null;
        if (!"SCHEDULED".equals(retryMode) || scheduledAt == null || now == null
                || openTime == null || closeTime == null) return false;
        return scheduledAt.isAfter(now) && !scheduledAt.isAfter(now.plusHours(24))
                && isOpen(openTime, closeTime, scheduledAt.toLocalTime());
    }

    private static boolean isOpen(LocalTime openTime, LocalTime closeTime, LocalTime time) {
        return openTime.equals(closeTime) || (openTime.isBefore(closeTime)
                ? !time.isBefore(openTime) && time.isBefore(closeTime)
                : !time.isBefore(openTime) || time.isBefore(closeTime));
    }
}
