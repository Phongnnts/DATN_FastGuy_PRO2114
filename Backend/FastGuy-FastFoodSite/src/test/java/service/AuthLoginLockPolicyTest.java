package service;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthLoginLockPolicyTest {

    private static final Path AUTH = Path.of("src/main/java/service/AuthService.java");
    private static final Path SERVLET = Path.of("src/main/java/servlet/AuthServlet.java");

    private static String read(Path p) throws Exception {
        return Files.readString(p);
    }

    @Test
    void lockPolicyDefinesAttemptThresholdAndDuration() throws Exception {
        String src = read(AUTH);
        assertTrue(src.contains("MAX_FAILED_ATTEMPTS = 5"));
        assertTrue(src.contains("LOCK_DURATION_MINUTES = 15"));
    }

    @Test
    void lockedAccountIsRejectedAndWrongPasswordCountsAttempts() throws Exception {
        String src = read(AUTH);
        assertTrue(src.contains("throw new IllegalStateException(\"Tài khoản đã bị khóa tạm thời, vui lòng thử lại sau\")"));
        assertTrue(src.contains("user.getFailedLoginAttempts() + 1"));
        assertTrue(src.contains("attempts >= MAX_FAILED_ATTEMPTS"));
        assertTrue(src.contains("setLockedUntil(now.plusMinutes(LOCK_DURATION_MINUTES))"));
    }

    @Test
    void successfulLoginResetsAttemptsAndLock() throws Exception {
        String src = read(AUTH);
        assertTrue(src.contains("setFailedLoginAttempts(0)"));
        assertTrue(src.contains("setLockedUntil(null)"));
    }

    @Test
    void loginUsesPessimisticLockOnUser() throws Exception {
        String src = read(AUTH);
        assertTrue(src.contains("LockModeType.PESSIMISTIC_WRITE"));
        assertTrue(src.contains("em.find(User.class, found.getUserId(), LockModeType.PESSIMISTIC_WRITE)"));
    }

    @Test
    void servletSurfacesLockedMessage() throws Exception {
        String src = read(SERVLET);
        assertTrue(src.contains("catch (IllegalStateException e)"));
        assertTrue(src.contains("ApiResponse.error(e.getMessage())"));
    }
}
