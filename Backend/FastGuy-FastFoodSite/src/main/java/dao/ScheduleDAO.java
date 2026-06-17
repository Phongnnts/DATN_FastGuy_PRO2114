package dao;

import entity.Schedule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import utils.DatabaseUtil;

import java.time.LocalDate;

public class ScheduleDAO {
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
}
