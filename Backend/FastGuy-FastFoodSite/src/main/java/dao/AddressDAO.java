package dao;

import entity.Address;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.util.List;

public class AddressDAO {
    public Address findById(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.find(Address.class, id);
        } finally {
            em.close();
        }
    }

    public List<Address> findByUserId(int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT a FROM Address a WHERE a.user.userId = :uid ORDER BY a.isDefault DESC, a.createdAt DESC",
                    Address.class)
                    .setParameter("uid", userId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public Address findDefaultByUserId(int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Address> list = em.createQuery(
                    "SELECT a FROM Address a WHERE a.user.userId = :uid AND a.isDefault = true",
                    Address.class)
                    .setParameter("uid", userId)
                    .setMaxResults(1)
                    .getResultList();
            return list.isEmpty() ? null : list.get(0);
        } finally {
            em.close();
        }
    }

    public void save(Address address) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (address.getAddressId() == 0) {
                em.persist(address);
            } else {
                em.merge(address);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public void delete(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Address a = em.find(Address.class, id);
            if (a != null) em.remove(a);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public void resetDefaultForUser(int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery(
                    "UPDATE Address a SET a.isDefault = false WHERE a.user.userId = :uid")
                    .setParameter("uid", userId)
                    .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}
