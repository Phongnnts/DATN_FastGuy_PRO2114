package exception;

import java.math.BigDecimal;

public class InventoryItemConflictException extends RuntimeException {

    private final BigDecimal currentOnHandQuantity;

    public InventoryItemConflictException(
        String message,
        BigDecimal currentOnHandQuantity
    ) {
        super(message);
        this.currentOnHandQuantity = currentOnHandQuantity;
    }

    public BigDecimal getCurrentOnHandQuantity() {
        return currentOnHandQuantity;
    }
}
