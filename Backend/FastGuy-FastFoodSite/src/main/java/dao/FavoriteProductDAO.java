package dao;

import entity.FavoriteProduct;
import entity.Product;
import entity.User;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.util.List;

public class FavoriteProductDAO {
    public List<FavoriteProduct> findByUserId(int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT f FROM FavoriteProduct f WHERE f.user.userId = :uid ORDER BY f.createdAt DESC",
                    FavoriteProduct.class)
                    .setParameter("uid", userId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public boolean exists(int userId, int productId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                    "SELECT COUNT(f) FROM FavoriteProduct f WHERE f.user.userId = :uid AND f.product.productId = :pid",
                    Long.class)
                    .setParameter("uid", userId)
                    .setParameter("pid", productId)
                    .getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }

    public void add(int userId, int productId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Long count = em.createQuery(
                    "SELECT COUNT(f) FROM FavoriteProduct f WHERE f.user.userId = :uid AND f.product.productId = :pid",
                    Long.class)
                    .setParameter("uid", userId)
                    .setParameter("pid", productId)
                    .getSingleResult();
            if (count == 0) {
                FavoriteProduct favorite = new FavoriteProduct();
                favorite.setUser(em.getReference(User.class, userId));
                favorite.setProduct(em.getReference(Product.class, productId));
                favorite.setCreatedAt(java.time.LocalDateTime.now());
                em.persist(favorite);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw new RuntimeException("Failed to add favorite: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    public void remove(int userId, int productId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM FavoriteProduct f WHERE f.user.userId = :uid AND f.product.productId = :pid")
                    .setParameter("uid", userId)
                    .setParameter("pid", productId)
                    .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw new RuntimeException("Failed to remove favorite: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }
}
