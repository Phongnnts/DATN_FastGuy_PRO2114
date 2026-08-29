package service;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import dao.OperatingFinanceDAO;
import entity.FixedAsset;

class OperatingFinanceServiceTest {
    @Test void depreciationUsesActualCalendarDaysAndInclusiveOverlap() {
        FixedAsset asset=asset(new BigDecimal("1200.00"),new BigDecimal("0.00"),LocalDate.of(2024,1,1),2,null);
        assertEquals(new BigDecimal("580.00"),OperatingFinanceService.depreciation(asset,LocalDate.of(2024,2,1),LocalDate.of(2024,3,31)));
        assertEquals(new BigDecimal("20.00"),OperatingFinanceService.depreciation(asset,LocalDate.of(2024,2,1),LocalDate.of(2024,2,1)));
    }

    @Test void depreciationStopsAtRetirementDateInclusively() {
        FixedAsset asset=asset(new BigDecimal("366.00"),BigDecimal.ZERO,LocalDate.of(2024,1,1),12,LocalDateTime.of(2024,1,10,12,0));
        assertEquals(new BigDecimal("10.00"),OperatingFinanceService.depreciation(asset,LocalDate.of(2024,1,1),LocalDate.of(2024,1,31)));
    }

    @Test void operatingProfitKeepsLargeFractionalValuesInDecimalArithmetic() {
        OperatingFinanceDAO dao=new StubDAO(){public BigDecimal grossRevenue(LocalDate from,LocalDate to){return new BigDecimal("9007199254740992.005");}public BigDecimal refundTotal(LocalDate from,LocalDate to){return new BigDecimal("0.004");}};
        MenuPerformanceReportService menu=new MenuPerformanceReportService(){public Map<String,Object> report(LocalDate from,LocalDate to){return new LinkedHashMap<>(Map.of("cost",new BigDecimal("0.01"),"costComplete",true,"missingCostItemCount",0));}};
        Map<String,Object> report=new OperatingFinanceService(dao,menu).operatingProfit(LocalDate.of(2024,1,1),LocalDate.of(2024,1,1));
        assertEquals(new BigDecimal("9007199254740992.01"),report.get("grossRevenue"));
        assertEquals(new BigDecimal("0.00"),report.get("refundTotal"));
        assertEquals(new BigDecimal("9007199254740977.00"),report.get("operatingProfit"));
        assertEquals(new BigDecimal("15.00"),report.get("storeExpenses"));
        assertEquals(new BigDecimal("9007199254740977.00"),report.get("estimatedOperatingResult"));
        assertEquals(true,report.get("includesManualSalary"));
    }

    @Test void operatingFinanceOrderTotalsUseBigDecimalQueries() throws Exception {
        assertEquals(BigDecimal.class,dao.OrdersDAO.class.getMethod("sumRevenueDecimalByDateRange",LocalDateTime.class,LocalDateTime.class).getReturnType());
        assertEquals(BigDecimal.class,dao.OrdersDAO.class.getMethod("sumRefundsDecimalInRange",LocalDateTime.class,LocalDateTime.class).getReturnType());
    }

    @Test void depreciationRoundsHalfUpDeterministically() {
        FixedAsset asset=asset(new BigDecimal("1.00"),BigDecimal.ZERO,LocalDate.of(2024,1,1),1,null);
        assertEquals(new BigDecimal("0.03"),OperatingFinanceService.depreciation(asset,LocalDate.of(2024,1,1),LocalDate.of(2024,1,1)));
    }

    @Test void incompleteCostNullsAllCostDependentProfitFields() {
        OperatingFinanceDAO dao=new StubDAO();
        MenuPerformanceReportService menu=new MenuPerformanceReportService(){public Map<String,Object> report(LocalDate from,LocalDate to){return new LinkedHashMap<>(Map.of("grossRevenue",new BigDecimal("100.00"),"allocatedDiscount",new BigDecimal("10.00"),"netRevenue",new BigDecimal("90.00"),"cost",new BigDecimal("20.00"),"costComplete",false,"missingCostItemCount",2));}};
        Map<String,Object> report=new OperatingFinanceService(dao,menu).operatingProfit(LocalDate.of(2024,1,1),LocalDate.of(2024,1,31));
        assertEquals(new BigDecimal("100.00"),report.get("grossRevenue"));
        assertEquals(new BigDecimal("10.00"),report.get("refundTotal"));
        assertEquals(new BigDecimal("90.00"),report.get("netRevenue"));
        assertNull(report.get("cogs"));assertNull(report.get("grossProfit"));assertNull(report.get("profitBeforeDepreciation"));assertNull(report.get("operatingProfit"));assertNull(report.get("estimatedOperatingResult"));
        assertEquals(new BigDecimal("15.00"),report.get("operatingExpenses"));assertEquals(new BigDecimal("15.00"),report.get("storeExpenses"));assertEquals(true,report.get("includesManualSalary"));assertEquals(2,report.get("missingCostItemCount"));
        assertEquals(Set.of("grossRevenue","refundTotal","netRevenue","cogs","grossProfit","operatingExpenses","storeExpenses","profitBeforeDepreciation","estimatedOperatingResult","depreciation","operatingProfit","includesManualSalary","costComplete","missingCostItemCount","fromDate","toDate"),report.keySet());
        String json=assertDoesNotThrow(()->utils.JsonUtil.getMapper().writeValueAsString(report));
        assertTrue(json.contains("\"estimatedOperatingResult\":null"));
        assertTrue(json.contains("\"includesManualSalary\":true"));
    }

    private static FixedAsset asset(BigDecimal cost,BigDecimal salvage,LocalDate start,int months,LocalDateTime retired){FixedAsset a=new FixedAsset();a.setAcquisitionCost(cost);a.setSalvageValue(salvage);a.setDepreciationStartDate(start);a.setUsefulLifeMonths(months);a.setStatus(retired==null?FixedAsset.Status.ACTIVE:FixedAsset.Status.RETIRED);a.setRetiredAt(retired);return a;}
    private static class StubDAO extends OperatingFinanceDAO {public BigDecimal grossRevenue(LocalDate from,LocalDate to){return new BigDecimal("100.00");}public BigDecimal refundTotal(LocalDate from,LocalDate to){return new BigDecimal("10.00");}public BigDecimal sumExpenses(LocalDate from,LocalDate to){return new BigDecimal("15.00");}public List<FixedAsset> listAssetsForDepreciation(LocalDate to){return List.of();}}
}
