package servlet;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class StaffOrderHistoryPolicyTest {
    @Test
    void historyUsesValidatedServerSideFiltersAndPagination() throws IOException {
        String servlet = Files.readString(Path.of("src/main/java/servlet/StaffOrderServlet.java"));
        String dao = Files.readString(Path.of("src/main/java/dao/OrdersDAO.java"));

        assertTrue(servlet.contains("getHistoryFilter(req)"));
        assertTrue(servlet.contains("totalPages"));
        assertTrue(servlet.contains("pageSize must be between 1 and 100"));
        assertTrue(dao.contains("findStaffHistory("));
        assertTrue(dao.contains("countStaffHistory("));
        assertTrue(dao.contains("COALESCE(o.deliveredAt, o.cancelledAt, o.createdAt)"));
        assertTrue(dao.contains("LOWER(o.orderCode) LIKE :search"));
        assertTrue(dao.contains("LOWER(o.customerPhone) LIKE :search"));
        assertTrue(dao.contains("setFirstResult((page - 1) * size)"));
    }

    @Test
    void exportUsesSameFilterAndEscapesSpreadsheetValues() throws IOException {
        String servlet = Files.readString(Path.of("src/main/java/servlet/StaffOrderServlet.java"));

        assertTrue(servlet.contains("csvCell("));
        assertTrue(servlet.contains("startsWith(\"=\")"));
        assertTrue(servlet.contains("startsWith(\"+\")"));
        assertTrue(servlet.contains("startsWith(\"-\")"));
        assertTrue(servlet.contains("startsWith(\"@\")"));
        assertTrue(servlet.contains("\\uFEFF"));
        assertTrue(servlet.contains("findStaffHistoryForExport"));
    }
}
