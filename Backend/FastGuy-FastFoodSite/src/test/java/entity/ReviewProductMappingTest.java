package entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

class ReviewProductMappingTest {
    @Test
    void reviewMapsRequiredProductRelationship() throws Exception {
        Field product = Review.class.getDeclaredField("product");
        JoinColumn joinColumn = product.getAnnotation(JoinColumn.class);

        assertEquals(Product.class, product.getType());
        assertEquals(ManyToOne.class, product.getAnnotation(ManyToOne.class).annotationType());
        assertFalse(product.getAnnotation(ManyToOne.class).optional());
        assertEquals("product_id", joinColumn.name());
        assertFalse(joinColumn.nullable());

        Method getter = Review.class.getMethod("getProduct");
        Method setter = Review.class.getMethod("setProduct", Product.class);
        assertEquals(Product.class, getter.getReturnType());
        assertEquals(void.class, setter.getReturnType());
    }
}
