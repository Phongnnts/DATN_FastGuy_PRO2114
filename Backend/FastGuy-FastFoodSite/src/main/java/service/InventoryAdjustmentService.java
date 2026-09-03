package service;

import dao.InventoryItemDAO;
import entity.InventoryItem;
import entity.InventoryTransaction;
import entity.ProductVariant;
import entity.User;
import exception.InventoryConflictException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import utils.DatabaseUtil;

public class InventoryAdjustmentService {

    private static final Set<String> ADJUSTMENT_REASONS = Set.of(
        "STOCK_COUNT",
        "DAMAGE",
        "EXPIRED",
        "OTHER"
    );
    private final Supplier<EntityManager> entityManagers;
    private final InventoryItemDAO inventoryItems = new InventoryItemDAO();

    public InventoryAdjustmentService() {
        this(DatabaseUtil::getEntityManager);
    }

    InventoryAdjustmentService(Supplier<EntityManager> entityManagers) {
        this.entityManagers = entityManagers;
    }

    public Map<String, Object> adjust(
        int variantId,
        String operation,
        int quantity,
        Integer expectedQuantity,
        String reasonCode,
        String note,
        int adminId
    ) {
        if (
            !ADJUSTMENT_REASONS.contains(reasonCode)
        ) throw new IllegalArgumentException(
            "Vui lòng chọn lý do điều chỉnh hợp lệ"
        );
        if (
            "OTHER".equals(reasonCode) && (note == null || note.isBlank())
        ) throw new IllegalArgumentException(
            "Ghi chú là bắt buộc khi chọn lý do Khác"
        );
        if (
            note != null && note.length() > 500
        ) throw new IllegalArgumentException(
            "Ghi chú không được vượt quá 500 ký tự"
        );
        if (
            !Set.of("INCREASE", "DECREASE", "SET").contains(operation)
        ) throw new IllegalArgumentException(
            "Thao tác điều chỉnh không hợp lệ"
        );
        if (
            ("INCREASE".equals(operation) || "DECREASE".equals(operation)) &&
            quantity <= 0
        ) throw new IllegalArgumentException(
            "Số lượng điều chỉnh phải lớn hơn 0"
        );
        if (
            "SET".equals(operation) && quantity < 0
        ) throw new IllegalArgumentException(
            "Số lượng tồn kho mới không hợp lệ"
        );
        EntityManager em = entityManagers.get();
        try {
            em.getTransaction().begin();
            ProductVariant variant = em.find(
                ProductVariant.class,
                variantId,
                LockModeType.PESSIMISTIC_WRITE
            );
            if (variant == null) throw new IllegalArgumentException(
                "Biến thể không tồn tại"
            );
            Integer stock = variant.getQuantityAvailable();
            if (stock == null) throw new IllegalArgumentException(
                "Biến thể không quản lý tồn kho"
            );
            if (
                !Objects.equals(stock, expectedQuantity)
            ) throw new InventoryConflictException(variantId, stock);
            int before = stock;
            int after;
            try {
                after = switch (operation) {
                    case "INCREASE" -> Math.addExact(before, quantity);
                    case "DECREASE" -> Math.subtractExact(before, quantity);
                    default -> quantity;
                };
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException(
                    "Số lượng tồn kho vượt quá giới hạn",
                    e
                );
            }
            if (after < 0) throw new IllegalArgumentException(
                "Số lượng tồn kho mới không hợp lệ"
            );
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
            InventoryItem item = requireFinishedGood(em, variantId);
            item.setOnHandQuantity(
                item.getOnHandQuantity().add(BigDecimal.valueOf(after - before))
            );
            InventoryTransaction txn = new InventoryTransaction();
            txn.setInventoryItem(item);
            txn.setOrder(null);
            txn.setCreatedBy(em.find(User.class, adminId));
            txn.setTransactionType("ADJUSTMENT");
            txn.setQuantity(BigDecimal.valueOf(after - before));
            txn.setReasonCode(reasonCode);
            txn.setNote(note);
            txn.setQuantityBefore(BigDecimal.valueOf(before));
            txn.setQuantityAfter(BigDecimal.valueOf(after));
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

    public Map<String, Object> setManagedQuantity(
        int variantId,
        Integer newQuantity,
        Integer expectedQuantity,
        String reasonCode,
        String note,
        int adminId
    ) {
        return setManagedQuantity(
            variantId,
            newQuantity,
            expectedQuantity,
            reasonCode,
            note,
            adminId,
            null
        );
    }

    public Map<String, Object> setManagedQuantity(
        int variantId,
        Integer newQuantity,
        Integer expectedQuantity,
        String reasonCode,
        String note,
        int adminId,
        ProductVariant metadata
    ) {
        if (
            !ADJUSTMENT_REASONS.contains(reasonCode)
        ) throw new IllegalArgumentException(
            "Vui lòng chọn lý do điều chỉnh hợp lệ"
        );
        if (
            "OTHER".equals(reasonCode) && (note == null || note.isBlank())
        ) throw new IllegalArgumentException(
            "Ghi chú là bắt buộc khi chọn lý do Khác"
        );
        if (
            note != null && note.length() > 500
        ) throw new IllegalArgumentException(
            "Ghi chú không được vượt quá 500 ký tự"
        );
        if (
            newQuantity != null && newQuantity < 0
        ) throw new IllegalArgumentException("Tồn kho không được âm");
        EntityManager em = entityManagers.get();
        try {
            em.getTransaction().begin();
            ProductVariant variant = em.find(
                ProductVariant.class,
                variantId,
                LockModeType.PESSIMISTIC_WRITE
            );
            if (variant == null) throw new IllegalArgumentException(
                "Biến thể không tồn tại"
            );
            Integer before = variant.getQuantityAvailable();
            if (
                !Objects.equals(before, expectedQuantity)
            ) throw new InventoryConflictException(variantId, before);
            if (metadata != null) {
                variant.setVariantName(metadata.getVariantName());
                variant.setPrice(metadata.getPrice());
                variant.setOriginalPrice(metadata.getOriginalPrice());
                variant.setSku(metadata.getSku());
                variant.setWeight(metadata.getWeight());
                variant.setLength(metadata.getLength());
                variant.setWidth(metadata.getWidth());
                variant.setHeight(metadata.getHeight());
                variant.setIsDefault(metadata.getIsDefault());
                variant.setStatus(metadata.getStatus());
                variant.setUpdatedAt(metadata.getUpdatedAt());
            }
            Map<String, Object> result = new HashMap<>();
            result.put("variantId", variantId);
            result.put("before", before);
            result.put("after", newQuantity);
            if (Objects.equals(before, newQuantity)) {
                result.put("changed", false);
                em.getTransaction().commit();
                return result;
            }
            if (
                newQuantity == null || before == null
            ) throw new IllegalArgumentException(
                "Biến thể phải quản lý tồn kho thành phẩm"
            );
            variant.setQuantityAvailable(newQuantity);
            InventoryItem item = requireFinishedGood(em, variantId);
            item.setOnHandQuantity(
                item
                    .getOnHandQuantity()
                    .add(BigDecimal.valueOf(newQuantity - before))
            );
            InventoryTransaction txn = new InventoryTransaction();
            txn.setInventoryItem(item);
            txn.setOrder(null);
            txn.setCreatedBy(em.find(User.class, adminId));
            txn.setTransactionType("ADJUSTMENT");
            txn.setQuantity(BigDecimal.valueOf(newQuantity - before));
            txn.setReasonCode(reasonCode);
            txn.setNote(note);
            txn.setQuantityBefore(BigDecimal.valueOf(before));
            txn.setQuantityAfter(BigDecimal.valueOf(newQuantity));
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

    public Map<String, Object> waste(
        int variantId,
        int quantity,
        String reasonCode,
        String note,
        int adminId
    ) {
        if (quantity <= 0) throw new IllegalArgumentException(
            "Số lượng lãng phí phải lớn hơn 0"
        );
        if (
            reasonCode == null || reasonCode.isBlank()
        ) throw new IllegalArgumentException("Vui lòng chọn lý do lãng phí");
        EntityManager em = entityManagers.get();
        try {
            em.getTransaction().begin();
            ProductVariant variant = em.find(
                ProductVariant.class,
                variantId,
                LockModeType.PESSIMISTIC_WRITE
            );
            if (variant == null) throw new IllegalArgumentException(
                "Biến thể không tồn tại"
            );
            Integer stock = variant.getQuantityAvailable();
            if (stock == null) throw new IllegalArgumentException(
                "Biến thể không quản lý tồn kho"
            );
            if (quantity > stock) throw new IllegalArgumentException(
                "Số lượng lãng phí vượt quá tồn kho hiện tại"
            );
            int before = stock;
            int after = stock - quantity;
            variant.setQuantityAvailable(after);
            InventoryItem item = requireFinishedGood(em, variantId);
            item.setOnHandQuantity(
                item.getOnHandQuantity().subtract(BigDecimal.valueOf(quantity))
            );
            InventoryTransaction txn = new InventoryTransaction();
            txn.setInventoryItem(item);
            txn.setOrder(null);
            txn.setCreatedBy(em.find(User.class, adminId));
            txn.setTransactionType("WASTE");
            txn.setQuantity(BigDecimal.valueOf(quantity).negate());
            txn.setReasonCode(reasonCode);
            txn.setNote(note);
            txn.setQuantityBefore(BigDecimal.valueOf(before));
            txn.setQuantityAfter(BigDecimal.valueOf(after));
            BigDecimal cost = item
                .getAverageUnitCost()
                .setScale(4, java.math.RoundingMode.HALF_UP);
            txn.setUnitCostSnapshot(cost);
            txn.setTotalCost(
                BigDecimal.valueOf(quantity)
                    .multiply(cost)
                    .setScale(4, java.math.RoundingMode.HALF_UP)
            );
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

    private InventoryItem requireFinishedGood(EntityManager em, int variantId) {
        InventoryItem item = inventoryItems.findFinishedGood(em, variantId);
        if (item == null) throw new IllegalArgumentException(
            "Biến thể không có tồn kho thành phẩm"
        );
        return em.find(
            InventoryItem.class,
            item.getInventoryItemId(),
            LockModeType.PESSIMISTIC_WRITE
        );
    }
}
