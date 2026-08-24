package service;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminInventoryServicePolicyTest {
    @Test void decimalContractRejectsScaleOverflowAndOutOfRange() {
        assertEquals(new BigDecimal("1.2300"), AdminInventoryService.decimal("1.23", false));
        assertThrows(IllegalArgumentException.class, () -> AdminInventoryService.decimal("1.00001", false));
        assertThrows(IllegalArgumentException.class, () -> AdminInventoryService.decimal("-1", false));
    }

    @Test void validatesUnitsAndRecipeDuplicates() {
        assertThrows(IllegalArgumentException.class, () -> AdminInventoryService.unit("KG"));
        assertThrows(IllegalArgumentException.class, () -> AdminRecipeService.validateItems(List.of(1, 1), List.of(new BigDecimal("1"), new BigDecimal("2"))));
    }

    @Test void ingredientModeRequiresActiveRecipe() {
        assertThrows(IllegalArgumentException.class, () -> AdminRecipeService.validateMode("INGREDIENT", false));
        assertDoesNotThrow(() -> AdminRecipeService.validateMode("UNTRACKED", false));
    }

    @Test void movingAverageCostIncludesExistingStockValue() throws Exception {
        Method method=AdminInventoryService.class.getDeclaredMethod("movingAverageCost",BigDecimal.class,BigDecimal.class,BigDecimal.class,BigDecimal.class);
        assertEquals(new BigDecimal("0.1300"),method.invoke(null,new BigDecimal("10000"),new BigDecimal("0.1200"),new BigDecimal("5000"),new BigDecimal("0.1500")));
    }

    @Test void movingAverageCostUsesReceiptCostForEmptyStock() throws Exception {
        Method method=AdminInventoryService.class.getDeclaredMethod("movingAverageCost",BigDecimal.class,BigDecimal.class,BigDecimal.class,BigDecimal.class);
        assertEquals(new BigDecimal("0.7143"),method.invoke(null,new BigDecimal("10"),BigDecimal.ZERO,new BigDecimal("4"),new BigDecimal("2.5")));
    }
}
