package servlet;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class IngredientInventoryApiContractTest {
    @Test void legacyReceiptEndpointIsRemovedAndAdjustmentQuantityIsNonzero() throws Exception {
        String contract=Files.readString(Path.of("../../openapi/fastguy.yaml"));
        String adjustment=contract.substring(contract.indexOf("    InventoryAdjustmentRequest:"),contract.indexOf("    InventoryTransaction:"));
        assertFalse(contract.contains("  /admin/inventory/transactions/receipts:"));
        assertFalse(contract.contains("    InventoryReceiptRequest:"));
        assertFalse(Files.exists(Path.of("src/main/java/servlet/AdminInventoryReceiptServlet.java")));
        assertTrue(adjustment.contains("maximum: -0.0001"));
        assertTrue(adjustment.contains("minimum: 0.0001"));
    }

    @Test void costingReceiptCountAndReportContractsExist() throws Exception {
        String contract=Files.readString(Path.of("../../openapi/fastguy.yaml"));
        assertTrue(contract.contains("  /admin/inventory/receipts:"));
        assertTrue(contract.contains("  /admin/inventory/receipts/{receiptId}/approve:"));
        assertTrue(contract.contains("  /admin/inventory/stock-counts:"));
        assertTrue(contract.contains("  /admin/inventory/stock-counts/{stockCountId}/approve:"));
        assertTrue(contract.contains("  /admin/inventory/reports/summary:"));
        assertTrue(contract.contains("  /admin/inventory/reports/item-loss:"));
        assertTrue(contract.contains("  /admin/inventory/reports/menu-cost:"));
        String item=contract.substring(contract.indexOf("    InventoryItem:"),contract.indexOf("    InventoryItemResponse:"));
        assertTrue(item.contains("inventoryCode:"));
        assertTrue(item.contains("countFrequency:"));
        assertTrue(item.contains("averageUnitCost:"));
        String transaction=contract.substring(contract.indexOf("    InventoryTransaction:"),contract.indexOf("    InventoryTransactionPage:"));
        assertTrue(transaction.contains("unitCostSnapshot:"));
        assertTrue(transaction.contains("totalCost:"));
    }

    @Test void receiptAndStockCountUseClosedExactLineSchemas() throws Exception {
        String contract=Files.readString(Path.of("../../openapi/fastguy.yaml"));
        String receipt=contract.substring(contract.indexOf("    GoodsReceipt:"),contract.indexOf("    GoodsReceiptResponse:"));
        String count=contract.substring(contract.indexOf("    StockCount:"),contract.indexOf("    StockCountResponse:"));
        assertTrue(receipt.contains("additionalProperties: false"));
        assertTrue(receipt.contains("$ref: '#/components/schemas/GoodsReceiptLine'"));
        assertTrue(count.contains("additionalProperties: false"));
        assertTrue(count.contains("$ref: '#/components/schemas/StockCountLine'"));
        assertTrue(count.contains("approvedAt:"));
    }

    @Test void transactionBalancesAreNullableAndReceiptNumbersArePositive() throws Exception {
        String contract=Files.readString(Path.of("../../openapi/fastguy.yaml"));
        String transaction=contract.substring(contract.indexOf("    InventoryTransaction:"),contract.indexOf("    InventoryTransactionPage:"));
        assertTrue(transaction.contains("quantityBefore: { oneOf: [{ $ref: '#/components/schemas/InventoryItemQuantity' }, { type: 'null' }] }"));
        assertTrue(transaction.contains("quantityAfter: { oneOf: [{ $ref: '#/components/schemas/InventoryItemQuantity' }, { type: 'null' }] }"));
        String receiptItem=contract.substring(contract.indexOf("    GoodsReceiptItemRequest:"),contract.indexOf("    GoodsReceiptRequest:"));
        assertEquals(3,receiptItem.split("exclusiveMinimum: 0",-1).length-1);
    }
}
