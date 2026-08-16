package service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AdminDashboardStockPolicyTest {
    @Test
    void dashboardPublishesSharedSkuStockCounts() throws IOException {
        String src = Files.readString(Path.of("src/main/java/service/AdminService.java"));
        assertTrue(src.contains("storeConfigService.getLowStockThreshold()"));
        assertTrue(src.contains("productDAO.countStockRiskSkus(lowStockThreshold)"));
        assertTrue(src.contains("stockRiskCounts[0]"));
        assertTrue(src.contains("stockRiskCounts[1]"));
        assertTrue(src.contains("data.put(\"lowStockThreshold\", lowStockThreshold)"));
        assertTrue(src.contains("data.put(\"outOfStockSkuCount\""));
        assertTrue(src.contains("data.put(\"lowStockSkuCount\""));
    }
}
