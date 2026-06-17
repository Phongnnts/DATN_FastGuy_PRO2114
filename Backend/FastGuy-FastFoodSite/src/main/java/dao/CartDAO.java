package dao;

import entity.CartItem;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

public class CartDAO {
    public int countItemsByUserId(int userId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                    "SELECT SUM(ci.quantity) FROM CartItem ci WHERE ci.cart.user.userId = :uid",
                    Long.class)
                    .setParameter("uid", userId)
                    .getSingleResult();
            return count != null ? count.intValue() : 0;
        } finally {
            em.close();
        }
    }
}
