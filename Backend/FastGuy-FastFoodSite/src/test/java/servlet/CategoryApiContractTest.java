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
        for (String field : new String[] {"categoryId", "name", "description", "sortOrder", "productCount"}) {
            assertTrue(servlet.contains("m.put(\"" + field + "\""), field);
            assertTrue(contract.contains("        " + field + ":"), field);
        }
    }
}
