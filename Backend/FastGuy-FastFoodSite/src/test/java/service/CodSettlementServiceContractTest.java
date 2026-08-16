package service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CodSettlementServiceContractTest {
    @Test void submitUsesShiftLockUniqueLookupAndRollback() throws IOException {
        String service = Files.readString(Path.of("src/main/java/service/CodSettlementService.java"));
        assertTrue(service.contains("findOwnedShiftForUpdate(em, shiftId, shipperId, now)"));
        assertTrue(service.contains("findByShipperAndShift(em, shipperId, shiftId)"));
        assertTrue(service.contains("throw new SettlementConflictException(\"Ca này đã gửi bàn giao COD\")"));
        assertTrue(service.contains("if (em.getTransaction().isActive()) em.getTransaction().rollback();"));
    }

    @Test void verifyLocksSettlementAndProtectsExpectedStatus() throws IOException {
        String service = Files.readString(Path.of("src/main/java/service/CodSettlementService.java"));
        assertTrue(service.contains("findForUpdate(em, settlementId)"));
        assertTrue(service.contains("!settlement.getStatus().equals(expectedStatus)"));
        assertTrue(service.contains("!\"SUBMITTED\".equals(settlement.getStatus())"));
        assertTrue(service.contains("settlement.setReceivedBy(admin);"));
    }

    @Test void daoScopesExpectedCodToDeliveredOrdersAndShiftWindow() throws IOException {
        String dao = Files.readString(Path.of("src/main/java/dao/CodSettlementDAO.java"));
        assertTrue(dao.contains("o.shipper.userId = :shipperId"));
        assertTrue(dao.contains("o.paymentMethod = 'COD'"));
        assertTrue(dao.contains("o.orderStatus = 'DELIVERED'"));
        assertTrue(dao.contains("o.codCollectedAmount IS NOT NULL"));
        assertTrue(dao.contains("o.deliveredAt >= :start AND o.deliveredAt < :end"));
        assertTrue(dao.contains("LockModeType.PESSIMISTIC_WRITE"));
    }
}
