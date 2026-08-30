package entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name="ActivityLog")
public class ActivityLog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="activity_log_id") private long activityLogId;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="actor_user_id",nullable=false) private User actor;
 @Column(name="action_type",nullable=false,length=100) private String actionType;
 @Column(name="target_type",nullable=false,length=100) private String targetType;
 @Column(name="target_id",length=255) private String targetId;
 @Column(name="summary",nullable=false,length=500) private String summary;
 @Column(name="metadata_json",columnDefinition="nvarchar(max)") private String metadataJson;
 @Column(name="created_at",nullable=false,insertable=false,updatable=false) private LocalDateTime createdAt;
 public long getActivityLogId(){return activityLogId;} public User getActor(){return actor;} public void setActor(User v){actor=v;} public String getActionType(){return actionType;} public void setActionType(String v){actionType=v;} public String getTargetType(){return targetType;} public void setTargetType(String v){targetType=v;} public String getTargetId(){return targetId;} public void setTargetId(String v){targetId=v;} public String getSummary(){return summary;} public void setSummary(String v){summary=v;} public String getMetadataJson(){return metadataJson;} public void setMetadataJson(String v){metadataJson=v;} public LocalDateTime getCreatedAt(){return createdAt;}
}
