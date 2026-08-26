package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import service.OrderTransitionService;
import service.StaffOrderService;
import utils.DatabaseUtil;

class StaffShipperAssignmentIT {
    @Test
    void detailedTransitionAssignsCheckedInShipperToReadyOrder() {
        EntityManager em = DatabaseUtil.getEntityManager();
        int orderId;
        int staffId;
        int shipperId;
        int shiftId;
        try {
            assertTrue(((String) em.createNativeQuery("SELECT DB_NAME()").getSingleResult()).endsWith("_Test"));
            orderId = ((Number) em.createNativeQuery("SELECT TOP 1 order_id FROM Orders WHERE order_status='READY' AND shipper_id IS NULL ORDER BY order_id DESC").getSingleResult()).intValue();
            staffId = ((Number) em.createNativeQuery("SELECT TOP 1 user_id FROM Users WHERE role_name='STAFF' AND status='ACTIVE' ORDER BY user_id").getSingleResult()).intValue();
            shipperId = ((Number) em.createNativeQuery("SELECT TOP 1 user_id FROM Users WHERE role_name='SHIPPER' AND status='ACTIVE' ORDER BY user_id").getSingleResult()).intValue();
            em.getTransaction().begin();
            em.createNativeQuery("INSERT INTO WorkShift(user_id,shift_date,start_time,end_time,check_in_at,status,created_at) VALUES(:userId,:date,:start,:end,:checkedIn,'CHECKED_IN',:created)")
                    .setParameter("userId", shipperId)
                    .setParameter("date", LocalDate.now())
                    .setParameter("start", LocalTime.now().minusHours(1))
                    .setParameter("end", LocalTime.now().plusHours(4))
                    .setParameter("checkedIn", LocalDateTime.now().minusMinutes(5))
                    .setParameter("created", LocalDateTime.now())
                    .executeUpdate();
            shiftId = ((Number) em.createNativeQuery("SELECT MAX(shift_id) FROM WorkShift WHERE user_id=:userId AND shift_date=:date")
                    .setParameter("userId", shipperId).setParameter("date", LocalDate.now()).getSingleResult()).intValue();
            em.getTransaction().commit();
        } finally {
            em.close();
        }

        try {
            assertEquals(OrderTransitionService.MutationResult.SUCCESS,
                    new StaffOrderService().assignShipper(orderId, shipperId, staffId, "READY"));
            EntityManager verify = DatabaseUtil.getEntityManager();
            try {
                Object[] row = (Object[]) verify.createNativeQuery("SELECT order_status,shipper_id FROM Orders WHERE order_id=:id").setParameter("id", orderId).getSingleResult();
                assertEquals("ASSIGNED", row[0]);
                assertEquals(shipperId, ((Number) row[1]).intValue());
            } finally {
                verify.close();
            }
        } finally {
            EntityManager cleanup = DatabaseUtil.getEntityManager();
            try {
                cleanup.getTransaction().begin();
                cleanup.createNativeQuery("DELETE FROM OrderStatusHistory WHERE order_id=:orderId AND from_status='READY' AND to_status='ASSIGNED' AND actor_user_id=:staffId")
                        .setParameter("orderId", orderId).setParameter("staffId", staffId).executeUpdate();
                cleanup.createNativeQuery("UPDATE Orders SET order_status='READY',shipper_id=NULL,assigned_at=NULL WHERE order_id=:id").setParameter("id", orderId).executeUpdate();
                cleanup.createNativeQuery("DELETE FROM WorkShift WHERE shift_id=:id").setParameter("id", shiftId).executeUpdate();
                cleanup.getTransaction().commit();
            } catch (RuntimeException e) {
                if (cleanup.getTransaction().isActive()) cleanup.getTransaction().rollback();
                throw e;
            } finally {
                cleanup.close();
            }
        }
    }
}
