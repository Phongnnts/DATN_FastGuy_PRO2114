package service;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryAdjustmentPolicyTest {

    private static final Path SERVICE = Path.of("src/main/java/service/InventoryAdjustmentService.java");
    private static final Path SERVLET = Path.of("src/main/java/servlet/AdminInventoryAdjustmentServlet.java");

    private static String read(Path p) throws Exception {
        return Files.readString(p);
    }

    @Test
    void adjustmentRequiresReasonAndNonNegativeQuantity() throws Exception {
        String src = read(SERVICE);
        assertTrue(src.contains("newQuantity < 0"));
        assertTrue(src.contains("Vui lòng chọn lý do điều chỉnh"));
    }

    @Test
    void adjustmentLocksVariantAndRejectsUnmanagedStock() throws Exception {
        String src = read(SERVICE);
        assertTrue(src.contains("LockModeType.PESSIMISTIC_WRITE"));
        assertTrue(src.contains("if (stock == null)"));
        assertTrue(src.contains("Biến thể không quản lý tồn kho"));
    }

    @Test
    void adjustmentPersistsAuditLedgerWithBeforeAfter() throws Exception {
        String src = read(SERVICE);
        assertTrue(src.contains("setTransactionType(\"ADJUSTMENT\")"));
        assertTrue(src.contains("setQuantity(Math.abs(after - before))"));
        assertTrue(src.contains("setQuantityBefore(before)"));
        assertTrue(src.contains("setQuantityAfter(after)"));
        assertTrue(src.contains("setOrder(null)"));
    }

    @Test
    void wasteRejectsQuantityAboveStockAndPersistsWaste() throws Exception {
        String src = read(SERVICE);
        assertTrue(src.contains("quantity <= 0"));
        assertTrue(src.contains("quantity > stock"));
        assertTrue(src.contains("setTransactionType(\"WASTE\")"));
        assertTrue(src.contains("variant.setQuantityAvailable(after)"));
    }

    @Test
    void mutationServletRoutesAdjustmentsAndWasteWithAdminAuth() throws Exception {
        String src = read(SERVLET);
        assertTrue(src.contains("@WebServlet(\"/api/admin/inventory/transactions/*\")"));
        assertTrue(src.contains("\"/adjustments\".equals(path)"));
        assertTrue(src.contains("\"/waste\".equals(path)"));
        assertTrue(src.contains("PrivilegedAuth.isActiveRole"));
        assertTrue(src.contains("adjustmentService.adjust("));
        assertTrue(src.contains("adjustmentService.waste("));
    }
}
