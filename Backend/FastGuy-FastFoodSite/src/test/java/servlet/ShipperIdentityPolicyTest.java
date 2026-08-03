package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ShipperIdentityPolicyTest {
    @Test
    void requiresCurrentActiveShipperIdentity() {
        assertTrue(ShipperServlet.isActiveShipper("SHIPPER", "ACTIVE"));
        assertFalse(ShipperServlet.isActiveShipper("SHIPPER", "INACTIVE"));
        assertFalse(ShipperServlet.isActiveShipper("USER", "ACTIVE"));
    }

    @Test
    void shipperListsExposeOperationsContract() throws IOException {
        String servlet = Files.readString(Path.of("src/main/java/servlet/ShipperServlet.java"));
        for (String field : new String[]{"orderId", "orderCode", "status", "customerName", "customerPhone", "customerAddress", "finalAmount", "shippingFee", "paymentMethod", "paymentStatus", "itemCount", "assignedAt", "pickedUpAt", "deliveredAt", "createdAt"}) {
            assertTrue(servlet.contains("m.put(\"" + field + "\""), field);
        }
    }

    @Test
    void historyIncludesOwnedTerminalOrdersInDeterministicOrder() throws IOException {
        String dao = Files.readString(Path.of("src/main/java/dao/OrdersDAO.java"));

        assertTrue(dao.contains("findHistoryByShipperId"));
        assertTrue(dao.contains("o.shipper.userId = :shipperId"));
        assertTrue(dao.contains(".setParameter(\"shipperId\", shipperId)"));
        assertTrue(dao.contains("'DELIVERED','CANCELLED'"));
        assertTrue(dao.contains("COALESCE(o.deliveredAt, o.cancelledAt, o.createdAt) DESC, o.orderId DESC"));
    }

    @Test
    void detailUsesOwnedOrderAndCompleteOperationsContract() throws IOException {
        String servlet = Files.readString(Path.of("src/main/java/servlet/ShipperServlet.java"));

        assertTrue(servlet.contains("getOwnedOrder(detailOrderId, shipperId)"));
        assertTrue(servlet.contains("im.put(\"modifiers\", oi.getModifiers())"));
        assertTrue(servlet.contains("data.put(\"statusHistory\", savedHistory)"));
        assertTrue(servlet.contains("data.put(\"assignedAt\", o.getAssignedAt()"));
        assertTrue(servlet.contains("data.put(\"pickedUpAt\", o.getPickedUpAt()"));
        assertTrue(servlet.contains("data.put(\"deliveredAt\", o.getDeliveredAt()"));
        assertTrue(servlet.contains("data.put(\"allowedActions\", ShipperService.getAllowedActions("));
    }

    @Test
    void parsesOnlyExactDetailPath() {
        assertEquals(12, ShipperServlet.parseDetailOrderId("/orders/12"));
        assertNull(ShipperServlet.parseDetailOrderId("/orders/"));
        assertNull(ShipperServlet.parseDetailOrderId("/orders/12/"));
        assertNull(ShipperServlet.parseDetailOrderId("/orders/12/extra"));
        assertNull(ShipperServlet.parseDetailOrderId("/orders/x"));
    }

    @Test
    void parsesOnlyExactMutationPaths() {
        assertEquals("pickup", ShipperServlet.parseMutationPath("/orders/12/pickup").action());
        assertEquals(12, ShipperServlet.parseMutationPath("/orders/12/deliver").orderId());
        assertNull(ShipperServlet.parseMutationPath("/orders/12"));
        assertNull(ShipperServlet.parseMutationPath("/orders/12/pickup/extra"));
        assertNull(ShipperServlet.parseMutationPath("/orders/12/cancel"));
        assertNull(ShipperServlet.parseMutationPath("/orders//pickup"));
    }

    @Test
    void mutationRevalidatesShiftInsideCanonicalTransitionTransaction() throws IOException {
        String transition = Files.readString(Path.of("src/main/java/service/OrderTransitionService.java"));
        String shipper = Files.readString(Path.of("src/main/java/service/ShipperService.java"));

        assertTrue(transition.contains("requireCheckedInShipper(em, actorUserId)"));
        assertTrue(transition.indexOf("requireCheckedInShipper(em, actorUserId)") > transition.indexOf("PESSIMISTIC_WRITE"));
        assertFalse(shipper.contains("DatabaseUtil.getEntityManager()"));
    }

    @Test
    void dashboardDeliveredBoundaryUsesDeliveredAtAndHalfOpenRange() {
        var start = java.time.LocalDateTime.of(2026, 8, 2, 0, 0);
        var end = start.plusDays(1);
        assertTrue(service.ShipperDashboardBoundary.isTodayDelivery(start, start, start.minusYears(1), end));
        assertTrue(service.ShipperDashboardBoundary.isTodayDelivery(end.minusNanos(1), start, end.plusYears(1), end));
        assertFalse(service.ShipperDashboardBoundary.isTodayDelivery(start.minusNanos(1), start, start, end));
        assertFalse(service.ShipperDashboardBoundary.isTodayDelivery(end, start, start, end));
    }

    @Test
    void dashboardCountsTodayDeliveredByHalfOpenDeliveredAtRange() throws IOException {
        String dao = Files.readString(Path.of("src/main/java/dao/OrdersDAO.java"));
        String service = Files.readString(Path.of("src/main/java/service/ShipperService.java"));

        assertTrue(dao.contains("countDeliveredByShipperAndDateRange"));
        assertTrue(dao.contains("o.deliveredAt >= :start AND o.deliveredAt < :end"));
        assertTrue(service.contains("ShipperDashboardBoundary.forDate(today)"));
        assertTrue(service.contains("countDeliveredByShipperAndDateRange(shipperId, todayRange.start(), todayRange.end())"));
    }
}
