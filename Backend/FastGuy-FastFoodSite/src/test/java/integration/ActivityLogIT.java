package integration;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManager;
import java.util.Map;
import org.junit.jupiter.api.Test;
import service.ActivityLogService;
import utils.DatabaseUtil;

class ActivityLogIT {
    @Test
    void disposableDatabaseAppendsListsAndRollsBackMutationWhenAuditFails() {
        EntityManager em=DatabaseUtil.getEntityManager();int actorId;String originalName;
        try {
            assertEquals("FastGuyDB_ActivityLog063_Test",em.createNativeQuery("SELECT DB_NAME()").getSingleResult());
            Object[] actor=(Object[])em.createNativeQuery("SELECT TOP 1 user_id,full_name FROM Users WHERE role_name='ADMIN' ORDER BY user_id").getSingleResult();actorId=((Number)actor[0]).intValue();originalName=(String)actor[1];
            ActivityLogService logs=new ActivityLogService();
            em.getTransaction().begin();
            logs.append(em,actorId,"ORDER_CANCELLED","ORDER",900063,Map.of("orderCode","R7-063","reason","integration"));
            em.getTransaction().commit();
            Map<String,Object> page=logs.list(null,null,"ORDER_CANCELLED",actorId,1,20);Map<?,?> item=(Map<?,?>)((java.util.List<?>)page.get("items")).get(0);
            assertEquals(900063,item.get("targetId"));assertEquals("R7-063",((Map<?,?>)item.get("metadata")).get("orderCode"));assertEquals(6,ActivityLogService.ACTIONS.size());
            em.getTransaction().begin();
            em.createNativeQuery("UPDATE Users SET full_name=N'R7 rollback probe' WHERE user_id=:id").setParameter("id",actorId).executeUpdate();
            assertThrows(RuntimeException.class,()->em.createNativeQuery("INSERT ActivityLog(actor_user_id,action_type,target_type,target_id,summary,metadata_json) VALUES(:id,'ORDER_CANCELLED','ORDER','900064','probe','not-json')").setParameter("id",actorId).executeUpdate());
            em.getTransaction().rollback();em.clear();
            assertEquals(originalName,em.createNativeQuery("SELECT full_name FROM Users WHERE user_id=:id").setParameter("id",actorId).getSingleResult());
            assertEquals(0L,((Number)em.createNativeQuery("SELECT COUNT(*) FROM ActivityLog WHERE target_id='900064'").getSingleResult()).longValue());
        } finally {
            if(em.getTransaction().isActive())em.getTransaction().rollback();em.close();
        }
    }
}
