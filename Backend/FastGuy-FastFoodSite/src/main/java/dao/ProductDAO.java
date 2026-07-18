package dao;

import entity.Product;
import entity.ProductVariant;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.util.List;

public class ProductDAO {
    public Product findById(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.find(Product.class, id);
        } finally {
            em.close();
        }
    }

    public List<Product> findByCategoryId(int categoryId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT p FROM Product p WHERE p.category.categoryId = :cid AND p.status = 'AVAILABLE' ORDER BY p.productId",
                    Product.class)
                    .setParameter("cid", categoryId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Product> findAllAvailable() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT p FROM Product p WHERE p.status = 'AVAILABLE' ORDER BY p.productId",
                    Product.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Product> findAll() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT p FROM Product p ORDER BY p.productId", Product.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<ProductVariant> findVariantsByProductId(int productId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT v FROM ProductVariant v WHERE v.product.productId = :pid ORDER BY v.isDefault DESC, v.variantId",
                    ProductVariant.class)
                    .setParameter("pid", productId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public ProductVariant findDefaultVariantByProductId(int productId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<ProductVariant> result = em.createQuery(
                    "SELECT v FROM ProductVariant v WHERE v.product.productId = :pid AND v.isDefault = true ORDER BY v.variantId",
                    ProductVariant.class)
                    .setParameter("pid", productId)
                    .setMaxResults(1)
                    .getResultList();
            return result.isEmpty() ? null : result.get(0);
        } finally {
            em.close();
        }
    }

    public ProductVariant findVariantById(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.find(ProductVariant.class, id);
        } finally {
            em.close();
        }
    }

    public long count() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(p) FROM Product p", Long.class)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public void save(Product product) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (product.getProductId() == 0) {
                em.persist(product);
            } else {
                em.merge(product);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
            throw new RuntimeException("Failed to save product: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    public void delete(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Product p = em.find(Product.class, id);
            if (p != null) em.remove(p);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
            throw new RuntimeException("Failed to delete product: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    public void saveVariant(ProductVariant variant) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (variant.getVariantId() == 0) {
                em.persist(variant);
            } else {
                em.merge(variant);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
            throw new RuntimeException("Failed to save variant: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    public void deleteVariant(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            ProductVariant v = em.find(ProductVariant.class, id);
            if (v != null) em.remove(v);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
            throw new RuntimeException("Failed to delete variant: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }
}
