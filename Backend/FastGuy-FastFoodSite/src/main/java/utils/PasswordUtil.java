package utils;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordUtil {
    private static final String PREFIX = "pbkdf2$";
    private static final int ITERATIONS = 120000;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password is required");
        }
        try {
            byte[] salt = new byte[16];
            RANDOM.nextBytes(salt);
            byte[] encoded = pbkdf2(password.toCharArray(), salt);
            return PREFIX + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(encoded);
        } catch (Exception e) {
            throw new RuntimeException("Cannot hash password", e);
        }
    }

    public static boolean check(String password, String stored) {
        if (password == null || stored == null) return false;
        if (!isHashed(stored)) return password.equals(stored);
        try {
            String[] parts = stored.split("\\$");
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(password.toCharArray(), salt, iterations);
            return constantTimeEquals(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt) throws Exception {
        return pbkdf2(password, salt, ITERATIONS);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) throws Exception {
        KeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) result |= a[i] ^ b[i];
        return result == 0;
    }
}
