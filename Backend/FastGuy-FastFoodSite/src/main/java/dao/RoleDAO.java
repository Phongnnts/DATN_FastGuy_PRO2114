package dao;

import entity.Role;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

public class RoleDAO {
    public Role findByName(String name) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT r FROM Role r WHERE r.roleName = :name", Role.class)
                    .setParameter("name", name)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        } finally {
            em.close();
        }
    }
}
