package service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AdminCodMetricPolicyTest {
    @Test
    void dashboardUsesSubmittedSettlementsForPendingCodWithoutChangingRevenue() throws IOException {
        String service = Files.readString(Path.of("src/main/java/service/AdminService.java"));
        String dao = Files.readString(Path.of("src/main/java/dao/CodSettlementDAO.java"));
        assertTrue(service.contains("data.put(\"pendingCodAmount\", codSettlementDAO.sumPendingAmount());"));
        assertTrue(service.contains("data.put(\"pendingCodCount\", codSettlementDAO.countPending());"));
        assertTrue(dao.contains("WHERE cs.status = 'SUBMITTED'"));
        assertTrue(service.contains("ordersDAO.sumRevenueDecimal()"));
        assertTrue(service.contains("ordersDAO.sumDeliveredPaidRevenue(operationalStart, operationalEnd)"));
    }
}
