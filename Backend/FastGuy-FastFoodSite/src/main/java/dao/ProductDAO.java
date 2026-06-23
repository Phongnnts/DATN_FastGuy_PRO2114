package dao;

import entity.Product;
import entity.ProductOption;
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

    public List<entity.ProductOption> findOptionsByProductId(int productId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT o FROM ProductOption o WHERE o.product.productId = :pid",
                    entity.ProductOption.class)
                    .setParameter("pid", productId)
                    .getResultList();
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
        } finally {
            em.close();
        }
    }

    public ProductOption findOptionById(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.find(ProductOption.class, id);
        } finally {
            em.close();
        }
    }

    public void saveOption(ProductOption option) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (option.getOptionId() == 0) {
                em.persist(option);
            } else {
                em.merge(option);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public void deleteOption(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            ProductOption o = em.find(ProductOption.class, id);
            if (o != null) em.remove(o);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}
