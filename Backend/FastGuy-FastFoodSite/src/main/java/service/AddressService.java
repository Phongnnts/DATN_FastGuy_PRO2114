package service;

import entity.Address;
import entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import utils.DatabaseUtil;

public class AddressService {
    public Address create(int userId, Address address) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            User user = em.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
            if (user == null) throw new IllegalArgumentException("User not found");
            address.setUser(user);
            if (Boolean.TRUE.equals(address.getIsDefault())) clearDefault(em, userId);
            em.persist(address);
            em.getTransaction().commit();
            return address;
        } catch (RuntimeException e) {
            rollback(em);
            throw e;
        } finally {
            em.close();
        }
    }

    public Address update(int userId, int addressId, Address values, Boolean isDefault) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            User user = em.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
            if (user == null) throw new IllegalArgumentException("Address not found");
            Address address = em.find(Address.class, addressId, LockModeType.PESSIMISTIC_WRITE);
            if (address == null || address.getUser().getUserId() != userId) throw new IllegalArgumentException("Address not found");
            copy(address, values);
            if (Boolean.TRUE.equals(isDefault)) clearDefault(em, userId);
            if (isDefault != null) address.setIsDefault(isDefault);
            em.getTransaction().commit();
            return address;
        } catch (RuntimeException e) {
            rollback(em);
            throw e;
        } finally {
            em.close();
        }
    }

    public void setDefault(int userId, int addressId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            User user = em.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
            if (user == null) throw new IllegalArgumentException("Address not found");
            Address address = em.find(Address.class, addressId, LockModeType.PESSIMISTIC_WRITE);
            if (address == null || address.getUser().getUserId() != userId) throw new IllegalArgumentException("Address not found");
            clearDefault(em, userId);
            address.setIsDefault(true);
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            rollback(em);
            throw e;
        } finally {
            em.close();
        }
    }

    private void clearDefault(EntityManager em, int userId) {
        em.createQuery("UPDATE Address a SET a.isDefault = false WHERE a.user.userId = :userId")
                .setParameter("userId", userId).executeUpdate();
    }

    private void copy(Address target, Address source) {
        target.setRecipientName(source.getRecipientName());
        target.setPhone(source.getPhone());
        target.setStreet(source.getStreet());
        target.setWardName(source.getWardName());
        target.setDistrictName(source.getDistrictName());
        target.setProvinceName(source.getProvinceName());
        target.setGhnProvinceId(source.getGhnProvinceId());
        target.setGhnDistrictId(source.getGhnDistrictId());
        target.setGhnWardCode(source.getGhnWardCode());
        target.setCity(source.getCity());
    }

    private void rollback(EntityManager em) {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
    }
}
