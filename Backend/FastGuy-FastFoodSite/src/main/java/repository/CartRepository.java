package repository;

import config.HibernateConfig;
import entity.Cart;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import java.util.Optional;

public class CartRepository {

    public Optional<Cart> findByUserId(Long userId) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            Cart cart = em.createQuery(
                    "SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.user.userId = :uid",
                    Cart.class)
                    .setParameter("uid", userId)
                    .getSingleResult();
            return Optional.ofNullable(cart);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<Cart> findBySessionId(String sessionId) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            Cart cart = em.createQuery(
                    "SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.sessionId = :sid",
                    Cart.class)
                    .setParameter("sid", sessionId)
                    .getSingleResult();
            return Optional.ofNullable(cart);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Cart save(Cart cart) {
        EntityManager em = HibernateConfig.getEntityManager();
        try {
            em.getTransaction().begin();
            if (cart.getCartId() == null) {
                em.persist(cart);
            } else {
                cart = em.merge(cart);
            }
            em.getTransaction().commit();
            return cart;
        } finally {
            em.close();
        }
    }
}
