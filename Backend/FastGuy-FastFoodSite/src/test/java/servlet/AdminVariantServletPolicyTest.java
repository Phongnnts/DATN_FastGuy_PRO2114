package servlet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AdminVariantServletPolicyTest {
    @Test
    void rejectsStockFieldOnPersistedVariantUpdate() {
        Map<String, Object> body = new HashMap<>();
        body.put("quantityAvailable", 12);

        assertTrue(AdminVariantServlet.containsForbiddenStockUpdate(body));
        assertFalse(AdminVariantServlet.containsForbiddenStockUpdate(Map.of("price", 25000)));
    }
}
