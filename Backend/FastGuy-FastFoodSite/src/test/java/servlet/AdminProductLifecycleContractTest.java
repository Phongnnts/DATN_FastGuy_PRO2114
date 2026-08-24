package servlet;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AdminProductLifecycleContractTest {
    @Test
    void exposesRestoreAndPermanentDeleteWithConflictMapping() throws Exception {
        String source = Files.readString(Path.of("src/main/java/servlet/AdminProductServlet.java"));
        assertTrue(source.contains("\"restore\".equals(segs[1])"));
        assertTrue(source.contains("\"permanent\".equals(segs[1])"));
        assertTrue(source.contains("Product restored"));
        assertTrue(source.contains("Product permanently deleted"));
        assertTrue(source.contains("ProductDAO.ProductInUseException"));
        assertTrue(source.contains(", 409)"));
    }
}
