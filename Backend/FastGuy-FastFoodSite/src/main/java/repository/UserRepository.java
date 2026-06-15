package repository;

import config.HibernateConfig;
import entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import java.util.Optional;

public class UserRepository {

    public Optional<User> findById(Long userId) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            User user = em.find(User.class, userId);
            return Optional.ofNullable(user);
        }
    }

    public Optional<User> findByPhone(String phone) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            User user = em.createQuery("SELECT u FROM User u WHERE u.phone = :phone", User.class)
                    .setParameter("phone", phone)
                    .getSingleResult();
            return Optional.ofNullable(user);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            User user = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return Optional.ofNullable(user);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public long count() {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery("SELECT COUNT(u) FROM User u", Long.class)
                    .getSingleResult();
        }
    }

    public boolean existsByPhone(String phone) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            Long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.phone = :phone", Long.class)
                    .setParameter("phone", phone)
                    .getSingleResult();
            return count > 0;
        }
    }

    public User save(User user) {
        EntityManager em = HibernateConfig.getEntityManager();
        try {
            em.getTransaction().begin();
            if (user.getUserId() == null) {
                em.persist(user);
            } else {
                user = em.merge(user);
            }
            em.getTransaction().commit();
            return user;
        } finally {
            em.close();
        }
    }
}
