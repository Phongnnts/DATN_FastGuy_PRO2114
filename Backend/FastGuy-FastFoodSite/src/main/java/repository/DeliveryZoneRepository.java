package repository;

import config.HibernateConfig;
import entity.DeliveryZone;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class DeliveryZoneRepository {

    public List<DeliveryZone> findAllActive() {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery("SELECT d FROM DeliveryZone d WHERE d.isActive = true", DeliveryZone.class)
                    .getResultList();
        }
    }

    public Optional<DeliveryZone> findById(Long zoneId) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return Optional.ofNullable(em.find(DeliveryZone.class, zoneId));
        }
    }
}
