package config;

public class JwtConfig {
    public static final String SECRET_KEY = "FastGuySecretKey2026ForJWTTokenGenerationAtLeast256BitsLong";
    public static final long EXPIRATION_MS = 24 * 60 * 60 * 1000;
    public static final String ISSUER = "FastGuy";
}
