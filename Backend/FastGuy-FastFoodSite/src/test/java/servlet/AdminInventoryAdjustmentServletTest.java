package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import exception.InventoryConflictException;

class AdminInventoryAdjustmentServletTest {
    private static final Path SERVLET = Path.of("src/main/java/servlet/AdminInventoryAdjustmentServlet.java");

    @Test
    void staleExpectedQuantityReturns409WithCurrentQuantity() throws Exception {
        InventoryConflictException conflict = new InventoryConflictException(12, 27);
        assertEquals(12, conflict.getVariantId());
        assertEquals(27, conflict.getCurrentQuantity());
        String src = Files.readString(SERVLET);
        assertTrue(src.contains("catch (InventoryConflictException e)"));
        assertTrue(src.contains("409"));
        assertTrue(src.contains("currentQuantity"));
    }

    @Test
    void adjustmentPayloadUsesOperationQuantityAndExpectedQuantity() throws Exception {
        String src = Files.readString(SERVLET);
        assertTrue(src.contains("body.get(\"operation\")"));
        assertTrue(src.contains("body.get(\"quantity\")"));
        assertTrue(src.contains("body.get(\"expectedQuantity\")"));
        assertFalse(src.contains("body.get(\"newQuantity\")"));
    }
}
