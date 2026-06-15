package repository;

import config.HibernateConfig;
import entity.Category;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class CategoryRepository {

    public List<Category> findAllActive() {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery(
                    "SELECT c FROM Category c WHERE c.status = 'ACTIVE' ORDER BY c.sortOrder",
                    Category.class)
                    .getResultList();
        }
    }

    public Optional<Category> findById(Long categoryId) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return Optional.ofNullable(em.find(Category.class, categoryId));
        }
    }
}
