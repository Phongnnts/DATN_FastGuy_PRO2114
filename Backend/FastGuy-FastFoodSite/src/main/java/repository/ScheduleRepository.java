package repository;

import config.HibernateConfig;
import entity.Schedule;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ScheduleRepository {

    public Optional<Schedule> findById(Long id) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return Optional.ofNullable(em.find(Schedule.class, id));
        }
    }

    public Optional<Schedule> findByUserAndDate(Long userId, LocalDate date) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery(
                    "SELECT s FROM Schedule s WHERE s.user.userId = :uid AND s.workDate = :date",
                    Schedule.class)
                    .setParameter("uid", userId)
                    .setParameter("date", date)
                    .getResultStream().findFirst();
        }
    }

    public List<Schedule> findByUserIdAndDateRange(Long userId, LocalDate from, LocalDate to) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery(
                    "SELECT s FROM Schedule s WHERE s.user.userId = :uid AND s.workDate BETWEEN :from AND :to",
                    Schedule.class)
                    .setParameter("uid", userId)
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .getResultList();
        }
    }

    public List<Schedule> findByDate(LocalDate date) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery("SELECT s FROM Schedule s WHERE s.workDate = :date", Schedule.class)
                    .setParameter("date", date)
                    .getResultList();
        }
    }

    public Schedule save(Schedule schedule) {
        EntityManager em = HibernateConfig.getEntityManager();
        try {
            em.getTransaction().begin();
            if (schedule.getScheduleId() == null) {
                em.persist(schedule);
            } else {
                schedule = em.merge(schedule);
            }
            em.getTransaction().commit();
            return schedule;
        } finally {
            em.close();
        }
    }
}
