package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;

import org.junit.jupiter.api.Test;

import entity.InventoryTransaction;
import entity.ProductVariant;
import exception.InventoryConflictException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

class InventoryAdjustmentPolicyTest {

    @Test
    void staleExpectedQuantityDoesNotMutateOrPersist() {
        ProductVariant variant = variant(27);
        PersistenceCapture capture = new PersistenceCapture(variant);
        InventoryAdjustmentService service = new InventoryAdjustmentService(capture::entityManager);

        InventoryConflictException conflict = assertThrows(InventoryConflictException.class,
                () -> service.adjust(12, "INCREASE", 3, 26, "STOCK_COUNT", null, 1));

        assertEquals(12, conflict.getVariantId());
        assertEquals(27, conflict.getCurrentQuantity());
        assertEquals(27, variant.getQuantityAvailable());
        assertFalse(capture.persisted);
        assertTrue(capture.rolledBack);
    }

    @Test
    void noOpSetDoesNotMutateOrPersist() {
        ProductVariant variant = variant(27);
        PersistenceCapture capture = new PersistenceCapture(variant);
        InventoryAdjustmentService service = new InventoryAdjustmentService(capture::entityManager);

        Map<String, Object> result = service.adjust(12, "SET", 27, 27, "STOCK_COUNT", null, 1);

        assertEquals(false, result.get("changed"));
        assertEquals(27, variant.getQuantityAvailable());
        assertFalse(capture.persisted);
        assertTrue(capture.committed);
    }

    @Test
    void changedAdjustmentMutatesAndPersistsLedger() {
        ProductVariant variant = variant(27);
        PersistenceCapture capture = new PersistenceCapture(variant);
        InventoryAdjustmentService service = new InventoryAdjustmentService(capture::entityManager);

        Map<String, Object> result = service.adjust(12, "DECREASE", 2, 27, "DAMAGE", "Damaged", 1);

        assertEquals(true, result.get("changed"));
        assertEquals(25, variant.getQuantityAvailable());
        assertTrue(capture.persisted);
        assertEquals(2, capture.transaction.getQuantity());
        assertEquals(27, capture.transaction.getQuantityBefore());
        assertEquals(25, capture.transaction.getQuantityAfter());
    }

    private ProductVariant variant(int quantity) {
        ProductVariant variant = new ProductVariant();
        variant.setVariantId(12);
        variant.setQuantityAvailable(quantity);
        return variant;
    }

    private class PersistenceCapture {
        private final ProductVariant variant;
        private boolean persisted;
        private boolean committed;
        private boolean rolledBack;
        private InventoryTransaction transaction;

        private PersistenceCapture(ProductVariant variant) {
            this.variant = variant;
        }

        private EntityManager entityManager() {
            EntityTransaction tx = (EntityTransaction) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] {EntityTransaction.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "begin" -> { return null; }
                            case "commit" -> { committed = true; return null; }
                            case "rollback" -> { rolledBack = true; return null; }
                            case "isActive" -> { return !committed && !rolledBack; }
                            default -> { return defaultValue(method.getReturnType()); }
                        }
                    });
            return (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] {EntityManager.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getTransaction" -> { return tx; }
                            case "find" -> {
                                if (args[0] == ProductVariant.class) return variant;
                                return null;
                            }
                            case "persist" -> {
                                persisted = true;
                                transaction = (InventoryTransaction) args[0];
                                return null;
                            }
                            default -> { return defaultValue(method.getReturnType()); }
                        }
                    });
        }
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        return 0;
    }
}
