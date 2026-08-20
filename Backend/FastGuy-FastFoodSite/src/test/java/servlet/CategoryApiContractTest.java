package servlet;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CategoryApiContractTest {
    @Test
    void servletSerializesEveryFieldRequiredByOpenApi() throws Exception {
        String servlet = Files.readString(Path.of("src/main/java/servlet/CategoryServlet.java"));
        String contract = Files.readString(Path.of("../../openapi/fastguy.yaml"));

        assertTrue(servlet.contains("@WebServlet(\"/api/categories\")"));
        assertTrue(servlet.contains("ApiResponse.ok(resp, categories)"));
        for (String field : new String[] {"categoryId", "name", "description", "imageUrl", "sortOrder", "productCount"}) {
            assertTrue(servlet.contains("m.put(\"" + field + "\""), field);
            assertTrue(contract.contains("        " + field + ":"), field);
        }
    }

    @Test
    void migrationAndAdminCategorySupportManagedImages() throws Exception {
        String migration = Files.readString(Path.of("../../database/migrations/049_category_images.sql"));
        String validator = Files.readString(Path.of("../../database/migrations/049_validate.sql"));
        String admin = Files.readString(Path.of("src/main/java/servlet/AdminCategoryServlet.java"));
        String entity = Files.readString(Path.of("src/main/java/entity/Category.java"));

        assertTrue(migration.contains("migration_id = '048_homepage_merchandising'"));
        assertTrue(migration.contains("ALTER TABLE dbo.Category ADD image_url nvarchar(1000) NULL"));
        assertTrue(validator.contains("049_category_images"));
        assertTrue(entity.contains("@Column(name = \"image_url\")"));
        assertTrue(admin.contains("result.put(\"imageUrl\""));
        assertTrue(admin.contains("body.containsKey(\"imageUrl\")"));
        assertTrue(admin.contains("URI.create"));
    }
}
