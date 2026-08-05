package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StaffOrderDetailPolicyTest {
    @Test
    void validatesNoteAgainstBlankRequestMissingOrderAndStorageLimit() {
        assertEquals("Note is required", StaffOrderServlet.validateNote(null, "note", true));
        assertEquals("Note is required", StaffOrderServlet.validateNote("   ", "note", true));
        assertEquals("Order not found", StaffOrderServlet.validateNote("note", null, false));
        assertEquals("Note is too long", StaffOrderServlet.validateNote("x".repeat(1001), null, false));
        assertEquals("Note is too long", StaffOrderServlet.validateNote("b", "a".repeat(995), true));
        assertNull(StaffOrderServlet.validateNote(" b ", "a".repeat(994), true));
        assertEquals(1000, StaffOrderServlet.appendNote("a".repeat(994), " b ").length());
    }

    @Test
    void detailDtoExposesFinancialFieldsAndSerializedRefundTime() throws IOException {
        String servlet = Files.readString(Path.of("src/main/java/servlet/StaffOrderServlet.java"));
        assertTrue(servlet.contains("m.put(\"discountAmount\", o.getDiscountAmount())"));
        assertTrue(servlet.contains("m.put(\"refundAmount\", o.getRefundAmount())"));
        assertTrue(servlet.contains("m.put(\"refundedAt\", o.getRefundedAt() != null ? o.getRefundedAt().toString() : null)"));
    }
}
