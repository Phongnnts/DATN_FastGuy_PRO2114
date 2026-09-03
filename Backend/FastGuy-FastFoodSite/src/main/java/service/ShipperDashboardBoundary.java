package service;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ShipperDashboardBoundary {

    private ShipperDashboardBoundary() {}

    public static DateRange forDate(LocalDate date) {
        return new DateRange(
            date.atStartOfDay(),
            date.plusDays(1).atStartOfDay()
        );
    }

    public static boolean isTodayDelivery(
        LocalDateTime deliveredAt,
        LocalDateTime start,
        LocalDateTime createdAt,
        LocalDateTime end
    ) {
        return (
            deliveredAt != null &&
            !deliveredAt.isBefore(start) &&
            deliveredAt.isBefore(end)
        );
    }

    public record DateRange(LocalDateTime start, LocalDateTime end) {}
}
