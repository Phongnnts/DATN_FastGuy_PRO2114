package dao;

import entity.CouponUsage;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.util.List;

public class CouponUsageDAO {
    public void save(CouponUsage usage) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(usage);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public boolean hasUserUsed(int couponId, int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<CouponUsage> list = em.createQuery(
                    "from CouponUsage where couponId = :couponId and userId = :userId", CouponUsage.class)
                    .setParameter("couponId", couponId)
                    .setParameter("userId", userId)
                    .getResultList();
            return !list.isEmpty();
        } finally {
            em.close();
        }
    }
}
