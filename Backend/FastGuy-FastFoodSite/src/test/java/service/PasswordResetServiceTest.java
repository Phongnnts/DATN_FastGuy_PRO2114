package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class PasswordResetServiceTest {
    @Test
    void resetTokenIsHashedAndExpiresAfterFifteenMinutes() {
        String token = PasswordResetService.newToken();
        String tokenHash = PasswordResetService.hashToken(token);

        assertNotEquals(token, tokenHash);
        assertEquals(tokenHash, PasswordResetService.hashToken(token));
        assertTrue(PasswordResetService.expiresAt().isAfter(LocalDateTime.now().plusMinutes(14)));
        assertTrue(PasswordResetService.expiresAt().isBefore(LocalDateTime.now().plusMinutes(16)));
    }
}
