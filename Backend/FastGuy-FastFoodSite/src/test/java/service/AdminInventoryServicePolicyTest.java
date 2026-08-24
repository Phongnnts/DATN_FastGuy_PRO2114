package service;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import exception.InventoryItemConflictException;
import entity.ProductVariant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

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

    @Test void ingredientModeRecipeCannotBeDeactivatedBeforeAnyRecipeRowsAreTouched() {
        ProductVariant variant=new ProductVariant();variant.setInventoryMode("INGREDIENT");
        boolean[] touched={false};
        EntityTransaction transaction=(EntityTransaction)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{EntityTransaction.class},(p,m,a)->switch(m.getName()){case "isActive"->true;default->null;});
        EntityManager em=(EntityManager)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{EntityManager.class},(p,m,a)->switch(m.getName()){
            case "getTransaction"->transaction;
            case "find"->variant;
            case "createQuery","persist","remove","merge","flush"->{touched[0]=true;yield null;}
            default->null;
        });
        AdminRecipeService service=new AdminRecipeService(()->em);

        AdminRecipeService.ModeNotReadyException error=assertThrows(AdminRecipeService.ModeNotReadyException.class,()->service.replace(1,BigDecimal.ONE,false,List.of(1),List.of(BigDecimal.ONE),LocalDateTime.parse("2026-08-24T10:11:12")));

        assertEquals(AdminRecipeService.INGREDIENT_RECIPE_ACTIVE_REQUIRED,error.getMessage());
        assertFalse(touched[0],"The variant lock must be followed by the conflict guard before recipe queries, deletes, or writes");
    }

    @Test void secondClientStaleVersionFailsClosedBeforeMutation() {
        LocalDateTime firstRead=LocalDateTime.parse("2026-08-24T10:11:12");
        LocalDateTime afterFirstSave=LocalDateTime.parse("2026-08-24T10:11:13");
        int[] mutations={0};

        AdminRecipeService.requireExpectedVersion(firstRead,firstRead);mutations[0]++;
        assertThrows(AdminRecipeService.OptimisticConflictException.class,()->AdminRecipeService.requireExpectedVersion(afterFirstSave,firstRead));
        assertThrows(AdminRecipeService.OptimisticConflictException.class,()->AdminRecipeService.requireExpectedVersion(null,firstRead));

        assertEquals(1,mutations[0]);
    }

    @Test void recipeAndVariantCallbacksProduceMonotonicSecondPrecisionVersions() throws Exception {
        LocalDateTime current=LocalDateTime.now().plusSeconds(5).withNano(0);
        entity.Recipe recipe=new entity.Recipe();
        java.lang.reflect.Field recipeVersion=entity.Recipe.class.getDeclaredField("updatedAt");recipeVersion.setAccessible(true);recipeVersion.set(recipe,current);
        Method recipeUpdate=entity.Recipe.class.getDeclaredMethod("preUpdate");recipeUpdate.setAccessible(true);recipeUpdate.invoke(recipe);
        ProductVariant variant=new ProductVariant();variant.setUpdatedAt(current);
        Method variantUpdate=ProductVariant.class.getDeclaredMethod("preUpdate");variantUpdate.setAccessible(true);variantUpdate.invoke(variant);

        assertEquals(current.plusSeconds(1),recipe.getUpdatedAt());
        assertEquals(current.plusSeconds(1),variant.getUpdatedAt());
        assertEquals(0,recipe.getUpdatedAt().getNano());
        assertEquals(0,variant.getUpdatedAt().getNano());
    }

    @Test void childOnlyRecipeUpdateAdvancesVersionAndRapidUpdatesRemainStrictlyIncreasing() throws Exception {
        LocalDateTime current=LocalDateTime.now().plusSeconds(5).withNano(0);
        entity.Recipe recipe=new entity.Recipe();
        java.lang.reflect.Field recipeVersion=entity.Recipe.class.getDeclaredField("updatedAt");recipeVersion.setAccessible(true);recipeVersion.set(recipe,current);

        recipe.advanceUpdatedAt();
        LocalDateTime first=recipe.getUpdatedAt();
        recipe.advanceUpdatedAt();

        assertEquals(current.plusSeconds(1),first);
        assertEquals(first.plusSeconds(1),recipe.getUpdatedAt());
        assertEquals(0,recipe.getUpdatedAt().getNano());
    }

    @Test void rapidVariantSettingsUpdatesRemainStrictlyIncreasing() {
        LocalDateTime current=LocalDateTime.now().plusSeconds(5).withNano(0);
        ProductVariant variant=new ProductVariant();variant.setUpdatedAt(current);

        variant.advanceUpdatedAt();
        LocalDateTime first=variant.getUpdatedAt();
        variant.advanceUpdatedAt();

        assertEquals(current.plusSeconds(1),first);
        assertEquals(first.plusSeconds(1),variant.getUpdatedAt());
        assertEquals(0,variant.getUpdatedAt().getNano());
    }

    @Test void preUpdateDoesNotDoubleExplicitVersionAdvance() throws Exception {
        LocalDateTime current=LocalDateTime.now().plusSeconds(5).withNano(0);
        entity.Recipe recipe=new entity.Recipe();
        java.lang.reflect.Field recipeVersion=entity.Recipe.class.getDeclaredField("updatedAt");recipeVersion.setAccessible(true);recipeVersion.set(recipe,current);
        ProductVariant variant=new ProductVariant();variant.setUpdatedAt(current);
        Method recipeUpdate=entity.Recipe.class.getDeclaredMethod("preUpdate");recipeUpdate.setAccessible(true);
        Method variantUpdate=ProductVariant.class.getDeclaredMethod("preUpdate");variantUpdate.setAccessible(true);

        recipe.advanceUpdatedAt();recipeUpdate.invoke(recipe);
        variant.advanceUpdatedAt();variantUpdate.invoke(variant);

        assertEquals(current.plusSeconds(1),recipe.getUpdatedAt());
        assertEquals(current.plusSeconds(1),variant.getUpdatedAt());
    }

    @Test void recipeReplaceAndVariantSettingsExplicitlyAdvanceVersionsBeforeFlush() throws Exception {
        String source=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/service/AdminRecipeService.java"));
        String replace=source.substring(source.indexOf("public Map<String,Object> replace(int variantId,BigDecimal yield,boolean active,List<Integer> ids,List<BigDecimal> quantities,LocalDateTime expected)"),source.indexOf("public Map<String,Object> updateSettings(int variantId,String mode,LocalDateTime expected)"));
        String settings=source.substring(source.indexOf("public Map<String,Object> updateSettings(int variantId,String mode,LocalDateTime expected)"),source.indexOf("public Map<String,Object> capacity"));

        assertTrue(replace.indexOf("r.advanceUpdatedAt()")>=0&&replace.indexOf("r.advanceUpdatedAt()")<replace.indexOf("em.flush()"));
        assertTrue(settings.indexOf("v.advanceUpdatedAt()")>=0&&settings.indexOf("v.advanceUpdatedAt()")<settings.indexOf("em.flush()"));
    }

    @Test void referencedRecipeItemBlocksTypeChangeAndDeactivationButNotMetadataEdits() {
        BigDecimal quantity=new BigDecimal("12.0000");
        assertThrows(InventoryItemConflictException.class,()->AdminInventoryService.requireRecipeItemInvariant(true,"INGREDIENT","FINISHED_GOOD",true,true,quantity));
        assertThrows(InventoryItemConflictException.class,()->AdminInventoryService.requireRecipeItemInvariant(true,"INGREDIENT","INGREDIENT",true,false,quantity));
        assertDoesNotThrow(()->AdminInventoryService.requireRecipeItemInvariant(true,"INGREDIENT","INGREDIENT",true,true,quantity));
        assertDoesNotThrow(()->AdminInventoryService.requireRecipeItemInvariant(false,"INGREDIENT","FINISHED_GOOD",true,false,quantity));
    }

    @Test void itemUpdateLocksItemBeforeReferenceCheckWithoutLockingRecipeParents() throws Exception {
        String source=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/service/AdminInventoryService.java"));
        String update=source.substring(source.indexOf("public Map<String,Object> update(int id,String code"),source.indexOf("public Map<String,Object> mutate",source.indexOf("public Map<String,Object> update(int id,String code")));
        int itemLock=update.indexOf("em.find(InventoryItem.class,id,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)");
        int recipeReference=update.indexOf("FROM RecipeItem r WHERE r.inventoryItem.inventoryItemId=:id");
        assertTrue(itemLock>=0&&recipeReference>itemLock,"Item-first locking makes recipe replace wait on the item; update never waits back on variant/recipe");
        assertFalse(update.contains("em.find(ProductVariant.class"),"Conservative reference existence needs no parent lock and avoids item-to-variant lock cycles");
        assertFalse(update.contains("em.find(Recipe.class"),"Conservative reference existence needs no parent lock and avoids item-to-recipe lock cycles");
    }
}
