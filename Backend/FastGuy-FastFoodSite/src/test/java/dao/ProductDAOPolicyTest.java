package dao;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import entity.Product;

class ProductDAOPolicyTest {
    @Test
    void deletingProductOnlyMakesItUnavailable() {
        Product product = new Product();
        product.setStatus("AVAILABLE");

        ProductDAO.markDeleted(product);

        assertEquals("UNAVAILABLE", product.getStatus());
    }

    @Test
    void emptyAggregateMapsBothStockRiskCountsToZero() {
        assertArrayEquals(new long[]{0, 0}, ProductDAO.stockRiskCounts(new Object[]{null, null}));
    }

    @Test
    void aggregateMapsNumericStockRiskCounts() {
        assertArrayEquals(new long[]{2, 3}, ProductDAO.stockRiskCounts(new Object[]{2L, 3L}));
    }

    @Test
    void dashboardStockCountsUseSkuBoundaryAndExcludeUnlimitedStock() throws IOException {
        String src = Files.readString(Path.of("src/main/java/dao/ProductDAO.java"));
        assertTrue(src.contains("public long[] countStockRiskSkus(int threshold)"));
        assertTrue(src.contains("SUM(CASE WHEN v.quantityAvailable <= 0 THEN 1 ELSE 0 END)"));
        assertTrue(src.contains("SUM(CASE WHEN v.quantityAvailable > 0 AND v.quantityAvailable <= :threshold THEN 1 ELSE 0 END)"));
        assertEquals(1, src.split("FROM ProductVariant v\"", -1).length - 1);
    }
}
