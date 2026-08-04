package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class RefundPolicyTest {

    @Test
    void refundedRequiresPositiveAmountNotExceedingFinalAmount() {
        assertNull(RefundService.validate("REFUNDED", "PENDING", "PAID", "CANCELLED", new BigDecimal("50000"), new BigDecimal("100000"), null));
        assertNull(RefundService.validate("REFUNDED", "PENDING", "PAID", "CANCELLED", new BigDecimal("100000"), new BigDecimal("100000"), null));
        assertNull(RefundService.validate("REFUNDED", "PENDING", "PAID", "CANCELLED", new BigDecimal("50000"), null, null));
        assertNotNull(RefundService.validate("REFUNDED", "PENDING", "PAID", "CANCELLED", null, new BigDecimal("100000"), null));
        assertNotNull(RefundService.validate("REFUNDED", "PENDING", "PAID", "CANCELLED", BigDecimal.ZERO, new BigDecimal("100000"), null));
        assertNotNull(RefundService.validate("REFUNDED", "PENDING", "PAID", "CANCELLED", new BigDecimal("-1"), new BigDecimal("100000"), null));
        assertNotNull(RefundService.validate("REFUNDED", "PENDING", "PAID", "CANCELLED", new BigDecimal("100001"), new BigDecimal("100000"), null));
    }

    @Test
    void rejectedRequiresNonBlankNoteAndDoesNotReverseOrRepay() {
        assertNull(RefundService.validate("REJECTED", "PENDING", "PAID", "CANCELLED", null, new BigDecimal("100000"), "Thiếu chứng từ"));
        assertNotNull(RefundService.validate("REJECTED", "PENDING", "PAID", "CANCELLED", null, new BigDecimal("100000"), null));
        assertNotNull(RefundService.validate("REJECTED", "PENDING", "PAID", "CANCELLED", null, new BigDecimal("100000"), "   "));
        assertNull(RefundService.paymentStatusFor("REJECTED"));
        assertEquals("REFUNDED", RefundService.paymentStatusFor("REFUNDED"));
        assertFalse(RefundService.reverseForStatus("REJECTED"));
        assertTrue(RefundService.reverseForStatus("REFUNDED"));
    }

    @Test
    void terminalRefundIsIdempotentOnlyForSameStatus() {
        assertTrue(RefundService.isTerminal("REFUNDED"));
        assertTrue(RefundService.isTerminal("REJECTED"));
        assertFalse(RefundService.isTerminal("PENDING"));
        assertFalse(RefundService.isTerminal(null));
        assertTrue(RefundService.isIdempotent("REFUNDED", "REFUNDED"));
        assertTrue(RefundService.isIdempotent("REJECTED", "REJECTED"));
        assertFalse(RefundService.isIdempotent("REFUNDED", "REJECTED"));
        assertFalse(RefundService.isIdempotent("REJECTED", "REFUNDED"));
        assertFalse(RefundService.isIdempotent("REFUNDED", "PENDING"));
        assertNull(RefundService.validate("REJECTED", "REJECTED", "REFUNDED", "CANCELLED", null, new BigDecimal("100000"), null));
        assertNotNull(RefundService.validate("REFUNDED", "REJECTED", "REFUNDED", "CANCELLED", new BigDecimal("50000"), new BigDecimal("100000"), null));
    }

    @Test
    void rejectsUnknownStatusAndIneligibleOrder() {
        assertNotNull(RefundService.validate("APPROVED", "PENDING", "PAID", "CANCELLED", null, null, null));
        assertNotNull(RefundService.validate("REFUNDED", "PENDING", "UNPAID", "CANCELLED", new BigDecimal("50000"), new BigDecimal("100000"), null));
        assertNotNull(RefundService.validate("REFUNDED", "PENDING", "PAID", "DELIVERED", new BigDecimal("50000"), new BigDecimal("100000"), null));
        assertNotNull(RefundService.validate("REFUNDED", null, "PAID", "CANCELLED", new BigDecimal("50000"), new BigDecimal("100000"), null));
    }
}
