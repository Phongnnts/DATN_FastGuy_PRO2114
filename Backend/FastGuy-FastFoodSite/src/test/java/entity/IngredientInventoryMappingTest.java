package entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.Check;

class IngredientInventoryMappingTest {
    @Test
    void mapsDecimalInventorySchemaAndEnforcesBalances() throws Exception {
        assertTable(InventoryItem.class, "InventoryItem");
        assertColumn(InventoryItem.class, "onHandQuantity", "on_hand_quantity", 19, 4);
        assertColumn(InventoryItem.class, "reservedQuantity", "reserved_quantity", 19, 4);
        assertColumn(InventoryItem.class, "minimumQuantity", "minimum_quantity", 19, 4);
        assertColumn(InventoryItem.class, "averageUnitCost", "average_unit_cost", 19, 4);
        assertNullableColumn(InventoryItem.class, "inventoryCode", "inventory_code", false);
        assertNullableColumn(InventoryItem.class, "countFrequency", "count_frequency", false);
        assertNullableColumn(InventoryItem.class, "lastCountedAt", "last_counted_at", true);

        InventoryItem item = new InventoryItem();
        item.setOnHandQuantity(new BigDecimal("10.0000"));
        item.setReservedQuantity(new BigDecimal("6.2000"));

        assertEquals(new BigDecimal("3.8000"), item.availableQuantity());
        assertThrows(IllegalStateException.class, () -> item.reserve(new BigDecimal("6.0000")));
        item.reserve(new BigDecimal("3.8000"));
        assertEquals(new BigDecimal("10.0000"), item.getReservedQuantity());
        item.release(new BigDecimal("1.2500"));
        assertEquals(new BigDecimal("8.7500"), item.getReservedQuantity());
    }

    @Test
    void mapsRecipeAndVariantLinksWithSchemaUniqueness() throws Exception {
        assertUnique(VariantInventoryItem.class, List.of("variant_id"), List.of("inventory_item_id"));
        assertUnique(Recipe.class, List.of("variant_id"));
        assertUnique(RecipeItem.class, List.of("recipe_id", "inventory_item_id"));
        assertColumn(Recipe.class, "yieldQuantity", "yield_quantity", 19, 4);
        assertColumn(RecipeItem.class, "quantity", "quantity", 19, 4);

        ProductVariant variant = new ProductVariant();
        variant.setInventoryMode("INGREDIENT");
        assertEquals("INGREDIENT", variant.getInventoryMode());
    }

    @Test
    void inventoryItemAndRecipeInitializeAndUpdateSecondPrecisionTimestamps() throws Exception {
        assertTimestamps(InventoryItem.class, new InventoryItem());
        assertTimestamps(Recipe.class, new Recipe());
    }

    @Test
    void inventoryTransactionCreatedAtIsRequiredAndInitializedAtSecondPrecision() throws Exception {
        assertNullableColumn(InventoryTransaction.class, "createdAt", "created_at", false);
        InventoryTransaction transaction = new InventoryTransaction();
        Method persist = java.util.Arrays.stream(InventoryTransaction.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PrePersist.class)).findFirst().orElseThrow();
        persist.setAccessible(true);
        persist.invoke(transaction);
        assertNotNull(transaction.getCreatedAt());
        assertEquals(0, transaction.getCreatedAt().getNano());
    }

    @Test
    void mapsReservationAggregateAndDecimalTransactions() throws Exception {
        assertUnique(InventoryReservation.class, List.of("order_id"));
        assertUnique(InventoryReservationItem.class, List.of("reservation_id", "inventory_item_id"));
        assertColumn(InventoryReservationItem.class, "quantity", "quantity", 19, 4);
        assertJoin(InventoryTransaction.class, "inventoryItem", "inventory_item_id", false);
        assertJoin(InventoryTransaction.class, "order", "order_id", true);
        assertJoin(InventoryTransaction.class, "createdBy", "created_by", true);
        assertNullableColumn(InventoryTransaction.class, "referenceType", "reference_type", true);
        assertNullableColumn(InventoryTransaction.class, "referenceId", "reference_id", true);
        assertColumn(InventoryTransaction.class, "quantity", "quantity", 19, 4);
        assertColumn(InventoryTransaction.class, "quantityBefore", "quantity_before", 19, 4);
        assertColumn(InventoryTransaction.class, "quantityAfter", "quantity_after", 19, 4);
        assertColumn(InventoryTransaction.class, "unitCostSnapshot", "unit_cost_snapshot", 19, 4);
        assertColumn(InventoryTransaction.class, "totalCost", "total_cost", 19, 4);
        assertJoin(InventoryTransaction.class, "goodsReceipt", "goods_receipt_id", true);
        assertJoin(InventoryTransaction.class, "stockCount", "stock_count_id", true);
        assertThrows(NoSuchFieldException.class, () -> InventoryTransaction.class.getDeclaredField("variant"));
        assertThrows(NoSuchFieldException.class, () -> InventoryReservation.class.getDeclaredField("variant"));
        assertThrows(NoSuchFieldException.class, () -> InventoryReservation.class.getDeclaredField("quantity"));
        assertCheckContains(InventoryReservation.class, "RESERVED", "CONSUMED", "RELEASED", "WASTED");
        assertCheckContains(InventoryTransaction.class, "quantity <> 0", "RESERVE", "RELEASE", "CONSUME", "ADJUSTMENT", "WASTE");

        InventoryTransaction transaction = new InventoryTransaction();
        assertThrows(IllegalStateException.class, () -> transaction.setQuantity(BigDecimal.ZERO));
        transaction.setQuantity(new BigDecimal("-1.2500"));
        assertEquals(new BigDecimal("-1.2500"), transaction.getQuantity());

        InventoryReservation reservation = new InventoryReservation();
        InventoryReservationItem reservationItem = new InventoryReservationItem();
        reservation.setItems(List.of(reservationItem));
        assertEquals(List.of(reservationItem), reservation.getItems());
    }

    @Test
    void mapsReceiptAndStockCountDocumentsToMigration053() throws Exception {
        assertTable(GoodsReceipt.class, "GoodsReceipt");
        assertTable(GoodsReceiptItem.class, "GoodsReceiptItem");
        assertUnique(GoodsReceiptItem.class, List.of("goods_receipt_id", "inventory_item_id"));
        assertColumn(GoodsReceiptItem.class, "purchaseQuantity", "purchase_quantity", 19, 4);
        assertColumn(GoodsReceiptItem.class, "conversionFactor", "conversion_factor", 19, 4);
        assertColumn(GoodsReceiptItem.class, "baseQuantity", "base_quantity", 19, 4);
        assertColumn(GoodsReceiptItem.class, "purchaseUnitPrice", "purchase_unit_price", 19, 4);
        assertColumn(GoodsReceiptItem.class, "lineTotal", "line_total", 19, 4);
        assertColumn(GoodsReceiptItem.class, "averageCostBefore", "average_cost_before", 19, 4);
        assertColumn(GoodsReceiptItem.class, "averageCostAfter", "average_cost_after", 19, 4);

        assertTable(StockCount.class, "StockCount");
        assertTable(StockCountItem.class, "StockCountItem");
        assertUnique(StockCountItem.class, List.of("stock_count_id", "inventory_item_id"));
        assertColumn(StockCountItem.class, "theoreticalQuantity", "theoretical_quantity", 19, 4);
        assertColumn(StockCountItem.class, "actualQuantity", "actual_quantity", 19, 4);
        assertColumn(StockCountItem.class, "varianceQuantity", "variance_quantity", 19, 4);
        assertColumn(StockCountItem.class, "unitCostSnapshot", "unit_cost_snapshot", 19, 4);
        assertColumn(StockCountItem.class, "varianceCost", "variance_cost", 19, 4);
    }

    private static void assertTable(Class<?> type, String name) {
        assertEquals(name, type.getAnnotation(Table.class).name());
    }

    private static void assertColumn(Class<?> type, String fieldName, String name, int precision, int scale) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        assertEquals(BigDecimal.class, field.getType());
        Column column = field.getAnnotation(Column.class);
        assertEquals(name, column.name());
        assertEquals(precision, column.precision());
        assertEquals(scale, column.scale());
    }

    private static void assertJoin(Class<?> type, String fieldName, String name, boolean nullable) throws Exception {
        JoinColumn join = type.getDeclaredField(fieldName).getAnnotation(JoinColumn.class);
        assertEquals(name, join.name());
        assertEquals(nullable, join.nullable());
    }

    private static void assertNullableColumn(Class<?> type, String fieldName, String name, boolean nullable) throws Exception {
        Column column = type.getDeclaredField(fieldName).getAnnotation(Column.class);
        assertEquals(name, column.name());
        assertEquals(nullable, column.nullable());
    }

    private static void assertCheckContains(Class<?> type, String... tokens) {
        Check check = type.getAnnotation(Check.class);
        assertNotNull(check);
        for (String token : tokens) assertTrue(check.constraints().contains(token));
    }

    private static void assertTimestamps(Class<?> type, Object entity) throws Exception {
        Method persist = java.util.Arrays.stream(type.getDeclaredMethods()).filter(method -> method.isAnnotationPresent(PrePersist.class)).findFirst().orElseThrow();
        Method update = java.util.Arrays.stream(type.getDeclaredMethods()).filter(method -> method.isAnnotationPresent(PreUpdate.class)).findFirst().orElseThrow();
        Field created = type.getDeclaredField("createdAt");
        Field updated = type.getDeclaredField("updatedAt");
        persist.setAccessible(true);
        update.setAccessible(true);
        created.setAccessible(true);
        updated.setAccessible(true);
        persist.invoke(entity);
        LocalDateTime createdAt = (LocalDateTime) created.get(entity);
        LocalDateTime firstUpdatedAt = (LocalDateTime) updated.get(entity);
        assertNotNull(createdAt);
        assertEquals(createdAt, firstUpdatedAt);
        assertEquals(0, createdAt.getNano());
        updated.set(entity, firstUpdatedAt.minusSeconds(1));
        update.invoke(entity);
        assertEquals(createdAt, created.get(entity));
        LocalDateTime secondUpdatedAt = (LocalDateTime) updated.get(entity);
        assertTrue(secondUpdatedAt.isAfter(firstUpdatedAt.minusSeconds(1)));
        assertEquals(0, secondUpdatedAt.getNano());
    }

    @SafeVarargs
    private static void assertUnique(Class<?> type, List<String>... expected) {
        Table table = type.getAnnotation(Table.class);
        List<List<String>> actual = java.util.Arrays.stream(table.uniqueConstraints())
                .map(constraint -> List.of(constraint.columnNames()))
                .toList();
        for (List<String> columns : expected) {
            assertEquals(true, actual.contains(columns));
        }
    }
}
