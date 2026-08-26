package service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

import entity.Orders;

public class DispatchOrderPolicy {
    public LocalDateTime closingAt(LocalDateTime createdAt, LocalTime open, LocalTime close) {
        if (createdAt == null || open == null || close == null || open.equals(close)) return null;
        if (open.isBefore(close)) return createdAt.toLocalDate().atTime(close);
        return createdAt.toLocalTime().compareTo(open) >= 0
                ? createdAt.toLocalDate().plusDays(1).atTime(close)
                : createdAt.toLocalDate().atTime(close);
    }

    public String classify(Orders order, LocalDateTime now, LocalTime open, LocalTime close) {
        if (order == null) return null;
        if ("DELIVERY_FAILED".equals(order.getOrderStatus())) return "REVIEW";
        if (!"READY".equals(order.getOrderStatus()) || order.getReadyAt() == null) return null;
        LocalDateTime closing = closingAt(order.getCreatedAt(), open, close);
        if (closing != null && !closing.isAfter(now)) return null;
        if (isPriority(order, now, closing)) return "PRIORITY";
        return isNew(order, now) ? "NEW" : null;
    }

    boolean isNew(Orders order, LocalDateTime now) {
        return order.getReadyAt() != null && !order.getReadyAt().isBefore(now.minusMinutes(15));
    }

    boolean isPriority(Orders order, LocalDateTime now, LocalDateTime closing) {
        return !isNew(order, now) || closing != null && Duration.between(now, closing).toMinutes() <= 30;
    }

    public Long minutesUntilClose(Orders order, LocalDateTime now, LocalTime open, LocalTime close) {
        LocalDateTime closing = closingAt(order.getCreatedAt(), open, close);
        return closing == null ? null : Duration.between(now, closing).toMinutes();
    }
}
