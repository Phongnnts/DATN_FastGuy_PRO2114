package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import dao.ProductDAO;
import entity.Category;
import entity.Product;
import entity.ProductVariant;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

class ProductLifecycleIT {
    @Test
    void disposableDatabaseRestoresDeletesUnusedAndBlocksUsedProducts() {
        EntityManager em = DatabaseUtil.getEntityManager();
        int productId;
        int usedProductId;
        try {
            assertTrue(((String) em.createNativeQuery("SELECT DB_NAME()").getSingleResult()).endsWith("_Test"));
            em.getTransaction().begin();
            Category category = em.createQuery("SELECT c FROM Category c ORDER BY c.categoryId", Category.class).setMaxResults(1).getSingleResult();
            Product product = new Product();
            product.setCategory(category);
            product.setName("Lifecycle integration product");
            product.setDescription("Disposable");
            product.setBasePrice(BigDecimal.ONE);
            product.setStatus("UNAVAILABLE");
            em.persist(product);
            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setVariantName("Mặc định");
            variant.setPrice(BigDecimal.ONE);
            variant.setSku("LIFECYCLE-" + System.nanoTime());
            variant.setInventoryMode("UNTRACKED");
            variant.setIsDefault(true);
            variant.setStatus("AVAILABLE");
            em.persist(variant);
            em.flush();
            productId = product.getProductId();
            usedProductId = ((Number) em.createNativeQuery("SELECT TOP 1 product_id FROM OrderItem ORDER BY order_item_id").getSingleResult()).intValue();
            em.getTransaction().commit();
        } finally {
            em.close();
        }

        ProductDAO dao = new ProductDAO();
        assertTrue(dao.restore(productId));
        assertEquals("AVAILABLE", dao.findById(productId).getStatus());
        assertThrows(ProductDAO.ProductInUseException.class, () -> dao.permanentlyDelete(usedProductId));
        assertTrue(dao.permanentlyDelete(productId));
        assertFalse(dao.permanentlyDelete(productId));
    }
}
