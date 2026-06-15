package repository;

import config.HibernateConfig;
import entity.Product;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class ProductRepository {

    public List<Product> findByCategoryId(Long categoryId) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery(
                    "SELECT p FROM Product p WHERE p.category.categoryId = :catId AND p.status = 'AVAILABLE'",
                    Product.class)
                    .setParameter("catId", categoryId)
                    .getResultList();
        }
    }

    public List<Product> searchByName(String keyword) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery(
                    "SELECT p FROM Product p WHERE p.status = 'AVAILABLE' AND p.name LIKE :kw",
                    Product.class)
                    .setParameter("kw", "%" + keyword + "%")
                    .getResultList();
        }
    }

    public List<Product> findAllAvailable() {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery(
                    "SELECT p FROM Product p WHERE p.status = 'AVAILABLE'",
                    Product.class)
                    .getResultList();
        }
    }

    public long countAvailable() {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery("SELECT COUNT(p) FROM Product p WHERE p.status = 'AVAILABLE'", Long.class)
                    .getSingleResult();
        }
    }

    public Optional<Product> findById(Long productId) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return Optional.ofNullable(em.find(Product.class, productId));
        }
    }
}
