package service;

import java.util.HashMap;
import java.util.Map;

import entity.InventoryTransaction;
import entity.ProductVariant;
import entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

public class InventoryAdjustmentService {

    public Map<String, Object> adjust(int variantId, int newQuantity, String reasonCode, String note, int adminId) {
        if (newQuantity < 0) throw new IllegalArgumentException("Số lượng tồn kho mới không hợp lệ");
        if (reasonCode == null || reasonCode.isBlank()) throw new IllegalArgumentException("Vui lòng chọn lý do điều chỉnh");
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            ProductVariant variant = em.find(ProductVariant.class, variantId, LockModeType.PESSIMISTIC_WRITE);
            if (variant == null) throw new IllegalArgumentException("Biến thể không tồn tại");
            Integer stock = variant.getQuantityAvailable();
            if (stock == null) throw new IllegalArgumentException("Biến thể không quản lý tồn kho");
            int before = stock;
            int after = newQuantity;
            variant.setQuantityAvailable(after);
            InventoryTransaction txn = new InventoryTransaction();
            txn.setVariant(variant);
            txn.setOrder(null);
            txn.setCreatedBy(em.find(User.class, adminId));
            txn.setTransactionType("ADJUSTMENT");
            txn.setQuantity(Math.abs(after - before));
            txn.setReasonCode(reasonCode);
            txn.setNote(note);
            txn.setQuantityBefore(before);
            txn.setQuantityAfter(after);
            em.persist(txn);
            em.getTransaction().commit();
            Map<String, Object> result = new HashMap<>();
            result.put("variantId", variantId);
            result.put("before", before);
            result.put("after", after);
            return result;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public Map<String, Object> waste(int variantId, int quantity, String reasonCode, String note, int adminId) {
        if (quantity <= 0) throw new IllegalArgumentException("Số lượng lãng phí phải lớn hơn 0");
        if (reasonCode == null || reasonCode.isBlank()) throw new IllegalArgumentException("Vui lòng chọn lý do lãng phí");
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            ProductVariant variant = em.find(ProductVariant.class, variantId, LockModeType.PESSIMISTIC_WRITE);
            if (variant == null) throw new IllegalArgumentException("Biến thể không tồn tại");
            Integer stock = variant.getQuantityAvailable();
            if (stock == null) throw new IllegalArgumentException("Biến thể không quản lý tồn kho");
            if (quantity > stock) throw new IllegalArgumentException("Số lượng lãng phí vượt quá tồn kho hiện tại");
            int before = stock;
            int after = stock - quantity;
            variant.setQuantityAvailable(after);
            InventoryTransaction txn = new InventoryTransaction();
            txn.setVariant(variant);
            txn.setOrder(null);
            txn.setCreatedBy(em.find(User.class, adminId));
            txn.setTransactionType("WASTE");
            txn.setQuantity(quantity);
            txn.setReasonCode(reasonCode);
            txn.setNote(note);
            txn.setQuantityBefore(before);
            txn.setQuantityAfter(after);
            em.persist(txn);
            em.getTransaction().commit();
            Map<String, Object> result = new HashMap<>();
            result.put("variantId", variantId);
            result.put("wasted", quantity);
            result.put("before", before);
            result.put("after", after);
            return result;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
