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

    public Category findById(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.find(Category.class, id);
        } finally {
            em.close();
        }
    }
}
