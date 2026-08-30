package service;

import java.math.*;
import java.time.LocalDate;
import java.util.*;
import dao.StaffPayRateDAO;
import entity.*;
import jakarta.persistence.*;
import utils.DatabaseUtil;

public class StaffPayRateService {
 private final StaffPayRateDAO dao=new StaffPayRateDAO();private final ActivityLogService activityLogService=new ActivityLogService();
 public record Pay(BigDecimal regular,BigDecimal overtime,BigDecimal total){}
 public static BigDecimal money(BigDecimal value){if(value==null||value.signum()<=0||value.scale()>2)throw new IllegalArgumentException("Invalid hourly rate");return value.setScale(2,RoundingMode.UNNECESSARY);}
 public static Pay calculate(int minutes,int overtime,BigDecimal regularRate,BigDecimal overtimeRate){if(minutes<0||overtime<0)throw new IllegalArgumentException("Invalid approved minutes");BigDecimal r=regularRate.multiply(BigDecimal.valueOf(minutes)).divide(BigDecimal.valueOf(60),2,RoundingMode.HALF_UP);BigDecimal o=overtimeRate.multiply(BigDecimal.valueOf(overtime)).divide(BigDecimal.valueOf(60),2,RoundingMode.HALF_UP);return new Pay(r,o,r.add(o));}
 public List<Map<String,Object>> list(int userId){requireStaff(userId);return dao.list(userId).stream().map(StaffPayRateService::map).toList();}
 public Map<String,Object> create(int userId,LocalDate from,BigDecimal regular,BigDecimal overtime,int adminId){money(regular);money(overtime);EntityManager em=DatabaseUtil.getEntityManager();try{em.getTransaction().begin();User staff=em.find(User.class,userId,LockModeType.PESSIMISTIC_READ);if(staff==null||!"STAFF".equals(staff.getRole())||!"ACTIVE".equals(staff.getStatus()))throw new NoSuchElementException("Active Staff not found");StaffPayRate rate=new StaffPayRate();rate.setUser(staff);rate.setEffectiveFrom(Objects.requireNonNull(from));rate.setRegularHourlyRate(regular);rate.setOvertimeHourlyRate(overtime);rate.setCreatedBy(em.getReference(User.class,adminId));em.persist(rate);em.flush();activityLogService.append(em,adminId,"STAFF_PAY_RATE_CREATED","STAFF_PAY_RATE",rate.getPayRateId(),Map.of("staffUserId",userId,"effectiveFrom",from.toString(),"regularHourlyRate",regular.toPlainString(),"overtimeHourlyRate",overtime.toPlainString()));em.getTransaction().commit();return map(rate);}catch(PersistenceException e){if(em.getTransaction().isActive())em.getTransaction().rollback();throw new DuplicateRate();}catch(RuntimeException e){if(em.getTransaction().isActive())em.getTransaction().rollback();throw e;}finally{em.close();}}
 private void requireStaff(int id){EntityManager em=DatabaseUtil.getEntityManager();try{User u=em.find(User.class,id);if(u==null||!"STAFF".equals(u.getRole())||!"ACTIVE".equals(u.getStatus()))throw new NoSuchElementException("Active Staff not found");}finally{em.close();}}
 static Map<String,Object> map(StaffPayRate r){Map<String,Object>m=new LinkedHashMap<>();m.put("payRateId",r.getPayRateId());m.put("userId",r.getUser().getUserId());m.put("effectiveFrom",r.getEffectiveFrom());m.put("regularHourlyRate",r.getRegularHourlyRate());m.put("overtimeHourlyRate",r.getOvertimeHourlyRate());m.put("createdBy",r.getCreatedBy().getUserId());m.put("createdAt",r.getCreatedAt());return m;}
 public static class DuplicateRate extends RuntimeException{public DuplicateRate(){super("Pay rate already exists for effective date");}}
 public static class MissingRate extends RuntimeException{public MissingRate(){super("No effective pay rate for shift date");}}
}
