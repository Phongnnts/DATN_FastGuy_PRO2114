package repository;

import config.HibernateConfig;
import entity.InventoryTransaction;
import jakarta.persistence.EntityManager;

import java.util.List;

public class InventoryTransactionRepository {

    public List<InventoryTransaction> findByIngredientId(Long ingredientId) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery(
                    "SELECT t FROM InventoryTransaction t WHERE t.ingredient.ingredientId = :id ORDER BY t.createdAt DESC",
                    InventoryTransaction.class)
                    .setParameter("id", ingredientId)
                    .getResultList();
        }
    }

    public InventoryTransaction save(InventoryTransaction transaction) {
        EntityManager em = HibernateConfig.getEntityManager();
        try {
            em.getTransaction().begin();
            if (transaction.getTransactionId() == null) {
                em.persist(transaction);
            } else {
                transaction = em.merge(transaction);
            }
            em.getTransaction().commit();
            return transaction;
        } finally {
            em.close();
        }
    }
}
