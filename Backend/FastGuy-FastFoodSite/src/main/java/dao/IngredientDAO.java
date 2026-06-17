package dao;

import entity.Ingredient;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.util.List;

public class IngredientDAO {
    public long countLowStock() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(i) FROM Ingredient i WHERE i.stockQuantity < i.minStockThreshold",
                    Long.class)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public List<Ingredient> findAll() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT i FROM Ingredient i", Ingredient.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
