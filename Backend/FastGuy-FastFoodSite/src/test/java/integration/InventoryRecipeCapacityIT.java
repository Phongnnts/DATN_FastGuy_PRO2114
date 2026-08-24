package integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import service.AdminRecipeService;
import utils.DatabaseUtil;

class InventoryRecipeCapacityIT {
    @Test
    void sqlServerReturnsCapacityForExistingIngredientVariant() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("FASTGUY_DISPOSABLE_DB")),
                "Set FASTGUY_DISPOSABLE_DB=true only for a disposable database");
        EntityManager em = DatabaseUtil.getEntityManager();
        int variantId;
        try {
            String database = (String) em.createNativeQuery("SELECT DB_NAME()").getSingleResult();
            assertFalse(database.isBlank());
            variantId = ((Number) em.createNativeQuery("SELECT TOP 1 v.variant_id FROM ProductVariant v JOIN Recipe r ON r.variant_id=v.variant_id JOIN RecipeItem ri ON ri.recipe_id=r.recipe_id WHERE v.inventory_mode='INGREDIENT' AND r.active=1 ORDER BY v.variant_id").getSingleResult()).intValue();
        } finally {
            em.close();
        }

        Map<String,Object> capacity = new AdminRecipeService().capacity(variantId);

        assertEquals("INGREDIENT", capacity.get("inventoryMode"));
        assertNotNull(capacity.get("availableServings"));
        assertFalse(((List<?>) capacity.get("ingredients")).isEmpty());
    }
}
