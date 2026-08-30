package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class AdminDashboardAttentionPolicyTest {
    @Test
    void dashboardPublishesSixActionableAttentionTypes() throws Exception {
        Map<String, Object> data = new AdminDashboardTestFixture().service().getDashboard();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> attention = (List<Map<String, Object>>) data.get("attentionItems");

        assertEquals(Set.of("OVERDUE_PENDING_ORDERS", "DELIVERY_FAILED_ORDERS", "PENDING_REFUNDS", "STAFF_COVERAGE_GAPS", "LOW_STOCK_ITEMS", "PENDING_COD_SETTLEMENTS"),
                attention.stream().map(item -> (String) item.get("type")).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void failedRefundSectionDoesNotEraseSuccessfulSections() throws Exception {
        AdminDashboardTestFixture fixture = new AdminDashboardTestFixture();
        fixture.failPendingRefunds = true;

        Map<String, Object> data = fixture.service().getDashboard();
        @SuppressWarnings("unchecked")
        Map<String, String> availability = (Map<String, String>) data.get("sectionAvailability");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> attention = (List<Map<String, Object>>) data.get("attentionItems");

        assertEquals(Map.of(
                "financial", "AVAILABLE",
                "orders", "AVAILABLE",
                "refunds", "UNAVAILABLE",
                "cod", "AVAILABLE",
                "inventory", "AVAILABLE",
                "staffing", "AVAILABLE"), availability);
        assertEquals(0L, data.get("pendingRefundCount"));
        assertEquals(2L, data.get("lowStockItemCount"));
        assertFalse(attention.stream().anyMatch(item -> "PENDING_REFUNDS".equals(item.get("type"))));
        assertTrue(attention.stream().anyMatch(item -> "LOW_STOCK_ITEMS".equals(item.get("type"))));
    }

    @Test
    void overdueAttentionUsesStatusEnteredAtAndPolicyThresholdsInSql() throws Exception {
        String dao = Files.readString(Path.of("src/main/java/dao/OrdersDAO.java"));
        assertTrue(dao.contains("long countAttentionOverdue("));
        assertTrue(dao.contains("statusEnteredAt"));
        for (String minutes : new String[] {"10", "15", "20"}) assertTrue(dao.contains("minusMinutes(" + minutes + ")"), minutes);
    }
}
