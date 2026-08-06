package service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class StaffDashboardPolicyTest {
    @Test
    void dashboardUsesCurrentShiftWindowForOperationalMetrics() throws IOException {
        String serviceSrc = Files.readString(Path.of("src/main/java/service/StaffService.java"));
        String servletSrc = Files.readString(Path.of("src/main/java/servlet/StaffDashboardServlet.java"));
        String daoSrc = Files.readString(Path.of("src/main/java/dao/OrdersDAO.java"));

        assertTrue(servletSrc.contains("staffService.getDashboard(staffId)"));
        assertTrue(serviceSrc.contains("currentCheckedInShift(staffId)"));
        assertTrue(serviceSrc.contains("shiftCompletedOrders"));
        assertTrue(serviceSrc.contains("shiftFailedOrders"));
        assertTrue(serviceSrc.contains("shiftNetRevenue"));
        assertTrue(serviceSrc.contains("priorityOrders"));
        assertTrue(daoSrc.contains("COALESCE(o.finalAmount, 0) - COALESCE(o.refundAmount, 0)"));
        assertTrue(daoSrc.contains(".setMaxResults(6)"));
    }
}
