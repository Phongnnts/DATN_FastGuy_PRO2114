package dao;

import java.util.List;

import entity.PaymentAttempt;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

public class PaymentAttemptDAO {
    public PaymentAttempt findByOrderId(int orderId) {
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            List<PaymentAttempt> list = em.createQuery(
                    "SELECT pa FROM PaymentAttempt pa WHERE pa.order.orderId = :orderId", PaymentAttempt.class)
                    .setParameter("orderId", orderId)
                    .getResultList();
            return list.isEmpty() ? null : list.get(0);
        } finally {
            em.close();
        }
    }
}
