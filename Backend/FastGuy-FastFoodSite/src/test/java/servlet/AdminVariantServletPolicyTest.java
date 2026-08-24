package servlet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AdminVariantServletPolicyTest {
    @Test
    void editorStockChangesAreRejectedInFavorOfInventoryItemApi() throws Exception {
        String product = Files.readString(Path.of("src/main/java/servlet/AdminProductServlet.java"));
        String variant = Files.readString(Path.of("src/main/java/servlet/AdminVariantServlet.java"));

        assertFalse(product.contains("setManagedQuantity("));
        assertFalse(variant.contains("setManagedQuantity("));
        assertTrue(product.contains("Variant inventory must be changed through the inventory item API"));
        assertTrue(variant.contains("Variant inventory must be changed through the inventory item API"));
        assertTrue(product.indexOf("setQuantityAvailable(readStock(") == product.lastIndexOf("setQuantityAvailable(readStock("));
        assertFalse(variant.contains("containsForbiddenStockUpdate"));
    }

    @Test
    void legacyStockAndAuditFieldsAreRejected() throws Exception {
        String product = Files.readString(Path.of("src/main/java/servlet/AdminProductServlet.java"));
        String variant = Files.readString(Path.of("src/main/java/servlet/AdminVariantServlet.java"));

        assertTrue(product.contains("body.containsKey(\"quantityAvailable\")"));
        assertTrue(variant.contains("body.containsKey(\"quantityAvailable\")"));
        assertFalse(product.contains("setManagedQuantity("));
        assertFalse(variant.contains("setManagedQuantity("));
    }
}
