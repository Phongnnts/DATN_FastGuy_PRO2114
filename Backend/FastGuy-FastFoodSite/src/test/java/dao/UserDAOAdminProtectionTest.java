package dao;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class UserDAOAdminProtectionTest {
    @Test
    void countsOnlyActiveAdmins() throws IOException {
        String source = Files.readString(Path.of("src/main/java/dao/UserDAO.java"));
        assertTrue(source.contains("countActiveAdmins"));
        assertTrue(source.contains("u.role = 'ADMIN' AND u.status = 'ACTIVE'"));
    }

    @Test
    void locksActiveAdminsForProtectedMutation() throws IOException {
        String source = Files.readString(Path.of("src/main/java/dao/UserDAO.java"));
        assertTrue(source.contains("LockModeType.PESSIMISTIC_WRITE"));
    }
}
