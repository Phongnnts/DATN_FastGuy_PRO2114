package dao;

import java.util.List;

import entity.SupportTicket;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

public class SupportTicketDAO {
    public List<SupportTicket> findByUserId(int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            return em.createQuery("SELECT t FROM SupportTicket t WHERE t.user.userId = :userId ORDER BY t.createdAt DESC", SupportTicket.class)
                    .setParameter("userId", userId).getResultList();
        } finally {
            em.close();
        }
    }

    public List<SupportTicket> findAll(boolean openOnly) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            String query = "SELECT t FROM SupportTicket t" + (openOnly ? " WHERE t.status <> 'RESOLVED'" : "") + " ORDER BY t.createdAt DESC";
            return em.createQuery(query, SupportTicket.class).getResultList();
        } finally {
            em.close();
        }
    }
}
