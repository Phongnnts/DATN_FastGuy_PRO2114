package dao;

import java.time.LocalDate;
import java.util.List;
import entity.StaffPayRate;
import jakarta.persistence.*;
import utils.DatabaseUtil;

public class StaffPayRateDAO {
 public List<StaffPayRate> list(int userId){EntityManager em=DatabaseUtil.getEntityManager();try{return em.createQuery("SELECT r FROM StaffPayRate r WHERE r.user.userId=:id ORDER BY r.effectiveFrom DESC,r.payRateId DESC",StaffPayRate.class).setParameter("id",userId).getResultList();}finally{em.close();}}
 public StaffPayRate effective(int userId,LocalDate date){EntityManager em=DatabaseUtil.getEntityManager();try{return effective(em,userId,date,LockModeType.NONE);}finally{em.close();}}
 public StaffPayRate effective(EntityManager em,int userId,LocalDate date,LockModeType lock){return em.createQuery("SELECT r FROM StaffPayRate r WHERE r.user.userId=:id AND r.effectiveFrom<=:date ORDER BY r.effectiveFrom DESC,r.payRateId DESC",StaffPayRate.class).setParameter("id",userId).setParameter("date",date).setLockMode(lock).setMaxResults(1).getResultStream().findFirst().orElse(null);}
}
