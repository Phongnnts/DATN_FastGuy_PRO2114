package dao;

import entity.*;
import jakarta.persistence.*;
import utils.DatabaseUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class OperatingFinanceDAO {
    public List<OperatingExpense> listExpenses(){EntityManager em=DatabaseUtil.getEntityManager();try{return em.createQuery("SELECT e FROM OperatingExpense e JOIN FETCH e.createdBy ORDER BY e.expenseDate DESC,e.expenseId DESC",OperatingExpense.class).getResultList();}finally{em.close();}}
    public List<OperatingExpense> listExpenses(LocalDate from,LocalDate to){EntityManager em=DatabaseUtil.getEntityManager();try{return em.createQuery("SELECT e FROM OperatingExpense e JOIN FETCH e.createdBy WHERE e.expenseDate>=:from AND e.expenseDate<=:to ORDER BY e.expenseDate DESC,e.expenseId DESC",OperatingExpense.class).setParameter("from",from).setParameter("to",to).getResultList();}finally{em.close();}}
    public OperatingExpense findExpense(int id){EntityManager em=DatabaseUtil.getEntityManager();try{return em.createQuery("SELECT e FROM OperatingExpense e JOIN FETCH e.createdBy WHERE e.expenseId=:id",OperatingExpense.class).setParameter("id",id).getResultStream().findFirst().orElse(null);}finally{em.close();}}
    public OperatingExpense saveExpense(OperatingExpense value){return save(value);}
    public boolean deleteExpense(int id){EntityManager em=DatabaseUtil.getEntityManager();try{em.getTransaction().begin();OperatingExpense value=em.find(OperatingExpense.class,id);if(value==null){em.getTransaction().rollback();return false;}em.remove(value);em.getTransaction().commit();return true;}catch(RuntimeException e){rollback(em);throw e;}finally{em.close();}}
    public BigDecimal grossRevenue(LocalDate from,LocalDate to){return new OrdersDAO().sumRevenueDecimalByDateRange(from.atStartOfDay(),to.plusDays(1).atStartOfDay());}
    public BigDecimal refundTotal(LocalDate from,LocalDate to){return new OrdersDAO().sumRefundsDecimalInRange(from.atStartOfDay(),to.plusDays(1).atStartOfDay());}
    public BigDecimal sumExpenses(LocalDate from,LocalDate to){EntityManager em=DatabaseUtil.getEntityManager();try{return em.createQuery("SELECT COALESCE(SUM(e.amount),0) FROM OperatingExpense e WHERE e.expenseDate>=:from AND e.expenseDate<=:to",BigDecimal.class).setParameter("from",from).setParameter("to",to).getSingleResult();}finally{em.close();}}
    public List<FixedAsset> listAssets(){EntityManager em=DatabaseUtil.getEntityManager();try{return em.createQuery("SELECT a FROM FixedAsset a JOIN FETCH a.createdBy ORDER BY a.assetId DESC",FixedAsset.class).getResultList();}finally{em.close();}}
    public FixedAsset findAsset(int id){EntityManager em=DatabaseUtil.getEntityManager();try{return em.createQuery("SELECT a FROM FixedAsset a JOIN FETCH a.createdBy WHERE a.assetId=:id",FixedAsset.class).setParameter("id",id).getResultStream().findFirst().orElse(null);}finally{em.close();}}
    public FixedAsset saveAsset(FixedAsset value){return save(value);}
    public List<FixedAsset> listAssetsForDepreciation(LocalDate to){EntityManager em=DatabaseUtil.getEntityManager();try{return em.createQuery("SELECT a FROM FixedAsset a WHERE a.depreciationStartDate<=:to",FixedAsset.class).setParameter("to",to).getResultList();}finally{em.close();}}
    public User userReference(int id){EntityManager em=DatabaseUtil.getEntityManager();try{return em.getReference(User.class,id);}finally{em.close();}}
    private static <T>T save(T value){EntityManager em=DatabaseUtil.getEntityManager();try{em.getTransaction().begin();T saved=em.merge(value);em.getTransaction().commit();return saved;}catch(RuntimeException e){rollback(em);throw e;}finally{em.close();}}
    private static void rollback(EntityManager em){if(em.getTransaction().isActive())em.getTransaction().rollback();}
}
