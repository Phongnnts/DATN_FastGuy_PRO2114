package servlet;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RecipeCapacityApiContractTest {
    private final String contract = readContract();

    @Test void exposesAdminSettingsAndCapacityEndpoints() {
        assertTrue(contract.contains("  /admin/product-variants/{variantId}/inventory-settings:"));
        assertTrue(contract.contains("operationId: getVariantInventorySettings"));
        assertTrue(contract.contains("operationId: updateVariantInventorySettings"));
        assertTrue(contract.contains("  /admin/product-variants/{variantId}/inventory-capacity:"));
        assertTrue(contract.contains("operationId: getVariantInventoryCapacity"));
        assertFalse(contract.contains("inventory-capacity/preview"));
        for(String path:new String[]{"/admin/product-variants/{variantId}/inventory-settings:","/admin/product-variants/{variantId}/inventory-capacity:"}){
            String operation=contract.substring(contract.indexOf("  "+path),contract.indexOf("  /",contract.indexOf("  "+path)+3));
            for(String status:new String[]{"'400'","'401'","'403'","'404'","'409'","'500'"})assertTrue(operation.contains(status),path+" "+status);
        }
    }

    @Test void recipeRequestContainsRecipeFieldsOnly() {
        String schema = schema("RecipeRequest", "InventorySettings");
        assertTrue(schema.contains("required: [yieldQuantity, active, items, expectedUpdatedAt]"));
        assertTrue(schema.contains("expectedUpdatedAt: { type: [string, 'null'] }"));
        assertFalse(schema.contains("inventoryMode:"));
        assertTrue(schema.contains("additionalProperties: false"));
    }

    @Test void settingsAndCapacitySchemasAreStrictAndComplete() {
        String settings = schema("InventorySettings", "InventorySettingsRequest");
        assertTrue(settings.contains("required: [variantId, inventoryMode, updatedAt]"));
        assertTrue(settings.contains("additionalProperties: false"));

        String capacity = schema("InventoryCapacity", "InventoryCapacityResponse");
        assertTrue(capacity.contains("additionalProperties: false"));
        assertTrue(capacity.contains("required: [variantId, inventoryMode, availableServings, limitingInventoryItemId, calculatedAt, variantPrice, ingredients, costStatus, recipeCostPerServing, foodCostPercent, missingCostItemCount, missingCostItems]"));
        for (String field : new String[]{"availableServings", "limitingInventoryItemId", "recipeCostPerServing", "foodCostPercent"})
            assertTrue(capacity.contains(field + ": { oneOf:"), field);

        String ingredient = schema("InventoryCapacityIngredient", "MissingCostItem");
        assertTrue(ingredient.contains("additionalProperties: false"));
        assertTrue(ingredient.contains("required: [inventoryItemId, name, baseUnit, onHandQuantity, reservedQuantity, availableQuantity, requiredPerServing, availableServings, averageUnitCost, costPerServing, costAvailable, limiting, active]"));
        assertTrue(ingredient.contains("costPerServing: { oneOf:"));
        assertTrue(ingredient.contains("description: Zero means cost is missing for capacity calculations."));

        String missing = schema("MissingCostItem", "InventoryCapacity");
        assertTrue(missing.contains("required: [inventoryItemId, name]"));
        assertTrue(missing.contains("additionalProperties: false"));
        assertTrue(capacity.contains("enum: [COMPLETE, INCOMPLETE, NOT_APPLICABLE]"));
    }

    @Test void adminProductVariantsRequireInventoryModeWithoutChangingPublicVariants() {
        String admin = schema("AdminVariantDetail", "AdminModifierOptionDetail");
        assertTrue(admin.contains("required: [variantId, variantName, price, originalPrice, sku, quantityAvailable, inventoryMode, isDefault, status, updatedAt]"));
        assertTrue(admin.contains("inventoryMode: { $ref: '#/components/schemas/InventoryMode' }"));

        String publicVariant = schema("ProductVariantSummary", "InStockAvailability");
        assertFalse(publicVariant.contains("inventoryMode:"));
    }

    @Test void capacitySourceUsesDeterministicPessimisticReadLocks() throws Exception {
        String source=Files.readString(Path.of("src/main/java/service/AdminRecipeService.java"));
        assertTrue(source.contains("LockModeType.PESSIMISTIC_READ"));
        assertTrue(source.contains("ORDER BY ri.inventoryItem.inventoryItemId"));
        assertTrue(source.contains("ORDER BY m.inventoryItem.inventoryItemId"));
        assertTrue(source.contains("em.find(InventoryItem.class,itemId,LockModeType.PESSIMISTIC_READ)"));
    }

    @Test void settingsValidationLocksVariantRecipeAndReferencedItemsInDeterministicOrder() throws Exception {
        String source=Files.readString(Path.of("src/main/java/service/AdminRecipeService.java"));
        int settings=source.indexOf("public Map<String,Object> updateSettings");
        int capacity=source.indexOf("public Map<String,Object> capacity",settings);
        String method=source.substring(settings,capacity);
        int variant=method.indexOf("ProductVariant.class,variantId,LockModeType.PESSIMISTIC_WRITE");
        int recipe=method.indexOf("Recipe.class).setParameter(\"id\",variantId).setLockMode(LockModeType.PESSIMISTIC_READ)");
        int recipeItems=method.indexOf("ORDER BY ri.inventoryItem.inventoryItemId",recipe);
        int ingredient=method.indexOf("em.find(InventoryItem.class,itemId,LockModeType.PESSIMISTIC_READ)",recipeItems);
        int finishedMappings=method.indexOf("ORDER BY m.inventoryItem.inventoryItemId");
        assertTrue(variant>=0&&recipe>variant&&recipeItems>recipe&&ingredient>recipeItems);
        assertTrue(finishedMappings>variant);
        assertFalse(method.contains("SELECT COUNT(r)"));
    }

    @Test void recipeReplacementUsesTheSameVariantRecipeItemLockOrder() throws Exception {
        String source=Files.readString(Path.of("src/main/java/service/AdminRecipeService.java"));
        String method=source.substring(source.indexOf("public Map<String,Object> replace"),source.indexOf("public Map<String,Object> settings"));
        int variant=method.indexOf("ProductVariant.class,variantId,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE");
        int recipe=method.indexOf("Recipe.class).setParameter(\"id\",variantId).setLockMode(LockModeType.PESSIMISTIC_WRITE)");
        int sortedItems=method.indexOf("ids.stream().sorted().toList()",recipe);
        assertTrue(variant>=0&&recipe>variant&&sortedItems>recipe);
        assertTrue(method.contains("requireRecipeIngredient(item)"));
    }

    private String schema(String from, String to) {
        return contract.substring(contract.indexOf("    " + from + ":"), contract.indexOf("    " + to + ":"));
    }

    private static String readContract() {
        try { return Files.readString(Path.of("../../openapi/fastguy.yaml")); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
