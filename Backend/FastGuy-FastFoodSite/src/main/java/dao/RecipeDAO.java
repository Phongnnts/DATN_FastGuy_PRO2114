package dao;

import java.util.List;

import entity.Recipe;
import jakarta.persistence.EntityManager;

public class RecipeDAO {
    public Recipe findActiveByVariant(EntityManager em, int variantId) {
        List<Recipe> recipes = em.createQuery("SELECT DISTINCT r FROM Recipe r LEFT JOIN FETCH r.items ri LEFT JOIN FETCH ri.inventoryItem WHERE r.variant.variantId = :variantId AND r.active = true", Recipe.class)
                .setParameter("variantId", variantId).setMaxResults(1).getResultList();
        return recipes.isEmpty() ? null : recipes.get(0);
    }

    public List<Recipe> findActiveByVariants(EntityManager em, List<Integer> variantIds) {
        if (variantIds.isEmpty()) return List.of();
        return em.createQuery("SELECT DISTINCT r FROM Recipe r LEFT JOIN FETCH r.items ri LEFT JOIN FETCH ri.inventoryItem WHERE r.variant.variantId IN :variantIds AND r.active = true", Recipe.class)
                .setParameter("variantIds", variantIds).getResultList();
    }
}
