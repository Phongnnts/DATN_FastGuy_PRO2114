package dao;

import entity.Banner;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.util.List;

public class BannerDAO {
    public List<Banner> findAll() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("from Banner order by sortOrder asc, createdAt desc", Banner.class).getResultList();
        } finally {
            em.close();
        }
    }

    public List<Banner> findAllActive() {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("from Banner where isActive = true order by sortOrder asc, createdAt desc", Banner.class).getResultList();
        } finally {
            em.close();
        }
    }

    public Banner findById(int id) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.find(Banner.class, id);
        } finally {
            em.close();
        }
    }

    public void save(Banner banner) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (banner.getBannerId() == 0) {
                em.persist(banner);
            } else {
                em.merge(banner);
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
            Banner b = em.find(Banner.class, id);
            if (b != null) em.remove(b);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }
}
