package dao;

import entity.DeliveryZone;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.util.List;

public class DeliveryZoneDAO {
    public DeliveryZone findById(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.find(DeliveryZone.class, id);
        } finally {
            em.close();
        }
    }

    public List<DeliveryZone> findAllActive() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT z FROM DeliveryZone z WHERE z.isActive = true ORDER BY z.zoneId",
                    DeliveryZone.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<DeliveryZone> findAll() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT z FROM DeliveryZone z ORDER BY z.zoneId", DeliveryZone.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void save(DeliveryZone zone) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (zone.getZoneId() == 0) {
                em.persist(zone);
            } else {
                em.merge(zone);
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
            DeliveryZone z = em.find(DeliveryZone.class, id);
            if (z != null) em.remove(z);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}
