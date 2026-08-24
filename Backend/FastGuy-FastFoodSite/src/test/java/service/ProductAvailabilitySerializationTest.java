package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class ProductAvailabilitySerializationTest {
    @Test
    void mapsPublicAvailabilityWithoutInventoryDetails() {
        InventoryAvailabilityService service = service(Map.of(
                1, variant(1, "FINISHED_GOOD", "0"),
                2, variant(2, "FINISHED_GOOD", "1"),
                3, variant(3, "FINISHED_GOOD", "3"),
                4, variant(4, "FINISHED_GOOD", "4"),
                5, new InventoryAvailabilityService.VariantStock(5, "UNTRACKED", null, null),
                6, new InventoryAvailabilityService.VariantStock(6, "SUSPENDED", null, null)));

        Map<Integer, Map<String, Object>> results = service.publicAvailability(null, List.of(1, 2, 3, 4, 5, 6));

        assertEquals(Map.of("availabilityStatus", "OUT_OF_STOCK"), results.get(1));
        assertEquals(Map.of("availabilityStatus", "LOW_STOCK", "remainingServings", 1), results.get(2));
        assertEquals(Map.of("availabilityStatus", "LOW_STOCK", "remainingServings", 3), results.get(3));
        assertEquals(Map.of("availabilityStatus", "IN_STOCK", "remainingServings", 4), results.get(4));
        assertEquals(Map.of("availabilityStatus", "UNTRACKED"), results.get(5));
        assertEquals(Map.of("availabilityStatus", "SUSPENDED"), results.get(6));
        results.values().forEach(this::assertPrivateFieldsAbsent);
    }

    @Test
    void corruptOrMissingInventoryFailsClosedPerVariant() {
        InventoryAvailabilityService service = service(new LinkedHashMap<>(Map.of(
                1, new InventoryAvailabilityService.VariantStock(1, "INGREDIENT", null, null),
                2, new InventoryAvailabilityService.VariantStock(2, "INGREDIENT", new InventoryAvailabilityService.RecipeStock(BigDecimal.ONE, List.of()), null),
                3, new InventoryAvailabilityService.VariantStock(3, "INGREDIENT", new InventoryAvailabilityService.RecipeStock(BigDecimal.ONE, List.of(new InventoryAvailabilityService.IngredientStock(new InventoryAvailabilityService.ItemStock(30, "INGREDIENT", false, BigDecimal.TEN, BigDecimal.ZERO), BigDecimal.ONE))), null),
                4, new InventoryAvailabilityService.VariantStock(4, "FINISHED_GOOD", null, null),
                5, new InventoryAvailabilityService.VariantStock(5, "BOGUS", null, null))));

        Map<Integer, Map<String, Object>> results = service.publicAvailability(null, List.of(1, 2, 3, 4, 5, 6));

        for (int id = 1; id <= 6; id++) assertEquals(Map.of("availabilityStatus", "OUT_OF_STOCK"), results.get(id));
    }

    @Test
    void hydratesAllPublicVariantsWithOneLoaderCall() {
        AtomicInteger calls = new AtomicInteger();
        InventoryAvailabilityService service = new InventoryAvailabilityService((em, ids) -> {
            calls.incrementAndGet();
            Map<Integer, InventoryAvailabilityService.VariantStock> stocks = new LinkedHashMap<>();
            ids.forEach(id -> stocks.put(id, new InventoryAvailabilityService.VariantStock(id, "UNTRACKED", null, null)));
            return stocks;
        });

        service.publicAvailability(null, List.of(1, 2, 3));

        assertEquals(1, calls.get());
    }

    private InventoryAvailabilityService service(Map<Integer, InventoryAvailabilityService.VariantStock> stocks) {
        return new InventoryAvailabilityService((em, ids) -> stocks);
    }

    private InventoryAvailabilityService.VariantStock variant(int id, String mode, String available) {
        return new InventoryAvailabilityService.VariantStock(id, mode, null,
                new InventoryAvailabilityService.ItemStock(100 + id, "FINISHED_GOOD", true, new BigDecimal(available), BigDecimal.ZERO));
    }

    private void assertPrivateFieldsAbsent(Map<String, Object> result) {
        assertFalse(result.containsKey("inventoryItemId"));
        assertFalse(result.containsKey("onHandQuantity"));
        assertFalse(result.containsKey("reservedQuantity"));
        assertFalse(result.containsKey("limitingItemId"));
    }
}
