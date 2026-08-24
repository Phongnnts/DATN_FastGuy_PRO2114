package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import service.GoodsReceiptService;
import service.InventoryReportService;
import service.StockCountService;
import utils.DatabaseUtil;

class InventoryCostingIT {
    private int adminId;
    private int itemId;
    private BigDecimal originalQuantity;
    private BigDecimal originalCost;
    private Integer receiptId;
    private Integer stockCountId;

    @BeforeEach
    void captureDisposableState() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            adminId = ((Number) em.createNativeQuery("SELECT TOP 1 user_id FROM Users WHERE role_name='ADMIN' ORDER BY user_id").getSingleResult()).intValue();
            Object[] item = (Object[]) em.createNativeQuery("SELECT TOP 1 inventory_item_id,on_hand_quantity,average_unit_cost FROM InventoryItem WHERE active=1 ORDER BY inventory_item_id").getSingleResult();
            itemId = ((Number) item[0]).intValue();
            originalQuantity = (BigDecimal) item[1];
            originalCost = (BigDecimal) item[2];
        } finally {
            em.close();
        }
    }

    @AfterEach
    void restoreDisposableState() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (stockCountId != null) {
                em.createNativeQuery("DELETE FROM InventoryTransaction WHERE stock_count_id=:id").setParameter("id", stockCountId).executeUpdate();
                em.createNativeQuery("DELETE FROM StockCountItem WHERE stock_count_id=:id").setParameter("id", stockCountId).executeUpdate();
                em.createNativeQuery("DELETE FROM StockCount WHERE stock_count_id=:id").setParameter("id", stockCountId).executeUpdate();
            }
            if (receiptId != null) {
                em.createNativeQuery("DELETE FROM InventoryTransaction WHERE goods_receipt_id=:id").setParameter("id", receiptId).executeUpdate();
                em.createNativeQuery("DELETE FROM GoodsReceiptItem WHERE goods_receipt_id=:id").setParameter("id", receiptId).executeUpdate();
                em.createNativeQuery("DELETE FROM GoodsReceipt WHERE goods_receipt_id=:id").setParameter("id", receiptId).executeUpdate();
            }
            em.createNativeQuery("UPDATE InventoryItem SET on_hand_quantity=:quantity,average_unit_cost=:cost WHERE inventory_item_id=:id")
                    .setParameter("quantity", originalQuantity).setParameter("cost", originalCost).setParameter("id", itemId).executeUpdate();
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    @Test
    void disposableDatabaseRunsReceiptCountAndReportWorkflows() {
        GoodsReceiptService receipts = new GoodsReceiptService();
        Map<String, Object> draft = receipts.create(Map.of(
                "supplierName", "Integration Test Supplier",
                "invoiceNumber", "IT-053",
                "receivedAt", LocalDateTime.now().withNano(0).toString(),
                "items", List.of(Map.of(
                        "inventoryItemId", itemId,
                        "purchaseQuantity", "2.0000",
                        "purchaseUnit", "test-pack",
                        "conversionFactor", "1.0000",
                        "purchaseUnitPrice", "3.0000"))), adminId);
        receiptId = ((Number) draft.get("goodsReceiptId")).intValue();
        assertEquals("DRAFT", draft.get("status"));

        Map<String, Object> approved = receipts.approve(receiptId, adminId);
        assertEquals("APPROVED", approved.get("status"));
        assertNotNull(approved.get("approvedAt"));

        StockCountService counts = new StockCountService();
        Map<String, Object> count = counts.create(LocalDate.now(), "WEEKLY", adminId);
        stockCountId = ((Number) count.get("stockCountId")).intValue();
        assertEquals("DRAFT", count.get("status"));
        assertFalse(counts.list().isEmpty());
        assertEquals(stockCountId, counts.get(stockCountId).get("stockCountId"));

        InventoryReportService reports = new InventoryReportService();
        Map<String, BigDecimal> summary = reports.summary(LocalDate.now(), LocalDate.now());
        assertTrue(summary.get("purchaseCost").compareTo(new BigDecimal("6.0000")) >= 0);
        assertNotNull(reports.itemLoss(LocalDate.now(), LocalDate.now()));
        assertFalse(reports.menuCost().isEmpty());
    }
}
