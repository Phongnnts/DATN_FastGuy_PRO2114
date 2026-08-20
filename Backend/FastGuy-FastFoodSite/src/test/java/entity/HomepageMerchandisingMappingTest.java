package entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;

class HomepageMerchandisingMappingTest {
    @Test
    void entitiesMapHomepageColumnsWithSafeDefaults() throws Exception {
        Product product = new Product();
        ProductCombo combo = new ProductCombo();
        Review review = new Review();

        assertEquals("is_new", column(Product.class, "isNew"));
        assertEquals("spice_level", column(Product.class, "spiceLevel"));
        assertEquals(false, product.getIsNew());
        assertEquals(0, product.getSpiceLevel());
        assertEquals("homepage_occasion", column(ProductCombo.class, "homepageOccasion"));
        assertEquals("homepage_sort_order", column(ProductCombo.class, "homepageSortOrder"));
        assertEquals(0, combo.getHomepageSortOrder());
        assertEquals("is_featured", column(Review.class, "featured"));
        assertEquals(false, review.getFeatured());
        assertEquals("homepage_consent", column(Review.class, "homepageConsent"));
        assertEquals(false, review.getHomepageConsent());
    }

    private String column(Class<?> type, String fieldName) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        return field.getAnnotation(Column.class).name();
    }
}
