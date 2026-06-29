package dao;

import entity.WorkShift;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.util.List;

public class WorkShiftDAO {
    public WorkShift findById(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.find(WorkShift.class, id);
        } finally {
            em.close();
        }
    }

    public List<WorkShift> findAll() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT w FROM WorkShift w ORDER BY w.startTime", WorkShift.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void save(WorkShift shift) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (shift.getShiftId() == 0) em.persist(shift);
            else em.merge(shift);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw new RuntimeException("Failed to save shift: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    public void delete(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            WorkShift w = em.find(WorkShift.class, id);
            if (w != null) em.remove(w);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw new RuntimeException("Failed to delete shift: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }
}
