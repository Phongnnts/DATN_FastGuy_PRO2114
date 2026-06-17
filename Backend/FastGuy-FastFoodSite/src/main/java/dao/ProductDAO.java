package dao;

import entity.Product;
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

    public long count() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(p) FROM Product p", Long.class)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }
}
