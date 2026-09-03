package service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InventoryCostingWorkflowTest {
    @Test void goodsReceiptsAcceptOnlyActiveIngredients() {
        entity.InventoryItem ingredient=new entity.InventoryItem();ingredient.setItemType("INGREDIENT");ingredient.setActive(true);
        assertDoesNotThrow(()->GoodsReceiptService.requireReceivableItem(ingredient));
        entity.InventoryItem finished=new entity.InventoryItem();finished.setItemType("FINISHED_GOOD");finished.setActive(true);
        assertThrows(IllegalArgumentException.class,()->GoodsReceiptService.requireReceivableItem(finished));
        ingredient.setActive(false);
        assertThrows(IllegalArgumentException.class,()->GoodsReceiptService.requireReceivableItem(ingredient));
    }

    @Test void receiptNormalizesOptionalSupplierWithoutPersistingNull() {
        assertEquals("Không khai báo", GoodsReceiptService.supplier(null));
        assertEquals("Không khai báo", GoodsReceiptService.supplier("  "));
        assertEquals("Fresh Food", GoodsReceiptService.supplier(" Fresh Food "));
    }

    @Test void receiptCalculatesBaseQuantityLineTotalAndWeightedCostAtScaleFour() {
        assertEquals(new BigDecimal("5000.0000"), GoodsReceiptService.baseQuantity(new BigDecimal("5"), new BigDecimal("1000")));
        assertEquals(new BigDecimal("750.0000"), GoodsReceiptService.lineTotal(new BigDecimal("5"), new BigDecimal("150")));
        assertEquals(new BigDecimal("0.1300"), GoodsReceiptService.weightedAverage(new BigDecimal("10000"), new BigDecimal("0.1200"), new BigDecimal("5000"), new BigDecimal("0.1500")));
        assertEquals(new BigDecimal("0.7143"), GoodsReceiptService.weightedAverage(new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("4"), new BigDecimal("2.5")));
    }

    @Test void stockCountCalculatesSignedVarianceAndAbsoluteCost() {
        assertEquals(new BigDecimal("-2.5000"), StockCountService.variance(new BigDecimal("7.5"), new BigDecimal("10")));
        assertEquals(new BigDecimal("5.0000"), StockCountService.varianceCost(new BigDecimal("-2.5"), new BigDecimal("2")));
        assertThrows(IllegalArgumentException.class, () -> StockCountService.requireReservedFloor(new BigDecimal("4"), new BigDecimal("5")));
    }

    @Test void stockCountApprovalRejectsChangedCostAndKeepsDraftSnapshot() {
        assertDoesNotThrow(()->StockCountService.requireCurrentCost(new BigDecimal("2.5000"),new BigDecimal("2.5")));
        assertThrows(IllegalStateException.class,()->StockCountService.requireCurrentCost(new BigDecimal("2.5000"),new BigDecimal("2.6000")));
    }

    @Test void summarySeparatesWasteAndNegativeCountLoss() {
        List<Map<String,Object>> rows=List.of(
                Map.of("transactionType","RECEIPT","quantity",new BigDecimal("2"),"totalCost",new BigDecimal("20")),
                Map.of("transactionType","CONSUME","quantity",new BigDecimal("-3"),"totalCost",new BigDecimal("12")),
                Map.of("transactionType","WASTE","quantity",new BigDecimal("-1"),"totalCost",new BigDecimal("4")),
                Map.of("transactionType","ADJUSTMENT","quantity",new BigDecimal("-2"),"totalCost",new BigDecimal("8"),"stockCountId",1),
                Map.of("transactionType","ADJUSTMENT","quantity",new BigDecimal("1"),"totalCost",new BigDecimal("4"),"stockCountId",2));
        Map<String,BigDecimal> result=InventoryReportService.summarize(rows,new BigDecimal("100"));
        assertEquals(new BigDecimal("20.0000"),result.get("purchaseCost"));
        assertEquals(new BigDecimal("12.0000"),result.get("consumptionCost"));
        assertEquals(new BigDecimal("4.0000"),result.get("wasteCost"));
        assertEquals(new BigDecimal("8.0000"),result.get("stockCountLossCost"));
        assertEquals(new BigDecimal("4.0000"),result.get("stockCountGainCost"));
        assertEquals(new BigDecimal("12.0000"),result.get("totalLossCost"));
        assertEquals(new BigDecimal("100.0000"),result.get("lossRate"));
    }

    @Test void endingValueReplaysHistoricalBalancesAndReceiptCostsAtRequestedDate() {
        List<Map<String,Object>> rows=List.of(
                Map.of("inventoryItemId",1,"transactionType","RECEIPT","quantity",new BigDecimal("10"),"quantityBefore",BigDecimal.ZERO,"quantityAfter",new BigDecimal("10"),"unitCostSnapshot",new BigDecimal("2")),
                Map.of("inventoryItemId",1,"transactionType","CONSUME","quantity",new BigDecimal("-3"),"quantityBefore",new BigDecimal("10"),"quantityAfter",new BigDecimal("7"),"unitCostSnapshot",new BigDecimal("2")),
                Map.of("inventoryItemId",2,"transactionType","WASTE","quantity",new BigDecimal("-1"),"quantityBefore",new BigDecimal("5"),"quantityAfter",new BigDecimal("4"),"unitCostSnapshot",new BigDecimal("3")));
        assertEquals(new BigDecimal("26.0000"),InventoryReportService.endingValue(rows));
    }

    @Test void endingValueReplaysOpeningAdjustmentBeforeReceipts() {
        List<Map<String,Object>> rows=List.of(
                Map.of("inventoryItemId",1,"transactionType","ADJUSTMENT","quantity",new BigDecimal("5"),"quantityBefore",BigDecimal.ZERO,"quantityAfter",new BigDecimal("5"),"unitCostSnapshot",BigDecimal.ZERO),
                Map.of("inventoryItemId",1,"transactionType","RECEIPT","quantity",new BigDecimal("2"),"quantityBefore",new BigDecimal("5"),"quantityAfter",new BigDecimal("7"),"unitCostSnapshot",new BigDecimal("2")));
        assertEquals(new BigDecimal("3.9998"),InventoryReportService.endingValue(rows));
    }

    @Test void endingValueIgnoresReservedBalanceTransactions() {
        List<Map<String,Object>> rows=List.of(
                Map.of("inventoryItemId",1,"transactionType","RECEIPT","quantity",new BigDecimal("10"),"quantityAfter",new BigDecimal("10"),"unitCostSnapshot",new BigDecimal("2")),
                Map.of("inventoryItemId",1,"transactionType","RESERVE","quantity",new BigDecimal("-4"),"quantityAfter",new BigDecimal("4"),"unitCostSnapshot",new BigDecimal("2")),
                Map.of("inventoryItemId",1,"transactionType","RELEASE","quantity",new BigDecimal("2"),"quantityAfter",new BigDecimal("2"),"unitCostSnapshot",new BigDecimal("2")));
        assertEquals(new BigDecimal("20.0000"),InventoryReportService.endingValue(rows));
    }

    @Test void endingValueRetainsPriorBalanceWhenQuantityAfterIsNull() {
        Map<String,Object> row=new java.util.LinkedHashMap<>();
        row.put("inventoryItemId",1);row.put("transactionType","CONSUME");row.put("quantity",new BigDecimal("-2"));row.put("quantityAfter",null);row.put("unitCostSnapshot",new BigDecimal("3"));
        List<Map<String,Object>> rows=List.of(
                Map.of("inventoryItemId",1,"transactionType","RECEIPT","quantity",new BigDecimal("5"),"quantityAfter",new BigDecimal("5"),"unitCostSnapshot",new BigDecimal("3")),row);
        assertEquals(new BigDecimal("15.0000"),InventoryReportService.endingValue(rows));
    }

    @Test void itemLossSeparatesWasteFromNegativeStockCountAdjustment() {
        List<Map<String,Object>> rows=List.of(
                Map.of("inventoryItemId",1,"inventoryCode","TOMATO","name","Tomato","transactionType","WASTE","quantity",new BigDecimal("-2"),"totalCost",new BigDecimal("6")),
                Map.of("inventoryItemId",1,"inventoryCode","TOMATO","name","Tomato","transactionType","ADJUSTMENT","quantity",new BigDecimal("-1"),"totalCost",new BigDecimal("3"),"stockCountId",4),
                Map.of("inventoryItemId",1,"inventoryCode","TOMATO","name","Tomato","transactionType","ADJUSTMENT","quantity",new BigDecimal("2"),"totalCost",new BigDecimal("6"),"stockCountId",5));
        Map<String,Object> result=InventoryReportService.itemLossRows(rows).get(0);
        assertEquals(new BigDecimal("2.0000"),result.get("wasteQuantity"));
        assertEquals(new BigDecimal("1.0000"),result.get("stockCountLossQuantity"));
        assertEquals(new BigDecimal("9.0000"),result.get("totalLossCost"));
    }

    @Test void itemLossTreatsNullTotalCostAsZero() {
        Map<String,Object> row=new java.util.LinkedHashMap<>();
        row.put("inventoryItemId",1);row.put("inventoryCode","TOMATO");row.put("name","Tomato");
        row.put("transactionType","WASTE");row.put("quantity",new BigDecimal("-2"));row.put("totalCost",null);
        Map<String,Object> result=InventoryReportService.itemLossRows(List.of(row)).get(0);
        assertEquals(new BigDecimal("2.0000"),result.get("wasteQuantity"));
        assertEquals(new BigDecimal("0.0000"),result.get("totalLossCost"));
    }

    @Test void analyticsComparisonPeriodUsesSameInclusiveDayCount() {
        assertArrayEquals(new LocalDate[]{LocalDate.of(2026,8,2),LocalDate.of(2026,8,31)},InventoryReportService.comparisonPeriod(LocalDate.of(2026,9,1),LocalDate.of(2026,9,30)));
        assertThrows(IllegalArgumentException.class,()->InventoryReportService.comparisonPeriod(LocalDate.of(2025,1,1),LocalDate.of(2026,2,1)));
    }

    @Test void analyticsClassifiesInventoryHealthByAvailableToMinimumRatio() {
        assertEquals("OUT",InventoryReportService.healthState(new BigDecimal("0"),new BigDecimal("10")));
        assertEquals("LOW",InventoryReportService.healthState(new BigDecimal("9.9"),new BigDecimal("10")));
        assertEquals("ATTENTION",InventoryReportService.healthState(new BigDecimal("12.5"),new BigDecimal("10")));
        assertEquals("HEALTHY",InventoryReportService.healthState(new BigDecimal("20"),new BigDecimal("10")));
        assertEquals("EXCESS",InventoryReportService.healthState(new BigDecimal("20.1"),new BigDecimal("10")));
        assertEquals("HEALTHY",InventoryReportService.healthState(new BigDecimal("5"),BigDecimal.ZERO));
    }

    @Test void menuCostDividesCurrentIngredientCostByRecipeYield() {
        Map<String,Object> result=InventoryReportService.menuCostRow(new Object[]{7,"Large","SKU-7",new BigDecimal("2"),new BigDecimal("12")});
        assertEquals(new BigDecimal("12.0000"),result.get("recipeCost"));
        assertEquals(new BigDecimal("6.0000"),result.get("costPerServing"));
    }
}
