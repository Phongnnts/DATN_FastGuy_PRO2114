package servlet;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class AdminActiveRolePolicyTest {
    @Test
    void everyAdminServletAuthorizationRequiresActiveAdminRole() throws IOException {
        Path servletDirectory = Path.of("src/main/java/servlet");
        List<Path> adminServlets;
        try (Stream<Path> paths = Files.list(servletDirectory)) {
            adminServlets = paths
                    .filter(path -> path.getFileName().toString().matches("Admin.*Servlet\\.java"))
                    .filter(path -> {
                        try {
                            return Files.readString(path).contains("\"ADMIN\".equals");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
        }

        for (Path servlet : adminServlets) {
            String source = Files.readString(servlet);
            assertTrue(source.contains("PrivilegedAuth.isActiveRole("), servlet.getFileName() + " must revalidate active ADMIN role");
            assertTrue(source.contains("\"Missing token\", 401"), servlet.getFileName() + " must return 401 for missing or malformed bearer token");
            assertTrue(source.contains("\"Forbidden\", 403"), servlet.getFileName() + " must return 403 for inactive or wrong role");
        }
    }
}
