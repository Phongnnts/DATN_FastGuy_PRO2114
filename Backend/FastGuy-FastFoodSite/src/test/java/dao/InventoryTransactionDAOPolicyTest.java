package dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertTrue(conditions.contains("t.variant.variantId = :variantId"));
        assertTrue(conditions.contains("t.variant.product.productId = :productId"));
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
        assertEquals(0, InventoryTransactionDAO.firstResult(1, 50));
        assertEquals(100, InventoryTransactionDAO.firstResult(3, 50));
        assertEquals(0, InventoryTransactionDAO.firstResult(1, 200));
    }

    @Test
    void toEndGuardsLocalDateMax() {
        assertEquals(LocalDateTime.MAX, InventoryTransactionDAO.toEnd(LocalDate.MAX));
        assertEquals(LocalDate.of(2024, 1, 3).atStartOfDay(), InventoryTransactionDAO.toEnd(LocalDate.of(2024, 1, 2)));
    }

    @Test
    void toDtoFormatsCreatedAtAsIso() {
        Object[] row = {11, 22, "FG0001", 33, "S", 44, "Coke", "RESERVE", 5,
                LocalDateTime.of(2024, 5, 6, 7, 8, 9, 123_000_000), "REASON", "ghi chu", 4, 5, "Admin"};
        Map<String, Object> dto = InventoryTransactionDAO.toDto(row);
        assertEquals(11, dto.get("transactionId"));
        assertEquals("RESERVE", dto.get("type"));
        assertEquals(5, dto.get("quantity"));
        assertEquals("2024-05-06T07:08:09", dto.get("createdAt"));
        assertEquals(33, dto.get("variantId"));
        assertEquals("S", dto.get("variantName"));
        assertEquals(44, dto.get("productId"));
        assertEquals("Coke", dto.get("productName"));
        assertEquals(22, dto.get("orderId"));
        assertEquals("FG0001", dto.get("orderCode"));
        assertEquals("REASON", dto.get("reasonCode"));
        assertEquals("ghi chu", dto.get("note"));
        assertEquals(4, dto.get("quantityBefore"));
        assertEquals(5, dto.get("quantityAfter"));
        assertEquals("Admin", dto.get("createdByName"));
    }
}
