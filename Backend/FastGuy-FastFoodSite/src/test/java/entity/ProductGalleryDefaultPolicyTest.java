package entity;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductGalleryDefaultPolicyTest {

    private static final Path PRODUCT = Path.of("src/main/java/entity/Product.java");

    @Test
    void galleryImagesDefaultsToEmptyJsonArrayOnMissingPayload() throws Exception {
        Product product = new Product();
        assertEquals("[]", product.getGalleryImages());

        String src = Files.readString(PRODUCT);
        assertTrue(src.contains("private String galleryImages = \"[]\";"));
        assertTrue(src.contains("if (galleryImages == null || galleryImages.isBlank()) galleryImages = \"[]\";"));
    }
}
