package repository;

import config.HibernateConfig;
import entity.WorkShift;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class WorkShiftRepository {

    public List<WorkShift> findAll() {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery("SELECT w FROM WorkShift w ORDER BY w.startTime", WorkShift.class)
                    .getResultList();
        }
    }

    public List<WorkShift> findByRoleType(String roleType) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery("SELECT w FROM WorkShift w WHERE w.roleType = :role", WorkShift.class)
                    .setParameter("role", roleType)
                    .getResultList();
        }
    }

    public Optional<WorkShift> findById(Long id) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return Optional.ofNullable(em.find(WorkShift.class, id));
        }
    }

    public WorkShift save(WorkShift shift) {
        EntityManager em = HibernateConfig.getEntityManager();
        try {
            em.getTransaction().begin();
            if (shift.getShiftId() == null) {
                em.persist(shift);
            } else {
                shift = em.merge(shift);
            }
            em.getTransaction().commit();
            return shift;
        } finally {
            em.close();
        }
    }

    public void deleteById(Long id) {
        EntityManager em = HibernateConfig.getEntityManager();
        try {
            em.getTransaction().begin();
            WorkShift shift = em.find(WorkShift.class, id);
            if (shift != null) em.remove(shift);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }
}
