package dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class InventoryTransactionDAOPolicyTest {
    @Test
    void conditionsIncludeAllFiltersWhenPresent() {
        List<String> conditions = InventoryTransactionDAO.buildConditions(1, 2, "RESERVE",
                LocalDateTime.of(2024, 1, 1, 0, 0), LocalDateTime.of(2024, 1, 2, 0, 0));
        assertEquals(5, conditions.size());
        assertTrue(conditions.contains("t.inventoryItem.inventoryItemId = :inventoryItemId"));
        assertTrue(conditions.contains("t.order.orderId = :orderId"));
        assertTrue(conditions.contains("t.transactionType = :transactionType"));
        assertTrue(conditions.contains("t.createdAt >= :fromDate"));
        assertTrue(conditions.contains("t.createdAt < :toDate"));
    }

    @Test
    void conditionsEmptyWhenAbsent() {
        assertTrue(InventoryTransactionDAO.buildConditions(null, null, null, null, null).isEmpty());
    }

    @Test
    void blankTransactionTypeSkipped() {
        List<String> conditions = InventoryTransactionDAO.buildConditions(null, null, "  ", null, null);
        assertFalse(conditions.contains("t.transactionType = :transactionType"));
    }

    @Test
    void whereClauseOmitsKeywordWhenNoConditions() {
        assertEquals("", InventoryTransactionDAO.where(List.of()));
        assertEquals(" WHERE t.variant.variantId = :variantId", InventoryTransactionDAO.where(List.of("t.variant.variantId = :variantId")));
    }

    @Test
    void firstResultComputesOffset() {
        assertEquals(0, InventoryTransactionDAO.firstResult(0, 50));
        assertEquals(100, InventoryTransactionDAO.firstResult(2, 50));
        assertEquals(200, InventoryTransactionDAO.firstResult(1, 200));
    }

    @Test
    void toEndGuardsLocalDateMax() {
        assertEquals(LocalDateTime.MAX, InventoryTransactionDAO.toEnd(LocalDate.MAX));
        assertEquals(LocalDate.of(2024, 1, 3).atStartOfDay(), InventoryTransactionDAO.toEnd(LocalDate.of(2024, 1, 2)));
    }

    @Test
    void toDtoFormatsCreatedAtAsIso() {
        Object[] row = {11, 33, 22, "RESERVE", 5, 4, 9, "ORDER", "22", "REASON", "ghi chu", 7,
                new java.math.BigDecimal("0.1200"), new java.math.BigDecimal("0.6000"), 8, 9,
                LocalDateTime.of(2024, 5, 6, 7, 8, 9)};
        Map<String, Object> dto = InventoryTransactionDAO.toDto(row);
        assertEquals(11, dto.get("inventoryTransactionId"));
        assertEquals(33, dto.get("inventoryItemId"));
        assertEquals("RESERVE", dto.get("transactionType"));
        assertEquals(5, dto.get("quantity"));
        assertEquals(LocalDateTime.of(2024, 5, 6, 7, 8, 9), dto.get("createdAt"));
        assertEquals(22, dto.get("orderId"));
        assertEquals("REASON", dto.get("reason"));
        assertEquals("ghi chu", dto.get("note"));
        assertEquals(4, dto.get("quantityBefore"));
        assertEquals(9, dto.get("quantityAfter"));
        assertEquals(7, dto.get("createdBy"));
        assertEquals(new java.math.BigDecimal("0.1200"), dto.get("unitCostSnapshot"));
        assertEquals(new java.math.BigDecimal("0.6000"), dto.get("totalCost"));
        assertEquals(8, dto.get("goodsReceiptId"));
        assertEquals(9, dto.get("stockCountId"));
    }

    @Test
    void toDtoEmitsNullableQuantityBalances() {
        Object[] row = {11, 33, null, "ADJUSTMENT", 5, null, null, null, null, null, null, null,
                null, null, null, null, LocalDateTime.of(2024, 5, 6, 7, 8, 9)};
        Map<String, Object> dto = InventoryTransactionDAO.toDto(row);
        assertTrue(dto.containsKey("quantityBefore"));
        assertTrue(dto.containsKey("quantityAfter"));
        assertNull(dto.get("quantityBefore"));
        assertNull(dto.get("quantityAfter"));
    }
}
