package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import entity.Orders;

class RefundPolicyTest {

    @Test
    void refundedRequiresFullAmountAndNonBlankManualReference() {
        assertNull(RefundService.validate("REFUNDED", "PENDING", "PAID", "CANCELLED", new BigDecimal("100000"), new BigDecimal("100000"), null, "BANK-123"));
        assertNotNull(RefundService.validate("REFUNDED", "PENDING", "PAID", "CANCELLED", new BigDecimal("50000"), new BigDecimal("100000"), null, "BANK-123"));
        assertNotNull(RefundService.validate("REFUNDED", "PENDING", "PAID", "CANCELLED", new BigDecimal("100001"), new BigDecimal("100000"), null, "BANK-123"));
        assertNotNull(RefundService.validate("REFUNDED", "PENDING", "PAID", "CANCELLED", new BigDecimal("100000"), null, null, "BANK-123"));
        assertNotNull(RefundService.validate("REFUNDED", "PENDING", "PAID", "CANCELLED", new BigDecimal("100000"), new BigDecimal("100000"), null, "   "));
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
        assertNotNull(RefundService.validate("REJECTED", "REJECTED", "REFUNDED", "CANCELLED", null, new BigDecimal("100000"), null));
        assertNotNull(RefundService.validate("REFUNDED", "REJECTED", "REFUNDED", "CANCELLED", new BigDecimal("50000"), new BigDecimal("100000"), null));
    }

    @Test
    void terminalRetryMatchesStatusAmountAndNormalizedNoteReferenceOnly() {
        Orders order = new Orders();
        order.setRefundStatus("REFUNDED");
        order.setRefundAmount(new BigDecimal("100000.00"));
        order.setRefundNote(" manual ");
        order.setRefundReference(" BANK-123 ");

        assertTrue(RefundService.matchesTerminalRequest(order, "REFUNDED", new BigDecimal("100000"), "manual", "BANK-123"));
        assertFalse(RefundService.matchesTerminalRequest(order, "REFUNDED", new BigDecimal("99999"), "manual", "BANK-123"));
        assertFalse(RefundService.matchesTerminalRequest(order, "REFUNDED", new BigDecimal("100000"), "changed", "BANK-123"));
        assertFalse(RefundService.matchesTerminalRequest(order, "REFUNDED", new BigDecimal("100000"), "manual", "OTHER"));
        assertFalse(RefundService.matchesTerminalRequest(order, "REJECTED", null, "manual", null));
    }

    @Test
    void rejectsUnknownStatusAndIneligibleOrder() {
        assertNotNull(RefundService.validate("APPROVED", "PENDING", "PAID", "CANCELLED", null, null, null));
        assertNotNull(RefundService.validate("REFUNDED", "PENDING", "UNPAID", "CANCELLED", new BigDecimal("50000"), new BigDecimal("100000"), null));
        assertNotNull(RefundService.validate("REFUNDED", "PENDING", "PAID", "DELIVERED", new BigDecimal("50000"), new BigDecimal("100000"), null));
        assertNotNull(RefundService.validate("REFUNDED", null, "PAID", "CANCELLED", new BigDecimal("50000"), new BigDecimal("100000"), null));
    }
}
