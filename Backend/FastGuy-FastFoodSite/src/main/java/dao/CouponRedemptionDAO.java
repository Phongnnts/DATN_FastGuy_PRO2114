package dao;

import entity.CouponRedemption;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.util.List;

public class CouponRedemptionDAO {
    public void save(CouponRedemption cr) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(cr);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RuntimeException(e);
        } finally {
            em.close();
        }
    }

    public boolean exists(int couponId, int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            Long count = em.createQuery("SELECT COUNT(cr) FROM CouponRedemption cr WHERE cr.couponId = :couponId AND cr.userId = :userId", Long.class)
                    .setParameter("couponId", couponId).setParameter("userId", userId).getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }

    public boolean hasUnused(int couponId, int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            Long count = em.createQuery("SELECT COUNT(cr) FROM CouponRedemption cr WHERE cr.couponId = :couponId AND cr.userId = :userId AND cr.usedAt IS NULL", Long.class)
                    .setParameter("couponId", couponId).setParameter("userId", userId).getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }

    public boolean hasUserUsed(int couponId, int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            Long count = em.createQuery("SELECT COUNT(cr) FROM CouponRedemption cr WHERE cr.couponId = :couponId AND cr.userId = :userId AND cr.usedAt IS NOT NULL", Long.class)
                    .setParameter("couponId", couponId).setParameter("userId", userId).getSingleResult();
            return count > 0;
        } finally {
            em.close();
        }
    }

    public CouponRedemption findUnused(int couponId, int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<CouponRedemption> list = em.createQuery("SELECT cr FROM CouponRedemption cr WHERE cr.couponId = :couponId AND cr.userId = :userId AND cr.usedAt IS NULL", CouponRedemption.class)
                    .setParameter("couponId", couponId).setParameter("userId", userId).getResultList();
            return list.isEmpty() ? null : list.get(0);
        } finally {
            em.close();
        }
    }

    public CouponRedemption findByOrderId(int orderId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<CouponRedemption> list = em.createQuery("SELECT cr FROM CouponRedemption cr WHERE cr.orderId = :orderId", CouponRedemption.class)
                    .setParameter("orderId", orderId).getResultList();
            return list.isEmpty() ? null : list.get(0);
        } finally {
            em.close();
        }
    }

    public List<CouponRedemption> findByUserId(int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT cr FROM CouponRedemption cr WHERE cr.userId = :userId ORDER BY cr.claimedAt DESC", CouponRedemption.class)
                    .setParameter("userId", userId).getResultList();
        } finally {
            em.close();
        }
    }

    public List<CouponRedemption> findUnusedByUserId(int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT cr FROM CouponRedemption cr WHERE cr.userId = :userId AND cr.usedAt IS NULL ORDER BY cr.claimedAt DESC", CouponRedemption.class)
                    .setParameter("userId", userId).getResultList();
        } finally {
            em.close();
        }
    }
}
