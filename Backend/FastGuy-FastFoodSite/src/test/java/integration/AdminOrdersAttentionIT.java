package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import dao.OrdersDAO;
import entity.Orders;
import service.AdminOrderAttentionPolicy;
import utils.DatabaseUtil;

class AdminOrdersAttentionIT {
    @Test
    void disposableDatabaseReturnsAllAttentionReasonsWithoutDuplicatesInPriorityOrder() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("FASTGUY_DISPOSABLE_DB")));
        String runId = System.getenv("FASTGUY_E2E_RUN_ID");
        Assumptions.assumeTrue(runId != null && !runId.isBlank());
        try {
            OrdersDAO dao = new OrdersDAO();
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            List<Orders> fixture = AdminOrderAttentionPolicy.sort(dao.findAttentionCandidates(), now).stream()
                    .filter(order -> order.getOrderCode() != null && order.getOrderCode().contains(runId))
                    .toList();

            assertEquals(List.of("E2E-" + runId + "-MULTI", "E2E-" + runId + "-OVERDUE", "E2E-" + runId + "-REFUND"),
                    fixture.stream().map(Orders::getOrderCode).toList());
            assertEquals(List.of("DELIVERY_FAILED", "PENDING_REFUND"), AdminOrderAttentionPolicy.reasons(fixture.get(0), now));
            assertEquals(List.of("PROCESSING_OVERDUE"), AdminOrderAttentionPolicy.reasons(fixture.get(1), now));
            assertEquals(List.of("PENDING_REFUND"), AdminOrderAttentionPolicy.reasons(fixture.get(2), now));
            assertEquals(1, fixture.stream().filter(order -> order.getOrderCode().endsWith("-MULTI")).count());
            assertTrue(dao.countAttentionOverdue(now) >= 1);
        } finally {
            DatabaseUtil.close();
        }
    }
}
