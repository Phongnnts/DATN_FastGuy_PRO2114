package service;

import entity.Orders;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AdminOrderAttentionPolicy {

    public static final String PROCESSING_OVERDUE = "PROCESSING_OVERDUE";
    public static final String DELIVERY_FAILED = "DELIVERY_FAILED";
    public static final String PENDING_REFUND = "PENDING_REFUND";

    private AdminOrderAttentionPolicy() {}

    public static List<String> reasons(Orders order, LocalDateTime now) {
        List<String> reasons = new ArrayList<>(3);
        if ("DELIVERY_FAILED".equals(order.getOrderStatus())) reasons.add(
            DELIVERY_FAILED
        );
        if (OrderExpiryPolicy.isExpired(order, now)) reasons.add(
            PROCESSING_OVERDUE
        );
        if ("PENDING".equals(order.getRefundStatus())) reasons.add(
            PENDING_REFUND
        );
        return List.copyOf(reasons);
    }

    public static List<Orders> sort(List<Orders> orders, LocalDateTime now) {
        return orders
            .stream()
            .filter(order -> !reasons(order, now).isEmpty())
            .sorted(
                Comparator.comparingInt((Orders order) ->
                    priority(reasons(order, now))
                )
                    .thenComparing(
                        AdminOrderAttentionPolicy::relevantTime,
                        Comparator.nullsLast(LocalDateTime::compareTo)
                    )
                    .thenComparingInt(Orders::getOrderId)
            )
            .toList();
    }

    private static int priority(List<String> reasons) {
        if (reasons.contains(DELIVERY_FAILED)) return 0;
        if (reasons.contains(PROCESSING_OVERDUE)) return 1;
        return 2;
    }

    private static LocalDateTime relevantTime(Orders order) {
        if (
            "DELIVERY_FAILED".equals(order.getOrderStatus())
        ) return order.getDeliveryFailedAt();
        if (
            OrderExpiryPolicy.isCutoffCancellationCandidate(order)
        ) return order.getStatusEnteredAt();
        return order.getCancelledAt() != null
            ? order.getCancelledAt()
            : order.getCreatedAt();
    }
}
