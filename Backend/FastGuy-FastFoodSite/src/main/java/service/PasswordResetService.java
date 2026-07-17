package service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import dao.PasswordResetTokenDAO;
import dao.UserDAO;
import entity.PasswordResetToken;
import entity.User;
import utils.PasswordUtil;

public class PasswordResetService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserDAO userDAO = new UserDAO();
    private final PasswordResetTokenDAO tokenDAO = new PasswordResetTokenDAO();
    private final MailService mailService = new MailService();

    public void request(String email) {
        User user = userDAO.findByEmail(email);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank() || !"ACTIVE".equals(user.getStatus())) return;
        String token = newToken();
        PasswordResetToken reset = new PasswordResetToken();
        reset.setUser(user);
        reset.setTokenHash(hashToken(token));
        reset.setExpiresAt(expiresAt());
        reset.setCreatedAt(LocalDateTime.now());
        tokenDAO.replaceForUser(reset);
        mailService.sendPasswordReset(user.getEmail(), token);
    }

    public void reset(String token, String password) {
        if (!AuthService.isStrongPassword(password)) {
            throw new IllegalArgumentException("Mật khẩu mới phải từ 8 ký tự, có ít nhất 1 chữ và 1 số");
        }
        PasswordResetToken reset = tokenDAO.findUsableByHash(hashToken(token));
        if (reset == null) throw new IllegalArgumentException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
        tokenDAO.consumeAndUpdatePassword(reset.getResetTokenId(), PasswordUtil.hash(password));
    }

    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo token đặt lại mật khẩu", e);
        }
    }

    public static LocalDateTime expiresAt() {
        return LocalDateTime.now().plusMinutes(15);
    }
}
