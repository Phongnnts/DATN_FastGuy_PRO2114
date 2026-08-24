package servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import entity.ProductVariant;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdminProductVariantSerializationTest {
    @Test void adminVariantDetailSerializesInventoryModeAndUpdatedAt() throws Exception {
        ProductVariant variant = new ProductVariant();
        variant.setVariantId(12);
        variant.setInventoryMode("FINISHED_GOOD");
        variant.setUpdatedAt(LocalDateTime.parse("2026-08-24T10:11:12"));
        Method method = AdminProductServlet.class.getDeclaredMethod("toVariantMap", ProductVariant.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String,Object> result = (Map<String,Object>) method.invoke(new AdminProductServlet(), variant);

        assertEquals("FINISHED_GOOD", result.get("inventoryMode"));
        assertEquals(LocalDateTime.parse("2026-08-24T10:11:12"), result.get("updatedAt"));
    }
}
