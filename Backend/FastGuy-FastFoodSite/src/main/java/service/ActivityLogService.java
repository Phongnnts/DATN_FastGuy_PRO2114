package service;

import com.fasterxml.jackson.core.JsonProcessingException;
import dao.ActivityLogDAO;
import entity.ActivityLog;
import entity.User;
import java.time.LocalDateTime;
import utils.DatabaseUtil;
import jakarta.persistence.EntityManager;
import java.util.*;
import utils.JsonUtil;

public class ActivityLogService {
 public static final Set<String>ACTIONS=Set.of("ORDER_CANCELLED","ORDER_REFUND_RECORDED","DELIVERY_ATTEMPT_OVERRIDDEN","ATTENDANCE_APPROVED","STAFF_PAY_RATE_CREATED","STOCK_COUNT_APPROVED");
 private static final Map<String,Set<String>>METADATA=Map.of("ORDER_CANCELLED",Set.of("orderCode","reason"),"ORDER_REFUND_RECORDED",Set.of("refundStatus","refundAmount"),"DELIVERY_ATTEMPT_OVERRIDDEN",Set.of("deliveryAttemptLimit"),"ATTENDANCE_APPROVED",Set.of("approvedMinutes","approvedOvertimeMinutes"),"STAFF_PAY_RATE_CREATED",Set.of("staffUserId","effectiveFrom","regularHourlyRate","overtimeHourlyRate"),"STOCK_COUNT_APPROVED",Set.of("countDate","frequency"));
 public void append(EntityManager em,int actorId,String action,String targetType,Object targetId,Map<String,?>values){if(em==null||!em.getTransaction().isActive())throw new IllegalStateException("Active transaction required");if(!ACTIONS.contains(action))throw new IllegalArgumentException("Invalid activity action");ActivityLog log=new ActivityLog();log.setActor(em.getReference(User.class,actorId));log.setActionType(action);log.setTargetType(targetType);log.setTargetId(targetId==null?null:String.valueOf(targetId));log.setSummary(summary(action,targetId));log.setMetadataJson(metadata(action,values));em.persist(log);em.flush();}
 static String metadata(String action,Map<String,?>values){Set<String>allowed=METADATA.get(action);if(allowed==null||values==null||!allowed.containsAll(values.keySet()))throw new IllegalArgumentException("Invalid activity metadata");try{return JsonUtil.getMapper().writeValueAsString(values);}catch(JsonProcessingException e){throw new IllegalArgumentException("Invalid activity metadata",e);}}
 private static String summary(String action,Object id){return switch(action){case"ORDER_CANCELLED"->"Order "+id+" cancelled";case"ORDER_REFUND_RECORDED"->"Refund recorded for order "+id;case"DELIVERY_ATTEMPT_OVERRIDDEN"->"Delivery attempt limit overridden for order "+id;case"ATTENDANCE_APPROVED"->"Attendance "+id+" approved";case"STAFF_PAY_RATE_CREATED"->"Staff pay rate "+id+" created";case"STOCK_COUNT_APPROVED"->"Stock count "+id+" approved";default->throw new IllegalArgumentException("Invalid activity action");};}
 public Map<String,Object>list(LocalDateTime from,LocalDateTime to,String action,Integer actor,int page,int size){if(from!=null&&to!=null&&to.isBefore(from)||action!=null&&!ACTIONS.contains(action)||actor!=null&&actor<1||page<1||size<1||size>100)throw new IllegalArgumentException("Invalid activity log filters");EntityManager em=DatabaseUtil.getEntityManager();try{ActivityLogDAO.Page result=new ActivityLogDAO().list(em,from,to,action,actor,page,size);List<Map<String,Object>>items=result.items().stream().map(this::dto).toList();long pages=(result.total()+size-1)/size;return Map.of("items",items,"pagination",Map.of("page",page,"pageSize",size,"totalItems",result.total(),"totalPages",pages));}finally{em.close();}}
 private Map<String,Object>dto(ActivityLog l){Map<String,Object>m=new LinkedHashMap<>();m.put("activityLogId",l.getActivityLogId());m.put("actor",Map.of("userId",l.getActor().getUserId(),"fullName",l.getActor().getFullName()));m.put("actionType",l.getActionType());m.put("targetType",l.getTargetType());m.put("targetId",l.getTargetId()==null?null:Integer.valueOf(l.getTargetId()));m.put("summary",l.getSummary());try{m.put("metadata",JsonUtil.getMapper().readValue(l.getMetadataJson(),Map.class));}catch(JsonProcessingException e){throw new IllegalStateException("Invalid persisted activity metadata",e);}m.put("createdAt",l.getCreatedAt());return m;}
}
