package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;

import service.StoreConfigService;

class AdminSettingsServletPolicyTest {
    @Test
    void revalidatesAdminRoleAgainstDatabase() throws IOException {
        String servlet = Files.readString(Path.of("src/main/java/servlet/AdminSettingsServlet.java"));
        assertTrue(servlet.contains("PrivilegedAuth.isActiveRole(JwtUtil.getUserId(token), \"ADMIN\")"));
        assertTrue(servlet.contains("\"ADMIN\".equals(JwtUtil.getRole(token))"));
    }

    @Test
    void rejectsMissingTokenWith401AndNonAdminWith403() throws IOException {
        String servlet = Files.readString(Path.of("src/main/java/servlet/AdminSettingsServlet.java"));
        assertTrue(servlet.contains("\"Missing token\", 401"));
        assertTrue(servlet.contains("\"Forbidden\", 403"));
        assertTrue(servlet.contains("header.startsWith(\"Bearer \")"));
    }

    @Test
    void ghnKeysExposeExpectedShippingConfigKeys() {
        assertEquals(Set.of("ghn_from_district_id", "ghn_from_ward_code", "default_service_type_id", "default_weight", "default_length", "default_width", "default_height"), StoreConfigService.GHN_KEYS);
    }
}
