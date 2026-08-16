package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import entity.Orders;
import entity.User;
import servlet.AdminRefundServlet;

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
    void returnedToStoreIsEligibleForRefund() {
        assertNull(RefundService.validate("REFUNDED", "PENDING", "PAID", "RETURNED_TO_STORE", new BigDecimal("100000"), new BigDecimal("100000"), null, "BANK-123"));
    }

    @Test
    void refundListResolvesProcessorNamesWithoutChangingOrderSchema() throws Exception {
        String servlet = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/servlet/AdminRefundServlet.java"));
        String order = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/entity/Orders.java"));
        assertTrue(servlet.contains("resolveProcessorNames(pending, userDAO::findByIds)"));
        assertFalse(servlet.contains("userDAO.findById("));
        assertTrue(servlet.contains("m.put(\"refundProcessedByName\""));
        assertTrue(order.contains("private Integer refundProcessedBy"));
        assertFalse(order.contains("refundProcessedByName"));
    }

    @Test
    void refundProcessorResolverLoadsUniqueIdsOnceAndMapsMissingOrBlankNamesToNull() {
        Orders first = new Orders();
        first.setRefundProcessedBy(7);
        Orders duplicate = new Orders();
        duplicate.setRefundProcessedBy(7);
        Orders blank = new Orders();
        blank.setRefundProcessedBy(8);
        Orders deleted = new Orders();
        deleted.setRefundProcessedBy(9);
        Orders unprocessed = new Orders();
        AtomicInteger loads = new AtomicInteger();
        User namedUser = new User();
        namedUser.setUserId(7);
        namedUser.setFullName("Admin A");
        User blankUser = new User();
        blankUser.setUserId(8);
        blankUser.setFullName("   ");

        Map<Integer, String> names = AdminRefundServlet.resolveProcessorNames(
                List.of(first, duplicate, blank, deleted, unprocessed), ids -> {
                    loads.incrementAndGet();
                    assertEquals(Set.of(7, 8, 9), ids);
                    return List.of(namedUser, blankUser);
                });

        assertEquals(1, loads.get());
        assertEquals(Set.of(7, 8, 9), names.keySet());
        assertEquals("Admin A", names.get(7));
        assertNull(names.get(8));
        assertNull(names.get(9));
    }

    @Test
    void rejectsUnknownStatusAndIneligibleOrder() {
        assertNotNull(RefundService.validate("APPROVED", "PENDING", "PAID", "CANCELLED", null, null, null));
        assertNotNull(RefundService.validate("REFUNDED", "PENDING", "UNPAID", "CANCELLED", new BigDecimal("50000"), new BigDecimal("100000"), null));
        assertNotNull(RefundService.validate("REFUNDED", "PENDING", "PAID", "DELIVERED", new BigDecimal("50000"), new BigDecimal("100000"), null));
        assertNotNull(RefundService.validate("REFUNDED", null, "PAID", "CANCELLED", new BigDecimal("50000"), new BigDecimal("100000"), null));
    }
}
