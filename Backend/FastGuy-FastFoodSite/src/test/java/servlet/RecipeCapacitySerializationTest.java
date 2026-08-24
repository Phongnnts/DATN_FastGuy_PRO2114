package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import service.AdminRecipeService;
import utils.ApiResponse;
import utils.JsonUtil;

class RecipeCapacitySerializationTest {
    @Test void providerSerializesOnlyContractedCapacityFields() throws Exception {
        var item=new AdminRecipeService.CapacityItem(3,"Flour","G",new BigDecimal("10"),BigDecimal.ONE,BigDecimal.ONE,new BigDecimal("2"),true);
        var stock=new AdminRecipeService.CapacityStock(7,"INGREDIENT",new BigDecimal("20"),BigDecimal.ONE,true,List.of(item));
        JsonNode root=JsonUtil.getMapper().readTree(JsonUtil.toJson(ApiResponse.ok(AdminRecipeService.calculateCapacity(stock,Instant.parse("2026-08-24T12:00:00Z")))));
        JsonNode data=root.get("data");

        assertEquals(Set.of("variantId","inventoryMode","availableServings","limitingInventoryItemId","calculatedAt","variantPrice","ingredients","costStatus","recipeCostPerServing","foodCostPercent","missingCostItemCount","missingCostItems"),fields(data));
        assertEquals("2026-08-24T12:00:00Z",data.get("calculatedAt").asText());
        assertEquals(Set.of("inventoryItemId","name","baseUnit","onHandQuantity","reservedQuantity","availableQuantity","requiredPerServing","availableServings","averageUnitCost","costPerServing","costAvailable","limiting","active"),fields(data.get("ingredients").get(0)));
    }

    private Set<String> fields(JsonNode node){Set<String> fields=new java.util.HashSet<>();node.fieldNames().forEachRemaining(fields::add);return fields;}
}
