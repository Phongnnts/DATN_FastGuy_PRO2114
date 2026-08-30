package dao;

import java.util.List;
import java.util.Map;

import entity.InventoryItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

public class InventoryItemDAO {
    public List<InventoryItem> findByIds(EntityManager em, List<Integer> ids) {
        if (ids.isEmpty()) return List.of();
        return em.createQuery("SELECT i FROM InventoryItem i WHERE i.inventoryItemId IN :ids ORDER BY i.inventoryItemId", InventoryItem.class)
                .setParameter("ids", ids).getResultList();
    }

    public InventoryItem lock(EntityManager em, int id) {
        return em.find(InventoryItem.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    public InventoryItem findFinishedGood(EntityManager em, int variantId) {
        List<InventoryItem> items = em.createQuery("SELECT m.inventoryItem FROM VariantInventoryItem m WHERE m.variant.variantId = :variantId", InventoryItem.class)
                .setParameter("variantId", variantId).setMaxResults(1).getResultList();
        return items.isEmpty() ? null : items.get(0);
    }

    public Map<String, Long> inventoryRiskCounts() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return inventoryRiskCounts(em.createQuery("SELECT i FROM InventoryItem i WHERE i.active = true", InventoryItem.class).getResultList());
        } finally {
            em.close();
        }
    }

    static Map<String, Long> inventoryRiskCounts(List<InventoryItem> items) {
        long outOfStock = items.stream()
                .filter(InventoryItem::isActive)
                .filter(item -> item.availableQuantity().signum() <= 0)
                .count();
        long lowStock = items.stream()
                .filter(InventoryItem::isActive)
                .filter(item -> item.availableQuantity().signum() > 0 && item.availableQuantity().compareTo(item.getMinimumQuantity()) <= 0)
                .count();
        return Map.of("outOfStock", outOfStock, "lowStock", lowStock, "lowStockItemCount", outOfStock + lowStock);
    }
}
