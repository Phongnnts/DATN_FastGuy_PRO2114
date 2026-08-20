package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class UserAvatarPolicyTest {
    @Test
    void acceptsHttpsOrNullAndRejectsUnsafeAvatarUrls() {
        assertNull(UserAvatarPolicy.normalize(null));
        assertNull(UserAvatarPolicy.normalize("   "));
        assertEquals("https://res.cloudinary.com/demo/avatar.jpg", UserAvatarPolicy.normalize("  https://res.cloudinary.com/demo/avatar.jpg  "));
        assertEquals("URL ảnh đại diện phải dùng HTTPS", UserAvatarPolicy.validationError("http://example.com/avatar.jpg"));
        assertEquals("URL ảnh đại diện quá dài", UserAvatarPolicy.validationError("https://example.com/" + "a".repeat(490)));
    }

    @Test
    void profileAndAdminMutationsPersistAndSerializeAvatarUrl() throws Exception {
        String auth = Files.readString(Path.of("src/main/java/servlet/AuthServlet.java"));
        String service = Files.readString(Path.of("src/main/java/service/AuthService.java"));
        String admin = Files.readString(Path.of("src/main/java/servlet/AdminUserServlet.java"));
        assertTrue(auth.contains("body.containsKey(\"avatarUrl\")"));
        assertEquals(2, auth.split("toProfileMap\\(user\\)", -1).length - 1);
        assertTrue(service.contains("user.setAvatarUrl((String) data.get(\"avatarUrl\"))"));
        assertTrue(admin.contains("result.put(\"avatarUrl\", user.getAvatarUrl())"));
        assertTrue(admin.contains("user.setAvatarUrl(UserAvatarPolicy.normalize(raw))"));
    }
}
