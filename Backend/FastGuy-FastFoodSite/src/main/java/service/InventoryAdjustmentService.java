package service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import entity.InventoryTransaction;
import entity.ProductVariant;
import entity.User;
import exception.InventoryConflictException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

public class InventoryAdjustmentService {
    private static final Set<String> ADJUSTMENT_REASONS = Set.of("STOCK_COUNT", "DAMAGE", "EXPIRED", "OTHER");
    private final Supplier<EntityManager> entityManagers;

    public InventoryAdjustmentService() {
        this(DatabaseUtil::getEntityManager);
    }

    InventoryAdjustmentService(Supplier<EntityManager> entityManagers) {
        this.entityManagers = entityManagers;
    }

    public Map<String, Object> adjust(int variantId, String operation, int quantity, Integer expectedQuantity,
            String reasonCode, String note, int adminId) {
        if (!ADJUSTMENT_REASONS.contains(reasonCode)) throw new IllegalArgumentException("Vui lòng chọn lý do điều chỉnh hợp lệ");
        if ("OTHER".equals(reasonCode) && (note == null || note.isBlank())) throw new IllegalArgumentException("Ghi chú là bắt buộc khi chọn lý do Khác");
        if (note != null && note.length() > 500) throw new IllegalArgumentException("Ghi chú không được vượt quá 500 ký tự");
        if (!Set.of("INCREASE", "DECREASE", "SET").contains(operation)) throw new IllegalArgumentException("Thao tác điều chỉnh không hợp lệ");
        if (("INCREASE".equals(operation) || "DECREASE".equals(operation)) && quantity <= 0) throw new IllegalArgumentException("Số lượng điều chỉnh phải lớn hơn 0");
        if ("SET".equals(operation) && quantity < 0) throw new IllegalArgumentException("Số lượng tồn kho mới không hợp lệ");
        EntityManager em = entityManagers.get();
        try {
            em.getTransaction().begin();
            ProductVariant variant = em.find(ProductVariant.class, variantId, LockModeType.PESSIMISTIC_WRITE);
            if (variant == null) throw new IllegalArgumentException("Biến thể không tồn tại");
            Integer stock = variant.getQuantityAvailable();
            if (stock == null) throw new IllegalArgumentException("Biến thể không quản lý tồn kho");
            if (!Objects.equals(stock, expectedQuantity)) throw new InventoryConflictException(variantId, stock);
            int before = stock;
            int after;
            try {
                after = switch (operation) {
                    case "INCREASE" -> Math.addExact(before, quantity);
                    case "DECREASE" -> Math.subtractExact(before, quantity);
                    default -> quantity;
                };
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("Số lượng tồn kho vượt quá giới hạn", e);
            }
            if (after < 0) throw new IllegalArgumentException("Số lượng tồn kho mới không hợp lệ");
            Map<String, Object> result = new HashMap<>();
            result.put("variantId", variantId);
            result.put("before", before);
            result.put("after", after);
            if (after == before) {
                result.put("changed", false);
                em.getTransaction().commit();
                return result;
            }
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
            result.put("changed", true);
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
        EntityManager em = entityManagers.get();
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
