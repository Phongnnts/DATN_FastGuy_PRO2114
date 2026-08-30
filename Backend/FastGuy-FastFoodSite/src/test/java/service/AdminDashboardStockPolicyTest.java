package service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dao.InventoryItemDAO;
import entity.InventoryItem;

class AdminDashboardStockPolicyTest {
    @Test
    void inventoryItemAvailableQuantityAndMinimumDriveRiskCounts() throws Exception {
        InventoryItem out = item("5.0000", "5.0000", "1.0000", true);
        InventoryItem low = item("10.0000", "7.5000", "3.0000", true);
        InventoryItem healthy = item("10.0000", "2.0000", "3.0000", true);
        InventoryItem inactive = item("1.0000", "1.0000", "2.0000", false);

        Method method = assertDoesNotThrow(() -> InventoryItemDAO.class.getDeclaredMethod("inventoryRiskCounts", List.class));
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Long> counts = (Map<String, Long>) method.invoke(null, List.of(out, low, healthy, inactive));

        assertEquals(Map.of("outOfStock", 1L, "lowStock", 1L, "lowStockItemCount", 2L), counts);
    }

    private InventoryItem item(String onHand, String reserved, String minimum, boolean active) {
        InventoryItem item = new InventoryItem();
        item.setOnHandQuantity(new BigDecimal(onHand));
        item.setReservedQuantity(new BigDecimal(reserved));
        item.setMinimumQuantity(new BigDecimal(minimum));
        item.setActive(active);
        return item;
    }
}
