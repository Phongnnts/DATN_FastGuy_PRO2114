package repository;

import config.HibernateConfig;
import entity.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

public class RoleRepository {

    public Role findByName(String roleName) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery("SELECT r FROM Role r WHERE r.roleName = :name", Role.class)
                    .setParameter("name", roleName)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
