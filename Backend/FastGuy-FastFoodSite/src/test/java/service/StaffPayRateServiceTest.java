package service;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StaffPayRateServiceTest {
    @Test void calculatesAndRoundsEachComponentHalfUp() {
        StaffPayRateService.Pay pay=StaffPayRateService.calculate(61,31,new BigDecimal("30000.00"),new BigDecimal("45000.00"));
        assertEquals(new BigDecimal("30500.00"),pay.regular());
        assertEquals(new BigDecimal("23250.00"),pay.overtime());
        assertEquals(new BigDecimal("53750.00"),pay.total());
    }
    @Test void rejectsInvalidMoney() {
        assertThrows(IllegalArgumentException.class,()->StaffPayRateService.money(new BigDecimal("0")));
        assertThrows(IllegalArgumentException.class,()->StaffPayRateService.money(new BigDecimal("1.001")));
    }
}
