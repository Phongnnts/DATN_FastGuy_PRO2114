package exception;

public class InventoryConflictException extends RuntimeException {
    private final int variantId;
    private final Integer currentQuantity;

    public InventoryConflictException(int variantId, Integer currentQuantity) {
        super("Tồn kho đã thay đổi, vui lòng kiểm tra lại");
        this.variantId = variantId;
        this.currentQuantity = currentQuantity;
    }

    public int getVariantId() {
        return variantId;
    }

    public Integer getCurrentQuantity() {
        return currentQuantity;
    }
}
