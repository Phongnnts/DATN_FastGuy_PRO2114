package servlet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AdminVariantServletPolicyTest {
    @Test
    void editorStockChangesDelegateToInventoryService() throws Exception {
        String product = Files.readString(Path.of("src/main/java/servlet/AdminProductServlet.java"));
        String variant = Files.readString(Path.of("src/main/java/servlet/AdminVariantServlet.java"));

        assertTrue(product.contains("setManagedQuantity("));
        assertTrue(variant.contains("setManagedQuantity("));
        assertTrue(product.indexOf("setQuantityAvailable(readStock(") == product.lastIndexOf("setQuantityAvailable(readStock("));
        assertFalse(variant.contains("containsForbiddenStockUpdate"));
    }
}
