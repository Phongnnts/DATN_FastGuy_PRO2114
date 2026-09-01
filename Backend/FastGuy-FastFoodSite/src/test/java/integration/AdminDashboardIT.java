package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import service.AdminService;
import utils.DatabaseUtil;

class AdminDashboardIT {
    @Test
    void dashboardHasCanonicalShapeOnFastGuyDatabase() {
        EntityManager em = null;
        try {
            em = DatabaseUtil.getEntityManager();
            assertEquals("FastGuyDB", em.createNativeQuery("SELECT DB_NAME()").getSingleResult());

            Map<String, Object> data = new AdminService().getDashboard();
            Set<String> requiredKeys = Set.of(
                    "netCashRevenueToday", "activeOrderCount", "pendingRefundCount", "pendingCodCount", "lowStockItemCount", "staffingGapCount",
                    "activeOrdersByStatus", "operationalOrderCountToday", "operationalCompletedCountToday", "completionRateToday", "attentionItems", "sectionAvailability",
                    "revenueLast7Days", "topProductsLast7Days", "lowStockProducts");
            assertTrue(data.keySet().containsAll(requiredKeys));
            for (String key : List.of("activeOrderCount", "pendingRefundCount", "pendingCodCount", "lowStockItemCount", "staffingGapCount", "operationalOrderCountToday", "operationalCompletedCountToday")) {
                assertInstanceOf(Number.class, data.get(key), key);
            }
            assertInstanceOf(Number.class, data.get("completionRateToday"));
            assertInstanceOf(BigDecimal.class, data.get("netCashRevenueToday"));
            assertInstanceOf(Map.class, data.get("activeOrdersByStatus"));
            assertInstanceOf(List.class, data.get("attentionItems"));
            assertEquals(7, assertInstanceOf(List.class, data.get("revenueLast7Days")).size());
            assertTrue(assertInstanceOf(List.class, data.get("topProductsLast7Days")).size() <= 5);
            assertTrue(assertInstanceOf(List.class, data.get("lowStockProducts")).size() <= 5);

            Map<?, ?> availability = assertInstanceOf(Map.class, data.get("sectionAvailability"));
            assertEquals(Set.of("financial", "orders", "refunds", "cod", "inventory", "staffing"), availability.keySet());
            assertTrue(availability.values().stream().allMatch(value -> Set.of("AVAILABLE", "UNAVAILABLE").contains(value)));
        } finally {
            try {
                if (em != null && em.isOpen()) em.close();
            } finally {
                DatabaseUtil.close();
            }
        }
    }
}
