package dao;

import java.util.List;
import java.util.Set;

import entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import utils.DatabaseUtil;

public class UserDAO {
    public User findById(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.find(User.class, id);
        } finally {
            em.close();
        }
    }

    public List<User> findByIds(Set<Integer> ids) {
        if (ids.isEmpty()) return List.of();
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.userId IN :ids", User.class)
                    .setParameter("ids", ids)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public User findByPhone(String phone) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.phone = :phone", User.class)
                    .setParameter("phone", phone)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    public User findByEmail(String email) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    public List<User> findAll() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u ORDER BY u.userId", User.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<User> findByRoleName(String roleName) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.role = :role ORDER BY u.fullName", User.class)
                    .setParameter("role", roleName)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public long count() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(u) FROM User u", Long.class)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long countByRole(String role) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(u) FROM User u WHERE u.role = :role", Long.class)
                    .setParameter("role", role)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public long countActiveAdmins() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("SELECT u FROM User u WHERE u.role = 'ADMIN' AND u.status = 'ACTIVE'", User.class)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();
            long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.role = 'ADMIN' AND u.status = 'ACTIVE'", Long.class)
                    .getSingleResult();
            em.getTransaction().commit();
            return count;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void save(User user) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (user.getUserId() == 0) {
                em.persist(user);
            } else {
                em.merge(user);
            }
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void delete(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            User u = em.find(User.class, id);
            if (u != null) em.remove(u);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
