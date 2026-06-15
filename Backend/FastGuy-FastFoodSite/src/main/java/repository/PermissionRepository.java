package repository;

import config.HibernateConfig;
import entity.Permission;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

public class PermissionRepository {

    public Permission findByName(String permissionName) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery("SELECT p FROM Permission p WHERE p.permissionName = :name", Permission.class)
                    .setParameter("name", permissionName)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
