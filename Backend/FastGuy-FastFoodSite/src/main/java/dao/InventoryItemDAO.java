package dao;

import java.util.List;

import entity.InventoryItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

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
}
