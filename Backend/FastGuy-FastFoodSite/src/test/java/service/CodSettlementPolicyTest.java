package service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CodSettlementPolicyTest {
    @Test void acceptsNonNegativeSubmission() {
        assertDoesNotThrow(() -> CodSettlementService.validateSubmission(new BigDecimal("0.00")));
    }

    @Test void rejectsMissingOrNegativeSubmission() {
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateSubmission(null));
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateSubmission(new BigDecimal("-0.01")));
    }

    @Test void settledMustEqualSubmittedAmount() {
        assertDoesNotThrow(() -> CodSettlementService.validateVerification("SETTLED", new BigDecimal("100000.00"), new BigDecimal("100000.00"), null));
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateVerification("SETTLED", new BigDecimal("100000.00"), new BigDecimal("99999.00"), null));
    }

    @Test void shortAndOverRequireDirectionAndReason() {
        assertDoesNotThrow(() -> CodSettlementService.validateVerification("SHORT", new BigDecimal("100000.00"), new BigDecimal("90000.00"), "Thiếu tiền mặt"));
        assertDoesNotThrow(() -> CodSettlementService.validateVerification("OVER", new BigDecimal("100000.00"), new BigDecimal("110000.00"), "Nộp dư"));
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateVerification("SHORT", new BigDecimal("100000.00"), new BigDecimal("90000.00"), " "));
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateVerification("OVER", new BigDecimal("100000.00"), new BigDecimal("90000.00"), "Sai chiều"));
    }

    @Test void rejectsUnknownOrNegativeVerification() {
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateVerification("SUBMITTED", BigDecimal.ZERO, BigDecimal.ZERO, null));
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateVerification("SETTLED", BigDecimal.ZERO, new BigDecimal("-0.01"), null));
    }

    @Test void rejectsNullVerificationStatusAsInvalidArgument() {
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateVerification(null, BigDecimal.ZERO, BigDecimal.ZERO, null));
    }

    @Test void amountsMustFitDecimal18Scale2BeforePersistence() {
        assertDoesNotThrow(() -> CodSettlementService.validateSubmission(new BigDecimal("9999999999999999.99")));
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateSubmission(new BigDecimal("10000000000000000.00")));
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateSubmission(new BigDecimal("123456789012345678")));
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateSubmission(new BigDecimal("1E+18")));
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateSubmission(new BigDecimal("1.001")));
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateVerification("SETTLED", new BigDecimal("1.001"), new BigDecimal("1.001"), null));
    }

    @Test void acceptedAmountsNormalizeExactlyToScaleTwo() {
        assertEquals(new BigDecimal("12.00"), CodSettlementService.normalizeAmount(new BigDecimal("12"), "invalid"));
        assertEquals(new BigDecimal("12.30"), CodSettlementService.normalizeAmount(new BigDecimal("12.3"), "invalid"));
        assertEquals(new BigDecimal("9999999999999999.99"), CodSettlementService.normalizeAmount(new BigDecimal("9999999999999999.99"), "invalid"));
    }

    @Test void differenceIsSubmittedMinusExpected() {
        assertEquals(new BigDecimal("-20.00"), CodSettlementService.difference(new BigDecimal("80"), new BigDecimal("100")));
        assertEquals(new BigDecimal("20.00"), CodSettlementService.difference(new BigDecimal("120"), new BigDecimal("100")));
    }

    @Test void mismatchReasonIsTrimmedNonblankAndAtMost500Characters() {
        String exact500 = "x".repeat(500);
        assertDoesNotThrow(() -> CodSettlementService.validateVerification("SHORT", BigDecimal.TEN, BigDecimal.ONE, "  " + exact500 + "  "));
        assertThrows(IllegalArgumentException.class, () -> CodSettlementService.validateVerification("OVER", BigDecimal.ONE, BigDecimal.TEN, "x".repeat(501)));
    }
}
