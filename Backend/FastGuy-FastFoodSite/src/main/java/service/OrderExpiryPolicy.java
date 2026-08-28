package service;

import entity.Orders;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public final class OrderExpiryPolicy {
    private static final Set<String> STORE_HELD = Set.of("PENDING", "CONFIRMED", "PREPARING", "READY");
    private static final Map<String, Long> MINUTES = Map.of("PENDING", 10L, "CONFIRMED", 15L, "PREPARING", 20L, "READY", 15L);

    private OrderExpiryPolicy() {}

    public record Metadata(LocalDateTime statusEnteredAt, LocalDateTime expiresAt, Long remainingSeconds, String timeoutPolicy) {}

    public static Metadata metadata(Orders order, LocalDateTime now) {
        if (order == null) return new Metadata(null, null, null, null);
        LocalDateTime entered = order.getStatusEnteredAt();
        if (entered == null || now == null || !STORE_HELD.contains(order.getOrderStatus())) return new Metadata(entered, null, null, null);
        LocalDateTime expires = entered.plusMinutes(timeoutMinutes(order));
        return new Metadata(entered, expires, Math.max(0, Duration.between(now, expires).getSeconds()), "AUTO_CANCEL");
    }

    public static boolean isExpired(Orders order, LocalDateTime now) {
        Metadata metadata = metadata(order, now);
        return metadata.expiresAt() != null && !metadata.expiresAt().isAfter(now);
    }

    private static long timeoutMinutes(Orders order) {
        return "PENDING".equals(order.getOrderStatus()) && "BANK_TRANSFER".equals(order.getPaymentMethod())
                && "UNPAID".equals(order.getPaymentStatus()) ? 15 : MINUTES.get(order.getOrderStatus());
    }

    public static boolean isCutoffCancellationCandidate(Orders order) {
        return order != null && STORE_HELD.contains(order.getOrderStatus());
    }
}
