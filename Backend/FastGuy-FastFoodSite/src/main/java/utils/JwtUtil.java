package utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {
    private static final String SECRET = "FastGuySecretKey2024VeryLongAndSecureEnough12345678";
    private static final long EXPIRATION = 86400000;
    private static final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());

    public static String generate(int userId, String role) {
        return Jwts.builder()
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key)
                .compact();
    }

    public static Claims validate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
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
