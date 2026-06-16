package repository;

import config.HibernateConfig;
import entity.Ingredient;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class IngredientRepository {

    public List<Ingredient> findAllActive() {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery("SELECT i FROM Ingredient i WHERE i.status = 'ACTIVE'", Ingredient.class)
                    .getResultList();
        }
    }

    public List<Ingredient> findAll() {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery("SELECT i FROM Ingredient i ORDER BY i.name", Ingredient.class)
                    .getResultList();
        }
    }

    public Optional<Ingredient> findById(Long id) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return Optional.ofNullable(em.find(Ingredient.class, id));
        }
    }

    public List<Ingredient> findLowStock() {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery(
                    "SELECT i FROM Ingredient i WHERE i.stockQuantity < i.minStockThreshold AND i.status = 'ACTIVE' ORDER BY i.stockQuantity ASC",
                    Ingredient.class)
                    .getResultList();
        }
    }

    public Ingredient save(Ingredient ingredient) {
        EntityManager em = HibernateConfig.getEntityManager();
        try {
            em.getTransaction().begin();
            if (ingredient.getIngredientId() == null) {
                em.persist(ingredient);
            } else {
                ingredient = em.merge(ingredient);
            }
            em.getTransaction().commit();
            return ingredient;
        } finally {
            em.close();
        }
    }
}
