package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InventoryAvailabilityServiceTest {
    @Test
    void computesExactYieldFormulaAndAggregatesSharedItemsWithOneFinalRounding() {
        InventoryAvailabilityService service = service(Map.of(
                1, variant(1, "INGREDIENT", recipe("3.0000", ingredient(10, "0.3333", true, "100.0000", "0"))),
                2, variant(2, "INGREDIENT", recipe("7.0000", ingredient(10, "0.2000", true, "100.0000", "0")))));

        Map<Integer, BigDecimal> demand = service.aggregateDemand(null, ordered(1, 2, 2, 3));

        assertEquals(new BigDecimal("0.3079"), demand.get(10));
    }

    @Test
    void handlesAllModesAndCapsCapacityAtIntegerMaximum() {
        InventoryAvailabilityService service = service(Map.of(
                1, variant(1, "UNTRACKED", null),
                2, variant(2, "SUSPENDED", null),
                3, variant(3, "FINISHED_GOOD", finished(30, true, "5.0000", "1.0000")),
                4, variant(4, "INGREDIENT", recipe("1.0000", ingredient(40, "0.0001", true, "999999999999999.9999", "0")))));

        assertEquals("AVAILABLE", service.availability(null, 1).status());
        assertEquals("SUSPENDED", service.availability(null, 2).status());
        assertEquals(5, service.availability(null, 3).servings());
        assertEquals(Integer.MAX_VALUE, service.availability(null, 4).servings());
    }

    @Test
    void missingRecipeAndInactiveIngredientsFailClosed() {
        InventoryAvailabilityService service = service(Map.of(
                1, variant(1, "INGREDIENT", null),
                2, variant(2, "INGREDIENT", recipe("1.0000", ingredient(20, "1.0000", false, "10.0000", "0")))));

        assertThrows(IllegalStateException.class, () -> service.aggregateDemand(null, Map.of(1, 1)));
        assertThrows(IllegalStateException.class, () -> service.aggregateDemand(null, Map.of(2, 1)));
        assertEquals("UNAVAILABLE", service.availability(null, 1).status());
        assertEquals("UNAVAILABLE", service.availability(null, 2).status());
    }

    @Test
    void validatesInputsModesQuantitiesAndDecimalOverflow() {
        InventoryAvailabilityService service = service(Map.of(
                1, variant(1, null, null),
                2, variant(2, "BOGUS", null),
                3, variant(3, "INGREDIENT", recipe("1.0000", ingredient(30, "600000000.0000", true, "999999999999999.9999", "0"))),
                4, variant(4, "INGREDIENT", recipe("1.0000", ingredient(30, "600000000.0000", true, "999999999999999.9999", "0")))));

        assertThrows(IllegalArgumentException.class, () -> service.aggregateDemand(null, null));
        assertThrows(IllegalArgumentException.class, () -> service.aggregateDemand(null, Map.of(0, 1)));
        assertThrows(IllegalArgumentException.class, () -> service.aggregateDemand(null, Map.of(1, 0)));
        assertThrows(IllegalArgumentException.class, () -> service.aggregateDemand(null, Map.of(1, Integer.MAX_VALUE)));
        assertThrows(IllegalStateException.class, () -> service.aggregateDemand(null, Map.of(1, 1)));
        assertThrows(IllegalStateException.class, () -> service.aggregateDemand(null, Map.of(2, 1)));
        assertThrows(ArithmeticException.class, () -> service.aggregateDemand(null, Map.of(3, 1_000_000, 4, 1_000_000)));
    }

    @Test
    void batchLoaderInvocationDoesNotGrowWithVariantCount() {
        AtomicInteger calls = new AtomicInteger();
        InventoryAvailabilityService service = new InventoryAvailabilityService((em, ids) -> {
            calls.incrementAndGet();
            Map<Integer, InventoryAvailabilityService.VariantStock> stocks = new LinkedHashMap<>();
            for (int id : ids) stocks.put(id, variant(id, "UNTRACKED", null));
            return stocks;
        });

        service.aggregateDemand(null, ordered(1, 1, 2, 1, 3, 1));

        assertEquals(1, calls.get());
    }

    private InventoryAvailabilityService service(Map<Integer, InventoryAvailabilityService.VariantStock> stocks) {
        return new InventoryAvailabilityService((em, ids) -> stocks);
    }

    private Map<Integer, Integer> ordered(int... pairs) {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) map.put(pairs[i], pairs[i + 1]);
        return map;
    }

    private InventoryAvailabilityService.VariantStock variant(int id, String mode, Object source) {
        return new InventoryAvailabilityService.VariantStock(id, mode,
                source instanceof InventoryAvailabilityService.RecipeStock recipe ? recipe : null,
                source instanceof InventoryAvailabilityService.ItemStock item ? item : null);
    }

    private InventoryAvailabilityService.RecipeStock recipe(String yield, InventoryAvailabilityService.IngredientStock... items) {
        return new InventoryAvailabilityService.RecipeStock(new BigDecimal(yield), List.of(items));
    }

    private InventoryAvailabilityService.IngredientStock ingredient(int id, String quantity, boolean active, String available, String minimum) {
        return new InventoryAvailabilityService.IngredientStock(item(id, active, available, minimum), new BigDecimal(quantity));
    }

    private InventoryAvailabilityService.ItemStock finished(int id, boolean active, String available, String minimum) {
        return item(id, active, available, minimum);
    }

    private InventoryAvailabilityService.ItemStock item(int id, boolean active, String available, String minimum) {
        return new InventoryAvailabilityService.ItemStock(id, active, new BigDecimal(available), new BigDecimal(minimum));
    }
}
