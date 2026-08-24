package service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrderItemCostingTest {
    @Test void recipeCostUsesCurrentIngredientCostsAndYield() {
        BigDecimal cost = OrderItemCostService.recipeUnitCost(new BigDecimal("2"), List.of(
                new OrderItemCostService.CostLine(new BigDecimal("240"), new BigDecimal("180")),
                new OrderItemCostService.CostLine(new BigDecimal("2"), new BigDecimal("3500"))));
        assertEquals(new BigDecimal("25100.0000"), cost);
    }

    @Test void missingIngredientCostKeepsSnapshotUnknown() {
        assertNull(OrderItemCostService.recipeUnitCost(BigDecimal.ONE, List.of(
                new OrderItemCostService.CostLine(new BigDecimal("120"), BigDecimal.ZERO))));
    }

    @Test void discountAllocationPreservesOrderDiscountAfterRounding() {
        List<BigDecimal> allocated = MenuPerformanceReportService.allocateDiscount(
                new BigDecimal("20000.00"),
                List.of(new BigDecimal("120000.00"), new BigDecimal("80000.00")));
        assertEquals(List.of(new BigDecimal("12000.00"), new BigDecimal("8000.00")), allocated);
        assertEquals(new BigDecimal("20000.00"), allocated.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test void tinyDiscountAcrossManyItemsNeverCreatesNegativeAllocation() {
        List<BigDecimal> allocated = MenuPerformanceReportService.allocateDiscount(
                new BigDecimal("0.02"), List.of(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));
        assertEquals(new BigDecimal("0.02"), allocated.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        assertTrue(allocated.stream().allMatch(value -> value.signum() >= 0));
    }

    @Test void reportSeparatesKnownCostFromLegacyRowsWithoutSnapshots() {
        Map<String,Object> result = MenuPerformanceReportService.summarize(List.of(
                MenuPerformanceReportService.row(1, "Burger", "Mặc định", 2, new BigDecimal("138000"), new BigDecimal("10000"), new BigDecimal("76600")),
                MenuPerformanceReportService.row(2, "Wrap", "Mặc định", 1, new BigDecimal("45000"), BigDecimal.ZERO, null)));
        assertEquals(new BigDecimal("173000.00"), result.get("netRevenue"));
        assertEquals(new BigDecimal("76600.00"), result.get("cost"));
        assertEquals(1, result.get("missingCostItemCount"));
        assertEquals(false, result.get("costComplete"));
    }
}
