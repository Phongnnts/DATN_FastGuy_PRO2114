package dao;

import entity.Coupon;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.util.List;

public class CouponDAO {
    public List<Coupon> findAll() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("from Coupon order by createdAt desc", Coupon.class).getResultList();
        } finally {
            em.close();
        }
    }

    public Coupon findById(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.find(Coupon.class, id);
        } finally {
            em.close();
        }
    }

    public Coupon findByCode(String code) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<Coupon> list = em.createQuery("from Coupon where code = :code", Coupon.class)
                    .setParameter("code", code).getResultList();
            return list.isEmpty() ? null : list.get(0);
        } finally {
            em.close();
        }
    }

    public void save(Coupon coupon) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (coupon.getCouponId() == 0) {
                em.persist(coupon);
            } else {
                em.merge(coupon);
            }
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public void delete(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Coupon c = em.find(Coupon.class, id);
            if (c != null) em.remove(c);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public void incrementUsedCount(int couponId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Coupon c = em.find(Coupon.class, couponId);
            if (c != null) {
                c.setUsedCount(c.getUsedCount() + 1);
                em.merge(c);
            }
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public List<Coupon> findPublicActive() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("from Coupon where isPublic = true and isActive = true and (expiresAt is null or expiresAt > current timestamp) and (maxUses = 0 or usedCount < maxUses) order by createdAt desc", Coupon.class).getResultList();
        } finally {
            em.close();
        }
    }
}
