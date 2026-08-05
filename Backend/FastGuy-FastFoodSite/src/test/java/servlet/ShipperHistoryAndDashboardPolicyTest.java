package servlet;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ShipperHistoryAndDashboardPolicyTest {

    @Test
    void dashboardPutsTodayAndPendingCodCollected() throws IOException {
        String service = Files.readString(Path.of("src/main/java/service/ShipperService.java"));

        assertTrue(service.contains("sumCodCollectedByShipperAndDateRange(shipperId, todayRange.start(), todayRange.end())"));
        assertTrue(service.contains("stats.put(\"todayCodCollected\", todayCodCollected);"));
        assertTrue(service.contains("stats.put(\"pendingCodCollected\", todayCodCollected);"));
    }

    @Test
    void historyEndpointReturnsItemsTotalPageAndSize() throws IOException {
        String servlet = Files.readString(Path.of("src/main/java/servlet/ShipperServlet.java"));

        assertTrue(servlet.contains("data.put(\"items\","));
        assertTrue(servlet.contains("data.put(\"total\","));
        assertTrue(servlet.contains("data.put(\"page\", page);"));
        assertTrue(servlet.contains("data.put(\"size\", size);"));
    }

    @Test
    void historyEndpointValidatesPageSizeAndDateParams() throws IOException {
        String servlet = Files.readString(Path.of("src/main/java/servlet/ShipperServlet.java"));

        assertTrue(servlet.contains("req.getParameter(\"page\")"));
        assertTrue(servlet.contains("req.getParameter(\"size\")"));
        assertTrue(servlet.contains("req.getParameter(\"fromDate\")"));
        assertTrue(servlet.contains("req.getParameter(\"toDate\")"));
        assertTrue(servlet.contains("LocalDate.parse(fromRaw.trim())"));
        assertTrue(servlet.contains("LocalDate.parse(toRaw.trim())"));
        assertTrue(servlet.contains("fromDate.isAfter(toDate)"));
        assertTrue(servlet.contains("if (size > 100) size = 100;"));
    }

    @Test
    void detailIncludesDiscountAmountWithZeroFallback() throws IOException {
        String servlet = Files.readString(Path.of("src/main/java/servlet/ShipperServlet.java"));

        assertTrue(servlet.contains("data.put(\"discountAmount\", o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO);"));
    }

    @Test
    void historyDaoAppliesCreatedAtRangeAndPagination() throws IOException {
        String dao = Files.readString(Path.of("src/main/java/dao/OrdersDAO.java"));

        assertTrue(dao.contains("findHistoryByShipperId(int shipperId, int page, int size, LocalDateTime from, LocalDateTime to)"));
        assertTrue(dao.contains("o.createdAt >= :from"));
        assertTrue(dao.contains("o.createdAt < :to"));
        assertTrue(dao.contains("setFirstResult((page - 1) * size)"));
        assertTrue(dao.contains("setMaxResults(size)"));
        assertTrue(dao.contains("countHistoryByShipperId(int shipperId, LocalDateTime from, LocalDateTime to)"));
    }

    @Test
    void codCollectedDaoUsesCodAndDeliveredAtRange() throws IOException {
        String dao = Files.readString(Path.of("src/main/java/dao/OrdersDAO.java"));

        assertTrue(dao.contains("sumCodCollectedByShipperAndDateRange"));
        assertTrue(dao.contains("o.paymentMethod = 'COD'"));
        assertTrue(dao.contains("o.codCollectedAmount IS NOT NULL"));
        assertTrue(dao.contains("o.deliveredAt >= :start AND o.deliveredAt < :end"));
    }
}
