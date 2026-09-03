package servlet;

final class UserAvatarPolicy {

    private static final int MAX_LENGTH = 500;

    private UserAvatarPolicy() {}

    static String normalize(Object raw) {
        if (raw == null) return null;
        if (!(raw instanceof String value)) throw new IllegalArgumentException(
            "URL ảnh đại diện không hợp lệ"
        );
        String normalized = value.trim();
        if (normalized.isEmpty()) return null;
        String error = validationError(normalized);
        if (error != null) throw new IllegalArgumentException(error);
        return normalized;
    }

    static String validationError(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.length() > MAX_LENGTH) return "URL ảnh đại diện quá dài";
        if (
            !value.regionMatches(true, 0, "https://", 0, 8)
        ) return "URL ảnh đại diện phải dùng HTTPS";
        return null;
    }
}
