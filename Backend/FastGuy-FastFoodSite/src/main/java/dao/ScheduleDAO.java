package dao;

import entity.Schedule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import utils.DatabaseUtil;

import java.time.LocalDate;
import java.util.List;

public class ScheduleDAO {
    public Schedule findById(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.find(Schedule.class, id);
        } finally {
            em.close();
        }
    }

    public Schedule findByUserAndDate(int userId, LocalDate date) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT s FROM Schedule s WHERE s.user.userId = :uid AND s.workDate = :date",
                    Schedule.class)
                    .setParameter("uid", userId)
                    .setParameter("date", date)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    public List<Schedule> findAll() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT s FROM Schedule s ORDER BY s.workDate DESC", Schedule.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Schedule> findByUserId(int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT s FROM Schedule s WHERE s.user.userId = :uid ORDER BY s.workDate DESC",
                    Schedule.class)
                    .setParameter("uid", userId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void save(Schedule schedule) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (schedule.getScheduleId() == 0) em.persist(schedule);
            else em.merge(schedule);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw new RuntimeException("Failed to save schedule: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    public void delete(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Schedule s = em.find(Schedule.class, id);
            if (s != null) em.remove(s);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw new RuntimeException("Failed to delete schedule: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }
}
