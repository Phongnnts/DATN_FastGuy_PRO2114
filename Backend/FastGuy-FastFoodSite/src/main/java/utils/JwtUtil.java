package utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {
    private static final long EXPIRATION = 86400000;
    private static final SecretKey KEY;

    static {
        String secret = AppConfig.getJwtSecret();
        if (secret.isEmpty()) {
            // Dev-only: generate ephemeral key for local development
            KEY = Keys.hmacShaKeyFor("dev-only-ephemeral-key-not-for-production-32bytes!".getBytes(StandardCharsets.UTF_8));
        } else {
            KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static String generate(int userId, String role) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(KEY)
                .compact();
    }

    public static Claims validate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    public static int getUserId(String token) {
        Claims claims = validate(token);
        return claims != null ? claims.get("userId", Integer.class) : -1;
    }

    public static String getRole(String token) {
        Claims claims = validate(token);
        return claims != null ? claims.get("role", String.class) : null;
    }
}
