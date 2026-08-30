package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OperationsFinanceSourcePolicyTest {
    @Test
    void manualCheckInUsesSafeMarginAndRunsBeforeOtherFixtureSetup() throws Exception {
        String source = Files.readString(Path.of("src/test/java/integration/OperationsFinanceIT.java"));

        int businessNow = source.indexOf("LocalDateTime businessNow = LocalDateTime.now(BUSINESS_ZONE).withNano(0)");
        int midnightGuard = source.indexOf("Assumptions.assumeTrue(!businessNow.toLocalTime().isBefore(LocalTime.of(0, 2))", businessNow);
        int today = source.indexOf("LocalDate today = businessNow.toLocalDate()", businessNow);
        int manualUser = source.indexOf("int manualUser = user(em, \"SHIPPER\", \"Manual\")", today);
        int manualWindow = source.indexOf("LocalTime[] manualWindow = manualCheckInWindow(businessNow.toLocalTime())", manualUser);
        int manualShift = source.indexOf("shift(em, manualUser, today, manualWindow[0], manualWindow[1], \"EVENING\", \"SCHEDULED\", null)", manualWindow);
        int manualCheck = source.indexOf("new WorkShiftService().check(manual, manualUser, true)", manualShift);
        int manualAssertion = source.indexOf("assertShift(em, manual, \"CHECKED_IN\", \"MANUAL\")", manualCheck);
        int morningUser = source.indexOf("int morningUser = user(em, \"STAFF\", \"Morning\")", today);
        int afternoonUser = source.indexOf("int afternoonUser = user(em, \"STAFF\", \"Afternoon\")", today);
        int missedUser = source.indexOf("int missedUser = user(em, \"STAFF\", \"Missed\")", today);
        int admin = source.indexOf("int admin = user(em, \"ADMIN\", \"Finance\")", today);

        assertTrue(businessNow >= 0);
        assertTrue(midnightGuard > businessNow && midnightGuard < today,
                "real-transaction fixture must guard its business date immediately after capturing businessNow");
        assertTrue(source.contains("businessNow.toLocalTime().isBefore(LocalTime.of(23, 58))"));
        assertTrue(source.contains("This real-transaction fixture must not cross the business date during manual check-in"));
        assertTrue(today < manualUser && manualUser < manualWindow && manualWindow < manualShift
                && manualShift < manualCheck && manualCheck < manualAssertion,
                "manual check-in must run immediately after capturing the guarded business date");
        assertTrue(manualAssertion < morningUser && manualAssertion < afternoonUser
                && manualAssertion < missedUser && manualAssertion < admin,
                "other users must be created only after manual check-in is verified");
        assertFalse(source.contains("shift(em, manualUser, today, LocalTime.MIDNIGHT, LocalTime.of(23, 59)"));
        assertTrue(source.contains("shift(em, missedUser, today.minusDays(1), LocalTime.of(8, 0), LocalTime.NOON, \"MORNING\", \"SCHEDULED\", null)"));
    }

    @Test
    void unusedShiftCodeHelperStaysRemoved() throws Exception {
        String source = Files.readString(Path.of("src/test/java/integration/OperationsFinanceIT.java"));

        assertFalse(source.contains("code(LocalTime"), "unused code(LocalTime) helper must stay removed");
    }
}
