package dao;

import entity.ClaimedCoupon;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.util.List;

public class ClaimedCouponDAO {
    public void save(ClaimedCoupon cc) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(cc);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public boolean exists(int couponId, int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<ClaimedCoupon> list = em.createQuery(
                    "from ClaimedCoupon where couponId = :couponId and userId = :userId", ClaimedCoupon.class)
                    .setParameter("couponId", couponId)
                    .setParameter("userId", userId)
                    .getResultList();
            return !list.isEmpty();
        } finally {
            em.close();
        }
    }

    public List<ClaimedCoupon> findByUserId(int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "from ClaimedCoupon where userId = :userId order by claimedAt desc", ClaimedCoupon.class)
                    .setParameter("userId", userId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<ClaimedCoupon> findUnusedByUserId(int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery(
                    "from ClaimedCoupon where userId = :userId and usedAt is null order by claimedAt desc", ClaimedCoupon.class)
                    .setParameter("userId", userId)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
