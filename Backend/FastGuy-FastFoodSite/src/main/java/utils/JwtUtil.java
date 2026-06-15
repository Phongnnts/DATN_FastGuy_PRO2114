package utils;

import config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {
    private static final SecretKey key = Keys.hmacShaKeyFor(
            JwtConfig.SECRET_KEY.getBytes(StandardCharsets.UTF_8)
    );

    public static String generateToken(Long userId, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(JwtConfig.ISSUER)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + JwtConfig.EXPIRATION_MS))
                .signWith(key)
                .compact();
    }

    private static Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(JwtConfig.ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static Long getUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public static String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public static boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
