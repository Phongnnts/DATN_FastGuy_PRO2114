package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import dao.CodSettlementDAO;
import entity.User;
import entity.WorkShift;
import jakarta.persistence.PersistenceException;

class CodSettlementShiftPolicyTest {
    @Test void overnightWindowEndsOnFollowingDay() {
        WorkShift shift = shift(LocalDate.of(2026, 8, 14), LocalTime.of(22, 0), LocalTime.of(6, 0));

        CodSettlementDAO.ShiftWindow window = CodSettlementDAO.shiftWindow(shift);

        assertEquals(LocalDateTime.of(2026, 8, 14, 22, 0), window.start());
        assertEquals(LocalDateTime.of(2026, 8, 15, 6, 0), window.end());
    }

    @Test void sameDayWindowKeepsShiftDate() {
        WorkShift shift = shift(LocalDate.of(2026, 8, 14), LocalTime.of(8, 0), LocalTime.of(16, 0));

        CodSettlementDAO.ShiftWindow window = CodSettlementDAO.shiftWindow(shift);

        assertEquals(LocalDateTime.of(2026, 8, 14, 8, 0), window.start());
        assertEquals(LocalDateTime.of(2026, 8, 14, 16, 0), window.end());
    }

    @Test void previousDateOvernightShiftIsCurrentAfterMidnight() {
        WorkShift overnight = checkedInShift(LocalDate.of(2026, 8, 14), LocalTime.of(22, 0), LocalTime.of(6, 0));

        WorkShift current = CodSettlementService.selectCurrentShift(List.of(overnight), LocalDateTime.of(2026, 8, 15, 1, 0));

        assertEquals(overnight, current);
    }

    @Test void currentShiftRequiresCheckedInStatusAndNoCheckoutAndCurrentInstant() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 1, 0);
        WorkShift wrongStatus = checkedInShift(LocalDate.of(2026, 8, 14), LocalTime.of(22, 0), LocalTime.of(6, 0));
        wrongStatus.setStatus("SCHEDULED");
        WorkShift checkedOut = checkedInShift(LocalDate.of(2026, 8, 14), LocalTime.of(22, 0), LocalTime.of(6, 0));
        checkedOut.setCheckOutAt(now.minusMinutes(1));
        WorkShift ended = checkedInShift(LocalDate.of(2026, 8, 13), LocalTime.of(22, 0), LocalTime.of(6, 0));

        assertFalse(CodSettlementService.isCurrentShift(wrongStatus, now));
        assertFalse(CodSettlementService.isCurrentShift(checkedOut, now));
        assertFalse(CodSettlementService.isCurrentShift(ended, now));
        assertNull(CodSettlementService.selectCurrentShift(List.of(wrongStatus, checkedOut, ended), now));
    }

    @Test void currentWindowIsHalfOpen() {
        WorkShift shift = checkedInShift(LocalDate.of(2026, 8, 14), LocalTime.of(22, 0), LocalTime.of(6, 0));

        assertTrue(CodSettlementService.isCurrentShift(shift, LocalDateTime.of(2026, 8, 14, 22, 0)));
        assertFalse(CodSettlementService.isCurrentShift(shift, LocalDateTime.of(2026, 8, 15, 6, 0)));
    }

    @Test void checkoutRequiresAdminVerifiedSettlementForShipper() {
        assertFalse(CodSettlementService.isVerifiedForCheckout(null));
        assertFalse(CodSettlementService.isVerifiedForCheckout("SUBMITTED"));
        assertTrue(CodSettlementService.isVerifiedForCheckout("SETTLED"));
        assertTrue(CodSettlementService.isVerifiedForCheckout("SHORT"));
        assertTrue(CodSettlementService.isVerifiedForCheckout("OVER"));
    }

    @Test void persistenceDuplicateIsNormalizedButOtherFailuresAreNot() {
        PersistenceException duplicate = new PersistenceException("Violation of UNIQUE KEY constraint 'UQ_CodSettlement_ShipperShift'");
        PersistenceException other = new PersistenceException("connection closed");

        assertTrue(CodSettlementService.isDuplicateSettlementFailure(duplicate));
        assertFalse(CodSettlementService.isDuplicateSettlementFailure(other));
    }

    private static WorkShift checkedInShift(LocalDate date, LocalTime start, LocalTime end) {
        WorkShift shift = shift(date, start, end);
        shift.setStatus("CHECKED_IN");
        shift.setCheckInAt(LocalDateTime.of(date, start));
        return shift;
    }

    private static WorkShift shift(LocalDate date, LocalTime start, LocalTime end) {
        User shipper = new User();
        shipper.setRole("SHIPPER");
        shipper.setStatus("ACTIVE");
        WorkShift shift = new WorkShift();
        shift.setUser(shipper);
        shift.setShiftDate(date);
        shift.setStartTime(start);
        shift.setEndTime(end);
        return shift;
    }
}
