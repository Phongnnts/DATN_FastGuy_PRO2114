package dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import entity.Product;

class ProductDAOPolicyTest {
    @Test
    void deletingProductOnlyMakesItUnavailable() {
        Product product = new Product();
        product.setStatus("AVAILABLE");

        ProductDAO.markDeleted(product);

        assertEquals("UNAVAILABLE", product.getStatus());
    }
}
