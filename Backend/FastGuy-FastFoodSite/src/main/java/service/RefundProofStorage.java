package service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import utils.JsonUtil;

public class RefundProofStorage {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private final HttpClient client;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public RefundProofStorage() {
        this(HttpClient.newHttpClient(), required("CLOUDINARY_CLOUD_NAME"), required("CLOUDINARY_API_KEY"), required("CLOUDINARY_API_SECRET"));
    }

    RefundProofStorage(HttpClient client, String cloudName, String apiKey, String apiSecret) {
        this.client = client; this.cloudName = cloudName; this.apiKey = apiKey; this.apiSecret = apiSecret;
    }

    public record UploadedProof(String publicId, String contentType, Instant uploadedAt) {}
    public record SignedProofUrl(String viewUrl, Instant expiresAt) {}

    public UploadedProof uploadPrivate(byte[] content, String contentType) { return uploadPrivate("proof", content, contentType); }

    public UploadedProof uploadPrivate(String identity, byte[] content, String contentType) {
        validate(content, contentType);
        long timestamp = Instant.now().getEpochSecond();
        String publicId = "fastguy/refunds/" + identity + "-" + sha256(content);
        String signature = sha1("public_id=" + publicId + "&timestamp=" + timestamp + "&type=authenticated" + apiSecret);
        String boundary = "----FastGuy" + UUID.randomUUID();
        byte[] body = multipart(boundary, publicId, timestamp, signature, apiKey, contentType, content);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload"))
                .timeout(Duration.ofSeconds(30)).header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) throw new IllegalStateException("Refund proof upload failed");
            JsonNode json = JsonUtil.getMapper().readTree(response.body());
            String stored = json.path("public_id").asText();
            if (stored.isBlank()) throw new IllegalStateException("Refund proof upload failed");
            return new UploadedProof(stored, contentType, Instant.now());
        } catch (IOException e) {
            throw new IllegalStateException("Refund proof upload failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Refund proof upload interrupted", e);
        }
    }

    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        long timestamp = Instant.now().getEpochSecond();
        String signature = sha1("public_id=" + publicId + "&timestamp=" + timestamp + "&type=authenticated" + apiSecret);
        String form = "public_id=" + encode(publicId) + "&timestamp=" + timestamp + "&type=authenticated&api_key=" + encode(apiKey) + "&signature=" + signature;
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.cloudinary.com/v1_1/" + cloudName + "/image/destroy"))
                .timeout(Duration.ofSeconds(20)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        RuntimeException failure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() / 100 == 2) return;
                failure = new IllegalStateException("Refund proof cleanup failed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); throw new IllegalStateException("Refund proof cleanup interrupted", e);
            } catch (IOException e) { failure = new IllegalStateException("Refund proof cleanup failed", e); }
        }
        throw failure;
    }

    public SignedProofUrl signedViewUrl(String publicId, Duration ttl) { return signedViewUrl(publicId, "image/jpeg", ttl); }

    public SignedProofUrl signedViewUrl(String publicId, String contentType, Duration ttl) {
        if (publicId == null || publicId.isBlank()) throw new IllegalArgumentException("Refund proof is unavailable");
        String format = switch (contentType) { case "image/png" -> "png"; case "image/webp" -> "webp"; default -> "jpg"; };
        Instant expiresAt = Instant.now().plus(ttl);
        long timestamp = Instant.now().getEpochSecond();
        String parameters = "expires_at=" + expiresAt.getEpochSecond() + "&format=" + format + "&public_id=" + publicId + "&timestamp=" + timestamp + "&type=authenticated";
        String signature = sha1(parameters + apiSecret);
        String url = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/download?" + parameters
                + "&api_key=" + encode(apiKey) + "&signature=" + signature;
        return new SignedProofUrl(url, expiresAt);
    }

    public static void validate(byte[] content, String contentType) {
        if (content == null || content.length == 0 || content.length > MAX_BYTES || !TYPES.contains(contentType)) throw new IllegalArgumentException("Invalid refund proof");
        boolean valid = switch (contentType) {
            case "image/jpeg" -> content.length >= 3 && (content[0] & 255) == 0xff && (content[1] & 255) == 0xd8 && (content[2] & 255) == 0xff;
            case "image/png" -> content.length >= 8 && (content[0] & 255) == 0x89 && content[1] == 0x50 && content[2] == 0x4e && content[3] == 0x47 && content[4] == 0x0d && content[5] == 0x0a && content[6] == 0x1a && content[7] == 0x0a;
            case "image/webp" -> content.length >= 12 && new String(content, 0, 4, StandardCharsets.US_ASCII).equals("RIFF") && new String(content, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
            default -> false;
        };
        if (!valid) throw new IllegalArgumentException("Invalid refund proof content");
    }

    private static byte[] multipart(String boundary, String publicId, long timestamp, String signature, String apiKey, String contentType, byte[] content) {
        String fields = part(boundary, "public_id", publicId) + part(boundary, "timestamp", String.valueOf(timestamp))
                + part(boundary, "type", "authenticated") + part(boundary, "api_key", apiKey)
                + part(boundary, "signature", signature)
                + "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"proof\"\r\nContent-Type: " + contentType + "\r\n\r\n";
        byte[] prefix = fields.getBytes(StandardCharsets.UTF_8);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[prefix.length + content.length + suffix.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length); System.arraycopy(content, 0, result, prefix.length, content.length); System.arraycopy(suffix, 0, result, prefix.length + content.length, suffix.length);
        return result;
    }

    private static String part(String boundary, String name, String value) { return "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n"; }
    private static String sha1(String value) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException("Proof signing unavailable", e); } }
    private static String sha256(byte[] value) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)); } catch (Exception e) { throw new IllegalStateException("Proof identity unavailable", e); } }
    private static String encode(String value) { return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String required(String name) { String value = System.getenv(name); if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required"); return value; }
}
