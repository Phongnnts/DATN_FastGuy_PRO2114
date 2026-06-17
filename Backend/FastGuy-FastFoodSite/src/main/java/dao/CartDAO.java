package dao;

import entity.Cart;
import entity.CartItem;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.util.List;

public class CartDAO {
    public Cart findByUserId(int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Cart> result = em.createQuery(
                    "SELECT c FROM Cart c WHERE c.user.userId = :uid", Cart.class)
                    .setParameter("uid", userId)
                    .getResultList();
            return result.isEmpty() ? null : result.get(0);
        } finally {
            em.close();
        }
    }

    public Cart create(Cart cart) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(cart);
            em.getTransaction().commit();
            return cart;
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
            return null;
        } finally {
            em.close();
        }
    }

    public List<CartItem> getItems(int cartId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT ci FROM CartItem ci WHERE ci.cart.cartId = :cid ORDER BY ci.cartItemId",
                    CartItem.class)
                    .setParameter("cid", cartId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void addItem(CartItem item) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(item);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public void updateItemQuantity(int cartItemId, int quantity) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            CartItem item = em.find(CartItem.class, cartItemId);
            if (item != null) {
                item.setQuantity(quantity);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public void removeItem(int cartItemId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            CartItem item = em.find(CartItem.class, cartItemId);
            if (item != null) {
                em.remove(item);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public void clearCart(int cartId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM CartItem ci WHERE ci.cart.cartId = :cid")
                    .setParameter("cid", cartId)
                    .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public int countItemsByUserId(int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                    "SELECT SUM(ci.quantity) FROM CartItem ci WHERE ci.cart.user.userId = :uid",
                    Long.class)
                    .setParameter("uid", userId)
                    .getSingleResult();
            return count != null ? count.intValue() : 0;
        } finally {
            em.close();
        }
    }
}
