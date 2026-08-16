package service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import entity.Orders;
import entity.User;
import entity.WorkShift;

class DeliveryFailureMutationContractTest {
    private static final Path SOURCE = Path.of("src/main/java/service/OrderTransitionService.java");

    @Test
    void exposesAtomicRecoveryMutationContracts() throws Exception {
        String source = Files.readString(SOURCE);
        assertTrue(source.contains("MutationResult reportDeliveryFailure(int orderId, int shipperId, String expectedStatus, String reasonCode, String note)"));
        assertTrue(source.contains("MutationResult retryDelivery(int orderId, int staffId, String expectedStatus, int shipperId, String retryMode, LocalDateTime scheduledAt, String note)"));
        assertTrue(source.contains("MutationResult startScheduledRetry(int orderId, int staffId, String expectedStatus)"));
        assertTrue(source.contains("MutationResult returnToStore(int orderId, int staffId, String expectedStatus, String note)"));
        assertTrue(source.contains("MutationResult overrideDeliveryAttemptLimit(int orderId, int adminId, String expectedStatus, String note)"));
        assertTrue(source.contains("SUCCESS, CONFLICT, INVALID, UNPROCESSABLE"));
    }

    @Test
    void recoveryMutationsLockValidateAndPersistHistoryInsideTransaction() throws Exception {
        String source = Files.readString(SOURCE);
        for (String method : new String[] {"reportDeliveryFailure", "retryDelivery", "startScheduledRetry", "returnToStore", "overrideDeliveryAttemptLimit"}) {
            int start = source.indexOf("MutationResult " + method + "(");
            int next = source.indexOf("public MutationResult ", start + 25);
            String body = source.substring(start, next < 0 ? source.length() : next);
            assertTrue(body.contains("LockModeType.PESSIMISTIC_WRITE"), method);
            assertTrue(body.contains("matchesExpectedStatus"), method);
            assertTrue(body.contains("OrderStatusHistory"), method);
            assertTrue(body.contains("commit()"), method);
        }
    }

    @Test
    void recoveryNotesAreNormalizedBeforeHistoryAndBusinessClockIsUsed() throws Exception {
        String source = Files.readString(SOURCE);
        assertTrue(source.contains("DeliveryFailurePolicy.normalizeNote(note)"));
        assertTrue(source.contains("WorkShiftService.businessNow()"));
        assertTrue(!source.contains("DeliveryFailurePolicy.isValidSchedule(retryMode, scheduledAt, LocalDateTime.now(),"));
        assertTrue(!source.contains("order.getRetryScheduledAt().isAfter(LocalDateTime.now())"));
    }

    @Test
    void currentShiftQueryLocksSelectedRowsForMutationValidation() throws Exception {
        String source = Files.readString(SOURCE);
        int start = source.indexOf("private WorkShift currentActiveShift(");
        int end = source.indexOf("static boolean isCurrentActiveShift", start);
        String body = source.substring(start, end);
        assertTrue(body.contains(".setLockMode(LockModeType.PESSIMISTIC_WRITE)"));
        assertTrue(body.indexOf(".setLockMode(LockModeType.PESSIMISTIC_WRITE)") < body.indexOf(".getResultList()"));
    }

    @Test
    void currentShipperShiftRejectsStaleDateAndExpiredGrace() {
        User shipper = user("SHIPPER", "ACTIVE");
        WorkShift shift = shift(shipper, LocalDate.of(2026, 8, 14), LocalTime.of(9, 0), LocalTime.of(17, 0));

        assertTrue(OrderTransitionService.isCurrentActiveShift(shift, shipper, "SHIPPER", LocalDateTime.of(2026, 8, 14, 17, 15)));
        assertTrue(!OrderTransitionService.isCurrentActiveShift(shift, shipper, "SHIPPER", LocalDateTime.of(2026, 8, 14, 17, 16)));
        assertTrue(!OrderTransitionService.isCurrentActiveShift(shift, shipper, "SHIPPER", LocalDateTime.of(2026, 8, 15, 10, 0)));
    }

    @Test
    void scheduledRetryRequiresCurrentActiveAssignedShipper() {
        Orders order = new Orders();
        User shipper = user("SHIPPER", "ACTIVE");
        order.setShipper(shipper);
        WorkShift shift = shift(shipper, LocalDate.of(2026, 8, 14), LocalTime.of(9, 0), LocalTime.of(17, 0));
        LocalDateTime now = LocalDateTime.of(2026, 8, 14, 12, 0);

        assertTrue(OrderTransitionService.canStartScheduledRetry(order, shift, now));
        shipper.setStatus("INACTIVE");
        assertTrue(!OrderTransitionService.canStartScheduledRetry(order, shift, now));
        order.setShipper(null);
        assertTrue(!OrderTransitionService.canStartScheduledRetry(order, shift, now));
    }

    @Test
    void returnUsesReservationCancellationAndRefundsOnlyPaidOrders() throws Exception {
        String source = Files.readString(SOURCE);
        int start = source.indexOf("MutationResult returnToStore(");
        int end = source.indexOf("public MutationResult overrideDeliveryAttemptLimit", start);
        String body = source.substring(start, end);
        assertTrue(body.contains("inventoryReservationService.cancel(em, order)"));
        assertTrue(body.contains("releaseCoupon(em, orderId)"));
        assertTrue(body.contains("\"PAID\".equals(order.getPaymentStatus())"));
        assertTrue(body.contains("setRefundStatus(\"PENDING\")"));
    }

    private static User user(String role, String status) {
        User user = new User();
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    private static WorkShift shift(User user, LocalDate date, LocalTime start, LocalTime end) {
        WorkShift shift = new WorkShift();
        shift.setUser(user);
        shift.setShiftDate(date);
        shift.setStartTime(start);
        shift.setEndTime(end);
        shift.setStatus("CHECKED_IN");
        shift.setCheckInAt(LocalDateTime.of(date, start));
        return shift;
    }
}
