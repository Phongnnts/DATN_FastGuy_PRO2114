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

    @Test
    void dashboardPublishesSharedSkuStockAlerts() throws IOException {
        String src = Files.readString(Path.of("src/main/java/service/StaffService.java"));
        assertTrue(src.contains("storeConfigService.getLowStockThreshold()"));
        assertTrue(src.contains("productDAO.countStockRiskSkus(lowStockThreshold)"));
        assertTrue(src.contains("stockRiskCounts[0]"));
        assertTrue(src.contains("stockRiskCounts[1]"));
        assertTrue(src.contains("data.put(\"lowStockThreshold\", lowStockThreshold)"));
        assertTrue(src.contains("data.put(\"outOfStockSkuCount\""));
        assertTrue(src.contains("data.put(\"lowStockSkuCount\""));
    }
}
