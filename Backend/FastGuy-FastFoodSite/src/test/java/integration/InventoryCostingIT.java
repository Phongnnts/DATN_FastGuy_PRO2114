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
import service.CartService;
import service.InventoryReportService;
import service.MenuPerformanceReportService;
import service.OrderItemCostService;
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
            Object[] item = (Object[]) em.createNativeQuery("SELECT TOP 1 inventory_item_id,on_hand_quantity,average_unit_cost FROM InventoryItem WHERE active=1 AND item_type='INGREDIENT' ORDER BY inventory_item_id").getSingleResult();
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
        Map<String, Object> count = counts.create(LocalDate.now(), List.of(itemId), adminId);
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

    @Test
    void disposableDatabaseSnapshotsOrderItemCostAndReportsLegacyGaps() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            String database = (String) em.createNativeQuery("SELECT DB_NAME()").getSingleResult();
            assertTrue(database.endsWith("_Test"));
            em.getTransaction().begin();
            Object[] selected = (Object[]) em.createNativeQuery("SELECT TOP 1 oi.order_id,oi.order_item_id FROM OrderItem oi JOIN ProductVariant v ON v.variant_id=oi.variant_id JOIN Recipe r ON r.variant_id=v.variant_id AND r.active=1 JOIN RecipeItem ri ON ri.recipe_id=r.recipe_id WHERE v.inventory_mode='INGREDIENT' ORDER BY oi.order_item_id").getSingleResult();
            int orderId = ((Number) selected[0]).intValue();
            int orderItemId = ((Number) selected[1]).intValue();
            em.createNativeQuery("UPDATE i SET average_unit_cost=2 FROM InventoryItem i JOIN RecipeItem ri ON ri.inventory_item_id=i.inventory_item_id JOIN Recipe r ON r.recipe_id=ri.recipe_id JOIN OrderItem oi ON oi.variant_id=r.variant_id WHERE oi.order_id=:orderId")
                    .setParameter("orderId", orderId).executeUpdate();
            em.createNativeQuery("UPDATE OrderItem SET unit_cost_snapshot=NULL,total_cost_snapshot=NULL WHERE order_id=:orderId")
                    .setParameter("orderId", orderId).executeUpdate();
            em.clear();
            new OrderItemCostService().snapshot(em, em.find(entity.Orders.class, orderId));
            em.flush();
            Object[] snapshot = (Object[]) em.createNativeQuery("SELECT unit_cost_snapshot,total_cost_snapshot FROM OrderItem WHERE order_item_id=:id")
                    .setParameter("id", orderItemId).getSingleResult();
            assertNotNull(snapshot[0]);
            assertNotNull(snapshot[1]);
        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.close();
        }

        Map<String,Object> report = new MenuPerformanceReportService().report(LocalDate.of(2020,1,1), LocalDate.of(2030,12,31));
        assertFalse((Boolean) report.get("costComplete"));
        assertTrue(((Number) report.get("missingCostItemCount")).intValue() > 0);
        assertTrue(((BigDecimal) report.get("netRevenue")).signum() > 0);
    }

    @Test
    void recipeCapacityOverridesLegacyVariantQuantityInCart() {
        EntityManager em=DatabaseUtil.getEntityManager();Integer originalItemId=null,originalQuantity=null,newItemId=null;int userId,cartId,productId,variantId;
        try{
            String database=(String)em.createNativeQuery("SELECT DB_NAME()").getSingleResult();assertTrue(database.endsWith("_Test"));
            userId=((Number)em.createNativeQuery("SELECT TOP 1 user_id FROM Users WHERE role_name='ADMIN' ORDER BY user_id").getSingleResult()).intValue();
            cartId=((Number)em.createNativeQuery("SELECT cart_id FROM Cart WHERE user_id=:userId").setParameter("userId",userId).getSingleResult()).intValue();
            Object[]variant=(Object[])em.createNativeQuery("SELECT TOP 1 v.product_id,v.variant_id FROM ProductVariant v JOIN Recipe r ON r.variant_id=v.variant_id AND r.active=1 WHERE v.inventory_mode='INGREDIENT' AND v.quantity_available IS NOT NULL AND NOT EXISTS(SELECT 1 FROM ProductModifierGroup g WHERE g.product_id=v.product_id AND g.is_active=1 AND g.min_selections>0) ORDER BY v.quantity_available,v.variant_id").getSingleResult();productId=((Number)variant[0]).intValue();variantId=((Number)variant[1]).intValue();
            List<?>before=em.createNativeQuery("SELECT cart_item_id,quantity FROM CartItem WHERE cart_id=:cartId AND variant_id=:variantId AND (modifiers_json IS NULL OR modifiers_json=N'[]') ORDER BY cart_item_id").setParameter("cartId",cartId).setParameter("variantId",variantId).setMaxResults(1).getResultList();
            if(!before.isEmpty()){Object[]row=(Object[])before.get(0);originalItemId=((Number)row[0]).intValue();originalQuantity=((Number)row[1]).intValue();}
            entity.User user=new entity.User();user.setUserId(userId);assertTrue(new CartService().addItem(user,productId,variantId,4,List.of()));
            Object[]after=(Object[])em.createNativeQuery("SELECT TOP 1 cart_item_id,quantity FROM CartItem WHERE cart_id=:cartId AND variant_id=:variantId AND (modifiers_json IS NULL OR modifiers_json=N'[]') ORDER BY cart_item_id").setParameter("cartId",cartId).setParameter("variantId",variantId).getSingleResult();newItemId=((Number)after[0]).intValue();int quantity=((Number)after[1]).intValue(),testItemId=newItemId;assertTrue(quantity>=4);assertTrue(new CartService().updateItemQuantity(testItemId,userId,20));org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,()->new CartService().updateItemQuantity(testItemId,userId,21));
        }finally{
            try{em.getTransaction().begin();if(newItemId!=null){if(originalItemId!=null&&newItemId.equals(originalItemId))em.createNativeQuery("UPDATE CartItem SET quantity=:quantity WHERE cart_item_id=:id").setParameter("quantity",originalQuantity).setParameter("id",originalItemId).executeUpdate();else em.createNativeQuery("DELETE FROM CartItem WHERE cart_item_id=:id").setParameter("id",newItemId).executeUpdate();}em.getTransaction().commit();}catch(RuntimeException e){if(em.getTransaction().isActive())em.getTransaction().rollback();throw e;}finally{em.close();}
        }
    }
}
