package servlet;

import entity.Orders;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryRecoveryMapperTest {
    private static String source(String file) throws Exception {
        Path path = Path.of("src/main/java/servlet", file);
        if (!Files.exists(path)) path = Path.of("Backend/FastGuy-FastFoodSite/src/main/java/servlet", file);
        return Files.readString(path);
    }

    @Test
    void failureQueueExtendsCommonListMapperBeforeRecoveryFields() throws Exception {
        String source = source("StaffOrderServlet.java");
        int start = source.indexOf("Map<String, Object> toFailureQueueItem");
        int end = source.indexOf("private Map<String, Object> toListItem", start);
        String mapper = source.substring(start, end);
        assertTrue(source.contains("toFailureQueueItem(o, toListItem(o))"));
        for (String field : List.of("deliveryAttemptCount", "deliveryAttemptLimit", "deliveryFailureCode", "failureNote", "deliveryFailedAt", "retryScheduledAt")) {
            assertTrue(mapper.contains("m.put(\"" + field + "\""), field);
        }
    }

    @Test
    void failureQueueMergeKeepsCommonAndRecoveryFieldsTogether() {
        Orders order = new Orders();
        order.setDeliveryAttemptCount(1);
        order.setDeliveryAttemptLimit(2);
        order.setDeliveryFailureCode("INVALID_ADDRESS");
        order.setFailureReason("Số nhà không tồn tại");
        Map<String, Object> items = Map.of("productName", "Burger", "quantity", 2);
        Map<String, Object> common = Map.of(
                "customerName", "Nguyễn An",
                "items", List.of(items),
                "finalAmount", new BigDecimal("125000"),
                "createdAt", "2026-08-14T09:30:00"
        );

        Map<String, Object> mapped = StaffOrderServlet.toFailureQueueItem(order, common);

        assertEquals("Nguyễn An", mapped.get("customerName"));
        assertEquals(List.of(items), mapped.get("items"));
        assertEquals(new BigDecimal("125000"), mapped.get("finalAmount"));
        assertEquals("2026-08-14T09:30:00", mapped.get("createdAt"));
        assertEquals(1, mapped.get("deliveryAttemptCount"));
        assertEquals(2, mapped.get("deliveryAttemptLimit"));
        assertEquals("INVALID_ADDRESS", mapped.get("deliveryFailureCode"));
        assertEquals("Số nhà không tồn tại", mapped.get("failureNote"));
    }

    @Test
    void customerHistoryMapperDropsNotesAndKeepsStatusAndTime() {
        List<Map<String, Object>> mapped = OrderServlet.toCustomerHistory(List.of(
                Map.of("status", "DELIVERY_FAILED", "time", "2026-08-14T10:00:00", "note", "internal route detail")
        ));
        assertEquals(List.of(Map.of("status", "DELIVERY_FAILED", "time", "2026-08-14T10:00:00")), mapped);
        assertFalse(mapped.get(0).containsKey("note"));
    }

    @Test
    void authenticatedAndPublicMappersUseCustomerHistorySanitizer() throws Exception {
        String source = source("OrderServlet.java");
        assertTrue(source.contains("data.put(\"statusHistory\", toCustomerHistory(savedHistory.isEmpty() ? history : savedHistory))"));
        assertTrue(source.contains("history = toCustomerHistory(savedHistory).stream()"));
    }
}
