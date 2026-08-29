package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import entity.User;
import jakarta.persistence.EntityManager;
import service.OperatingFinanceService;
import utils.DatabaseUtil;

class AdminReportsR5IT {
    @Test
    void disposableDatabaseFiltersManualSalaryAndCalculatesEstimatedResult() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("FASTGUY_DISPOSABLE_DB")));
        EntityManager em = DatabaseUtil.getEntityManager();
        int userId = 0;
        int expenseId = 0;
        try {
            assertEquals("FastGuyDB_Attendance061_Test", em.createNativeQuery("SELECT DB_NAME()").getSingleResult());
            LocalDate date = LocalDate.of(2091, 2, 15);
            em.getTransaction().begin();
            User user = new User();
            user.setRole("ADMIN"); user.setEmail("r5-" + System.nanoTime() + "@test.local"); user.setPhone("8" + String.format("%09d", Math.abs(System.nanoTime() % 1000000000L)));
            user.setPasswordHash("test"); user.setFullName("R5 Integration"); user.setStatus("ACTIVE"); em.persist(user); em.flush(); userId = user.getUserId();
            em.getTransaction().commit();

            OperatingFinanceService service = new OperatingFinanceService();
            Map<String,Object> expense = service.createExpense(date, "SALARY", "R5 manual salary", new BigDecimal("120.00"), userId);
            expenseId = (Integer) expense.get("expenseId");
            List<Map<String,Object>> included = service.listExpenses(date, date);
            assertEquals(1, included.size());
            assertEquals("SALARY", String.valueOf(included.get(0).get("category")));
            assertEquals(0, service.listExpenses(date.plusDays(1), date.plusDays(1)).size());

            Map<String,Object> report = service.operatingProfit(date, date);
            assertEquals(new BigDecimal("120.00"), report.get("storeExpenses"));
            assertEquals(new BigDecimal("-120.00"), report.get("estimatedOperatingResult"));
            assertEquals(Boolean.TRUE, report.get("includesManualSalary"));
        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.getTransaction().begin();
            if (expenseId > 0) em.createNativeQuery("DELETE OperatingExpense WHERE expense_id=:id").setParameter("id", expenseId).executeUpdate();
            if (userId > 0) em.createNativeQuery("DELETE Users WHERE user_id=:id").setParameter("id", userId).executeUpdate();
            em.getTransaction().commit();
            em.close(); DatabaseUtil.close();
        }
    }
}
