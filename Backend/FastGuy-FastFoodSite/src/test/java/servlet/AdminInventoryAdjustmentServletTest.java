package servlet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import java.nio.file.*;
import org.junit.jupiter.api.Test;

class AdminInventoryAdjustmentServletTest {
    @Test void legacyVariantAndWasteServletIsUnmapped() throws Exception {
        String source=Files.readString(Path.of("src/main/java/servlet/AdminInventoryAdjustmentServlet.java"));
        assertFalse(source.contains("@WebServlet"));
        assertFalse(source.contains("/waste"));
        assertFalse(source.contains("setManagedQuantity("));
    }
}
