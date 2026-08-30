package dao;

import entity.ActivityLog;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

public class ActivityLogDAO {
 public record Page(List<ActivityLog>items,long total){}
 public Page list(EntityManager em,LocalDateTime from,LocalDateTime to,String action,Integer actor,int page,int size){String where=" WHERE 1=1";if(from!=null)where+=" AND l.createdAt>=:from";if(to!=null)where+=" AND l.createdAt<=:to";if(action!=null)where+=" AND l.actionType=:action";if(actor!=null)where+=" AND l.actor.userId=:actor";TypedQuery<ActivityLog>q=em.createQuery("SELECT l FROM ActivityLog l JOIN FETCH l.actor"+where+" ORDER BY l.createdAt DESC,l.activityLogId DESC",ActivityLog.class);TypedQuery<Long>c=em.createQuery("SELECT COUNT(l) FROM ActivityLog l"+where,Long.class);for(Query x:List.of(q,c)){if(from!=null)x.setParameter("from",from);if(to!=null)x.setParameter("to",to);if(action!=null)x.setParameter("action",action);if(actor!=null)x.setParameter("actor",actor);}return new Page(q.setFirstResult((page-1)*size).setMaxResults(size).getResultList(),c.getSingleResult());}
}
