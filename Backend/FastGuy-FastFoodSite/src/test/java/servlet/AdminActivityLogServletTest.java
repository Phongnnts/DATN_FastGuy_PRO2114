package servlet;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AdminActivityLogServletTest {
    @Test
    void parsesUiAndOpenApiDateTimesAsUtcAndKeepsMissingIntegerNullable() {
        assertEquals(LocalDateTime.parse("2026-08-30T00:00:00"), AdminActivityLogServlet.date("2026-08-30T00:00:00"));
        assertEquals(LocalDateTime.parse("2026-08-30T00:00:00"), AdminActivityLogServlet.date("2026-08-30T00:00:00Z"));
        assertEquals(LocalDateTime.parse("2026-08-30T00:00:00"), AdminActivityLogServlet.date("2026-08-30T07:00:00+07:00"));
        assertNull(AdminActivityLogServlet.integer(null, null));
    }
}
