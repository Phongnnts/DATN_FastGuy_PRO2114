package dao;

import entity.Category;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.util.List;

public class CategoryDAO {
    public List<Category> findAllActive() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT c FROM Category c WHERE c.status = 'ACTIVE' ORDER BY c.sortOrder",
                    Category.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Category> findAll() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT c FROM Category c ORDER BY c.sortOrder", Category.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public Category findById(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.find(Category.class, id);
        } finally {
            em.close();
        }
    }

    public void save(Category category) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (category.getCategoryId() == 0) {
                em.persist(category);
            } else {
                em.merge(category);
            }
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean hasProducts(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(p) FROM Product p WHERE p.category.categoryId = :id", Long.class)
                    .setParameter("id", id)
                    .getSingleResult() > 0;
        } finally {
            em.close();
        }
    }

    public void delete(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Category c = em.find(Category.class, id);
            if (c != null) em.remove(c);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
