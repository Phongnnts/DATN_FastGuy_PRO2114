package service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import entity.InventoryItem;
import entity.ProductVariant;
import entity.Recipe;
import entity.RecipeItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;

class AdminRecipeCapacityServiceTest {
    private final Instant now = Instant.parse("2026-08-24T12:00:00Z");

    @Test void subtractsReservedStockAndFloorsThe583Case() {
        Map<String,Object> result = AdminRecipeService.calculateCapacity(stock(1,"INGREDIENT","100.0000","1",true,
                item(8,"Chicken","G","800.0000","100.0000","1.2000","2.0000",true)),now);

        assertEquals(583,result.get("availableServings"));
        assertEquals(new BigDecimal("700.0000"),line(result,0).get("availableQuantity"));
        assertEquals(8,result.get("limitingInventoryItemId"));
    }

    @Test void appliesYieldAndChoosesLowestItemIdForLimitingTie() {
        Map<String,Object> result = AdminRecipeService.calculateCapacity(stock(2,"INGREDIENT","20.0000","20",true,
                item(20,"B","G","0.4","0","4","3",true),
                item(10,"A","G","0.2","0","2","4",true)),now);

        assertEquals(new BigDecimal("0.1"),line(result,0).get("requiredPerServing"));
        assertEquals(2,result.get("availableServings"));
        assertEquals(10,result.get("limitingInventoryItemId"));
        assertEquals(true,line(result,0).get("limiting"));
        assertEquals(false,line(result,1).get("limiting"));
    }

    @Test void missingCostMakesTotalsUnavailableAndListsExactItems() {
        Map<String,Object> result = AdminRecipeService.calculateCapacity(stock(3,"INGREDIENT","50","1",true,
                item(4,"No cost","ML","10","0","1","0",true),
                item(7,"Costed","G","10","0","1","2.5",true)),now);

        assertEquals("INCOMPLETE",result.get("costStatus"));
        assertNull(result.get("recipeCostPerServing"));
        assertNull(result.get("foodCostPercent"));
        assertEquals(1,result.get("missingCostItemCount"));
        assertEquals(List.of(Map.of("inventoryItemId",4,"name","No cost")),result.get("missingCostItems"));
        assertNull(line(result,0).get("costPerServing"));
        assertEquals(false,line(result,0).get("costAvailable"));
    }

    @Test void settingsReadinessRequiresTheSourceForTrackedModesOnly() {
        assertDoesNotThrow(() -> AdminRecipeService.requireModeReady("UNTRACKED",false,false));
        assertDoesNotThrow(() -> AdminRecipeService.requireModeReady("SUSPENDED",false,false));
        assertDoesNotThrow(() -> AdminRecipeService.requireModeReady("INGREDIENT",true,false));
        assertDoesNotThrow(() -> AdminRecipeService.requireModeReady("FINISHED_GOOD",false,true));
        assertEquals(AdminRecipeService.INGREDIENT_NOT_READY,assertThrows(AdminRecipeService.ModeNotReadyException.class,() -> AdminRecipeService.requireModeReady("INGREDIENT",false,true)).getMessage());
        assertEquals(AdminRecipeService.FINISHED_GOOD_NOT_READY,assertThrows(AdminRecipeService.ModeNotReadyException.class,() -> AdminRecipeService.requireModeReady("FINISHED_GOOD",true,false)).getMessage());
    }

    @Test void recipeLinesRequireActiveIngredientInventoryItems() {
        InventoryItem ingredient = inventoryItem("INGREDIENT", true);
        assertDoesNotThrow(() -> AdminRecipeService.requireRecipeIngredient(ingredient));

        IllegalArgumentException finishedGood = assertThrows(IllegalArgumentException.class,
                () -> AdminRecipeService.requireRecipeIngredient(inventoryItem("FINISHED_GOOD", true)));
        assertEquals("All recipe items must be active ingredients", finishedGood.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> AdminRecipeService.requireRecipeIngredient(inventoryItem("INGREDIENT", false)));
    }

    @Test void twoClientsExpectingNoRecipeAllowFirstCreateAndConflictSecond() {
        assertDoesNotThrow(() -> AdminRecipeService.requireExpectedVersion(null,null));
        LocalDateTime persisted=LocalDateTime.parse("2026-08-24T10:11:12");
        assertThrows(AdminRecipeService.OptimisticConflictException.class,
                () -> AdminRecipeService.requireExpectedVersion(persisted,null));
        assertThrows(AdminRecipeService.OptimisticConflictException.class,
                () -> AdminRecipeService.requireExpectedVersion(null,LocalDateTime.parse("2026-08-24T10:11:12")));
        assertDoesNotThrow(() -> AdminRecipeService.requireExpectedVersion(LocalDateTime.parse("2026-08-24T10:11:12"),LocalDateTime.parse("2026-08-24T10:11:12")));
    }

    @Test void malformedFinishedGoodRecipeCannotEnableIngredientMode() {
        AdminRecipeService service = new AdminRecipeService(() -> malformedRecipeEntityManager());

        AdminRecipeService.ModeNotReadyException error = assertThrows(AdminRecipeService.ModeNotReadyException.class,
                () -> service.updateSettings(1, "INGREDIENT", LocalDateTime.now().withNano(0)));

        assertEquals(AdminRecipeService.INGREDIENT_NOT_READY, error.getMessage());
    }

    @Test void malformedFinishedGoodRecipeHasNoCapacity() {
        Map<String,Object> result = new AdminRecipeService(() -> malformedRecipeEntityManager()).capacity(1);

        assertEquals(0, result.get("availableServings"));
        assertNull(result.get("limitingInventoryItemId"));
        assertEquals(List.of(), result.get("ingredients"));
        assertEquals("NOT_APPLICABLE", result.get("costStatus"));
    }

    @Test void computesCompleteRecipeCostAndFoodCostPercent() {
        Map<String,Object> result = AdminRecipeService.calculateCapacity(stock(4,"FINISHED_GOOD","10.0000","1",true,
                item(9,"Box","PIECE","8","3","1","2.5000",true)),now);

        assertEquals(5,result.get("availableServings"));
        assertEquals("COMPLETE",result.get("costStatus"));
        assertEquals(new BigDecimal("2.5000"),result.get("recipeCostPerServing"));
        assertEquals(new BigDecimal("25.0000"),result.get("foodCostPercent"));
    }

    @Test void modesAndInvalidSourcesFailClosedDeterministically() {
        Map<String,Object> untracked=AdminRecipeService.calculateCapacity(stock(5,"UNTRACKED","10","1",false),now);
        assertNull(untracked.get("availableServings"));
        assertEquals(List.of(),untracked.get("ingredients"));
        assertEquals("NOT_APPLICABLE",untracked.get("costStatus"));

        for(String mode:List.of("SUSPENDED","INGREDIENT","FINISHED_GOOD")){
            Map<String,Object> result=AdminRecipeService.calculateCapacity(stock(6,mode,"10","1",false),now);
            assertEquals(0,result.get("availableServings"));
            assertNull(result.get("limitingInventoryItemId"));
            assertEquals(List.of(),result.get("ingredients"));
            assertEquals("NOT_APPLICABLE",result.get("costStatus"));
        }
    }

    @SuppressWarnings("unchecked") private Map<String,Object> line(Map<String,Object> result,int index){return((List<Map<String,Object>>)result.get("ingredients")).get(index);}
    private AdminRecipeService.CapacityStock stock(int id,String mode,String price,String yield,boolean valid,AdminRecipeService.CapacityItem...items){return new AdminRecipeService.CapacityStock(id,mode,new BigDecimal(price),new BigDecimal(yield),valid,List.of(items));}
    private AdminRecipeService.CapacityItem item(int id,String name,String unit,String onHand,String reserved,String required,String cost,boolean active){return new AdminRecipeService.CapacityItem(id,name,unit,new BigDecimal(onHand),new BigDecimal(reserved),new BigDecimal(required),new BigDecimal(cost),active);}
    private InventoryItem inventoryItem(String type,boolean active){InventoryItem item=new InventoryItem();item.setItemType(type);item.setActive(active);return item;}

    @SuppressWarnings("unchecked")
    private EntityManager malformedRecipeEntityManager() {
        ProductVariant variant=new ProductVariant();variant.setVariantId(1);variant.setInventoryMode("INGREDIENT");variant.setPrice(BigDecimal.TEN);variant.setUpdatedAt(LocalDateTime.now().withNano(0));
        Recipe recipe=new Recipe();recipe.setActive(true);recipe.setYieldQuantity(BigDecimal.ONE);
        InventoryItem item=inventoryItem("FINISHED_GOOD",true);item.setName("Malformed");item.setBaseUnit("PIECE");item.setOnHandQuantity(BigDecimal.TEN);item.setReservedQuantity(BigDecimal.ZERO);item.setMinimumQuantity(BigDecimal.ZERO);item.setAverageUnitCost(BigDecimal.ONE);
        RecipeItem line=new RecipeItem();line.setInventoryItem(item);line.setQuantity(BigDecimal.ONE);
        EntityTransaction transaction=(EntityTransaction)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{EntityTransaction.class},(p,m,a)->m.getName().equals("isActive")?true:null);
        return (EntityManager)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{EntityManager.class},(p,m,a)->switch(m.getName()){
            case "getTransaction" -> transaction;
            case "find" -> a[0]==ProductVariant.class?variant:a[0]==InventoryItem.class?item:null;
            case "createQuery" -> {
                List<?> rows=((String)a[0]).contains("FROM RecipeItem")?List.of(line):((String)a[0]).contains("FROM Recipe r")?List.of(recipe):List.of();
                final Object[] query=new Object[1];query[0]=Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{TypedQuery.class},(q,qm,qa)->switch(qm.getName()){
                    case "setParameter","setLockMode" -> query[0];
                    case "getResultList" -> rows;
                    default -> null;
                });
                yield query[0];
            }
            default -> null;
        });
    }
}
