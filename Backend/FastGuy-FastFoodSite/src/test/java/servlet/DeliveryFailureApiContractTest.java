package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import entity.Orders;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import service.OrderTransitionService;

class DeliveryFailureApiContractTest {
    @Test
    void parsesOnlyExactFailureMutationRoutes() {
        assertEquals("fail", ShipperServlet.parseMutationPath("/orders/12/fail").action());
        assertEquals("retry-delivery", StaffOrderServlet.parseDeliveryMutationPath("/12/retry-delivery").action());
        assertEquals("start-scheduled-retry", StaffOrderServlet.parseDeliveryMutationPath("/12/start-scheduled-retry").action());
        assertEquals("return-to-store", StaffOrderServlet.parseDeliveryMutationPath("/12/return-to-store").action());
        assertEquals(12, AdminOrderServlet.parseDeliveryOverridePath("/12/delivery-attempt-override"));
        assertNull(StaffOrderServlet.parseDeliveryMutationPath("/12/retry-delivery/extra"));
        assertNull(AdminOrderServlet.parseDeliveryOverridePath("/12/cancel"));
    }

    @Test
    void validatesAndTrimsCamelCasePayloads() {
        Map<String, Object> failure = ShipperServlet.validateFailurePayload(Map.of(
                "expectedStatus", " PICKED_UP ", "reasonCode", " ADDRESS_NOT_FOUND ", "note", " details "));
        assertEquals("PICKED_UP", failure.get("expectedStatus"));
        assertEquals("ADDRESS_NOT_FOUND", failure.get("reasonCode"));
        assertEquals("details", failure.get("note"));
        assertNull(ShipperServlet.validateFailurePayload(Map.of("expectedStatus", 1, "reasonCode", "X", "note", "x")));

        Map<String, Object> retry = StaffOrderServlet.validateRetryPayload(Map.of(
                "expectedStatus", " DELIVERY_FAILED ", "shipperId", 7, "retryMode", " SCHEDULED ",
                "scheduledAt", "2026-08-15T10:00:00", "note", " retry "));
        assertEquals(LocalDateTime.of(2026, 8, 15, 10, 0), retry.get("scheduledAt"));
        assertNull(StaffOrderServlet.validateRetryPayload(Map.of("expectedStatus", "DELIVERY_FAILED", "shipperId", "7", "retryMode", "IMMEDIATE")));
    }

    @Test
    void rejectsFractionalAndOutOfRangeShipperIds() {
        Map<String, Object> base = new HashMap<>();
        base.put("expectedStatus", "DELIVERY_FAILED");
        base.put("retryMode", "IMMEDIATE");
        base.put("shipperId", new BigDecimal("7.5"));
        assertNull(StaffOrderServlet.validateRetryPayload(base));
        base.put("shipperId", Long.MAX_VALUE);
        assertNull(StaffOrderServlet.validateRetryPayload(base));
        base.put("shipperId", 0);
        assertNull(StaffOrderServlet.validateRetryPayload(base));
    }

    @Test
    void distinguishesAbsentOwnedAndCrossOwnerShipperOrders() {
        assertEquals(404, ShipperServlet.ownershipStatus(null, 7));
        Orders owned = new Orders();
        entity.User owner = new entity.User();
        owner.setUserId(7);
        owned.setShipper(owner);
        assertEquals(200, ShipperServlet.ownershipStatus(owned, 7));
        assertEquals(403, ShipperServlet.ownershipStatus(owned, 8));
    }

    @Test
    void queueDtoContainsFailureIncidentFields() {
        Orders order = new Orders();
        order.setDeliveryAttemptCount(1);
        order.setDeliveryAttemptLimit(2);
        order.setDeliveryFailureCode("ADDRESS_NOT_FOUND");
        order.setFailureReason("details");
        order.setDeliveryFailedAt(LocalDateTime.of(2026, 8, 14, 10, 0));
        order.setRetryScheduledAt(LocalDateTime.of(2026, 8, 15, 10, 0));
        order.setReturnedToStoreAt(LocalDateTime.of(2026, 8, 16, 10, 0));
        Map<String, Object> dto = StaffOrderServlet.toFailureQueueItem(order);
        assertEquals(1, dto.get("deliveryAttemptCount"));
        assertEquals(2, dto.get("deliveryAttemptLimit"));
        assertEquals("ADDRESS_NOT_FOUND", dto.get("deliveryFailureCode"));
        assertEquals("details", dto.get("failureNote"));
        assertEquals("2026-08-14T10:00", dto.get("deliveryFailedAt"));
        assertEquals("2026-08-15T10:00", dto.get("retryScheduledAt"));
        assertEquals("2026-08-16T10:00", dto.get("returnedToStoreAt"));
    }

    @Test
    void mapsMutationResultsToRequiredHttpStatuses() {
        assertEquals(200, ShipperServlet.statusFor(OrderTransitionService.MutationResult.SUCCESS));
        assertEquals(409, ShipperServlet.statusFor(OrderTransitionService.MutationResult.CONFLICT));
        assertEquals(422, ShipperServlet.statusFor(OrderTransitionService.MutationResult.UNPROCESSABLE));
        assertEquals(400, ShipperServlet.statusFor(OrderTransitionService.MutationResult.INVALID));
    }

    @Test
    void servicesDelegateToTaskThreeMutations() throws IOException {
        String shipper = Files.readString(Path.of("src/main/java/service/ShipperService.java"));
        String staff = Files.readString(Path.of("src/main/java/service/StaffOrderService.java"));
        assertTrue(shipper.contains("reportDeliveryFailure(orderId, shipperId, expectedStatus, reasonCode, note)"));
        assertTrue(staff.contains("transitionService.retryDelivery"));
        assertTrue(staff.contains("transitionService.startScheduledRetry"));
        assertTrue(staff.contains("transitionService.returnToStore"));
    }

    @Test
    void staffQueueAndOwnerDtosExposeIncidentFields() throws IOException {
        String dao = Files.readString(Path.of("src/main/java/dao/OrdersDAO.java"));
        String staff = Files.readString(Path.of("src/main/java/servlet/StaffOrderServlet.java"));
        String admin = Files.readString(Path.of("src/main/java/servlet/AdminOrderServlet.java"));
        assertTrue(dao.contains("COALESCE(o.retryScheduledAt, o.deliveryFailedAt) ASC"));
        for (String field : new String[]{"deliveryAttemptCount", "deliveryAttemptLimit", "deliveryFailureCode", "failureNote", "deliveryFailedAt", "retryScheduledAt", "returnedToStoreAt"}) {
            assertTrue(staff.contains("m.put(\"" + field + "\""), field);
            assertTrue(admin.contains("data.put(\"" + field + "\""), field);
        }
    }

    @Test
    void malformedJsonAndAbsentOrdersAreMappedBeforeMutation() throws IOException {
        String shipper = Files.readString(Path.of("src/main/java/servlet/ShipperServlet.java"));
        String staff = Files.readString(Path.of("src/main/java/servlet/StaffOrderServlet.java"));
        assertTrue(shipper.contains("catch (IOException | RuntimeException e)"));
        assertTrue(staff.contains("catch (IOException | RuntimeException e)"));
        assertTrue(staff.indexOf("staffOrderService.getOrderDetail(mutation.orderId())")
                < staff.indexOf("staffOrderService.retryDelivery(mutation.orderId()"));
    }

    @Test
    void publicTrackingHidesInternalFailureDetailAndExposesRetrySchedule() throws IOException {
        String order = Files.readString(Path.of("src/main/java/servlet/OrderServlet.java"));
        String publicTrack = order.substring(order.indexOf("private Map<String, Object> toPublicTrack(Orders o)"),
                order.indexOf("private void handleGuestCheckout"));
        assertTrue(publicTrack.contains("data.put(\"retryScheduledAt\""));
        assertFalse(publicTrack.contains("failureReason"));
        assertFalse(publicTrack.contains("failureCode"));
        assertFalse(publicTrack.contains("failureNote"));
        assertFalse(publicTrack.contains("note"));
    }

    @Test
    void adminListUsesInternalIncidentFieldsWithoutLegacyFailureReason() throws IOException {
        String admin = Files.readString(Path.of("src/main/java/servlet/AdminOrderServlet.java"));
        String listMethod = admin.substring(admin.indexOf("public List<Map<String, Object>> getOrdersData()"), admin.indexOf("protected void doGet"));
        assertFalse(listMethod.contains("m.put(\"failureReason\""));
        for (String field : new String[]{"deliveryAttemptCount", "deliveryAttemptLimit", "deliveryFailureCode", "failureNote", "deliveryFailedAt", "retryScheduledAt", "returnedToStoreAt"}) {
            assertTrue(listMethod.contains("m.put(\"" + field + "\""), field);
        }
    }
}
