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
                    "SELECT z FROM DeliveryZone z WHERE z.isActive = true", DeliveryZone.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
