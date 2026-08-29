package integration;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import service.AdminService;
import utils.DatabaseUtil;

class AdminDashboardIT {
    @Test void dashboardBuildsDecisionKpisOnDisposableDatabase() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("FASTGUY_DISPOSABLE_DB")));
        try {
            Map<String,Object> data=new AdminService().getDashboard();
            for(String field:new String[]{"deliveredOrdersToday","activeOrdersToday","aovToday","grossProfitToday","costComplete","attentionItems"}) assertTrue(data.containsKey(field),field);
        } finally { DatabaseUtil.close(); }
    }
}
