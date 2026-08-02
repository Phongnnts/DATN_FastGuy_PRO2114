package service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class LoyaltyServiceTest {
    @Test
    void tierBoundaries() {
        assertEquals("Bronze", LoyaltyService.tierForPoints(0));
        assertEquals("Bronze", LoyaltyService.tierForPoints(499));
        assertEquals("Silver", LoyaltyService.tierForPoints(500));
        assertEquals("Silver", LoyaltyService.tierForPoints(1999));
        assertEquals("Gold", LoyaltyService.tierForPoints(2000));
    }

    @Test
    void pointsDoNotCreateZeroEarn() {
        assertEquals(0, LoyaltyService.pointsForAmount(null));
        assertEquals(0, LoyaltyService.pointsForAmount(new BigDecimal("999")));
        assertEquals(1, LoyaltyService.pointsForAmount(new BigDecimal("1000")));
        assertEquals(12, LoyaltyService.pointsForAmount(new BigDecimal("12999")));
    }
}
