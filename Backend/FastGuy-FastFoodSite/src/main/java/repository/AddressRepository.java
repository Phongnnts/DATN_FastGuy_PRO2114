package repository;

import config.HibernateConfig;
import entity.Address;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class AddressRepository {

    public List<Address> findByUserId(Long userId) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery(
                    "SELECT a FROM Address a WHERE a.user.userId = :userId ORDER BY a.isDefault DESC, a.createdAt DESC",
                    Address.class)
                    .setParameter("userId", userId)
                    .getResultList();
        }
    }

    public Optional<Address> findByIdAndUserId(Long addressId, Long userId) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery(
                    "SELECT a FROM Address a WHERE a.addressId = :id AND a.user.userId = :userId",
                    Address.class)
                    .setParameter("id", addressId)
                    .setParameter("userId", userId)
                    .getResultStream().findFirst();
        }
    }

    public void clearDefault(Long userId) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            em.getTransaction().begin();
            em.createQuery("UPDATE Address a SET a.isDefault = false WHERE a.user.userId = :userId")
                    .setParameter("userId", userId)
                    .executeUpdate();
            em.getTransaction().commit();
        }
    }

    public Address save(Address address) {
        EntityManager em = HibernateConfig.getEntityManager();
        try {
            em.getTransaction().begin();
            if (address.getAddressId() == null) {
                em.persist(address);
            } else {
                address = em.merge(address);
            }
            em.getTransaction().commit();
            return address;
        } finally {
            em.close();
        }
    }

    public void delete(Address address) {
        EntityManager em = HibernateConfig.getEntityManager();
        try {
            em.getTransaction().begin();
            Address managed = em.merge(address);
            em.remove(managed);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }
}
