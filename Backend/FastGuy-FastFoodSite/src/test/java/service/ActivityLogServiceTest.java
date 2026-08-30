package service;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ActivityLogServiceTest {
    @Test void exposesExactlyContractActionsAndRejectsUnknownMetadata() {
        assertEquals(Set.of("ORDER_CANCELLED","ORDER_REFUND_RECORDED","DELIVERY_ATTEMPT_OVERRIDDEN","ATTENDANCE_APPROVED","STAFF_PAY_RATE_CREATED","STOCK_COUNT_APPROVED"),ActivityLogService.ACTIONS);
        assertThrows(IllegalArgumentException.class,()->ActivityLogService.metadata("ORDER_REFUND_RECORDED",Map.of("refundReference","secret")));
        assertDoesNotThrow(()->ActivityLogService.metadata("ORDER_REFUND_RECORDED",Map.of("refundStatus","REFUNDED","refundAmount","100.00")));
    }

    @Test void productionHooksAppendBeforeCommitAndEntityIsRegistered() throws Exception {
        String root="src/main/java/";
        for(String file:new String[]{"service/RefundService.java","service/OrderTransitionService.java","service/WorkShiftService.java","service/StaffPayRateService.java","service/StockCountService.java"}){
            String source=Files.readString(Path.of(root+file));
            assertTrue(source.contains("activityLogService.append(em"),file);
            assertTrue(source.indexOf("activityLogService.append(em")<source.lastIndexOf("getTransaction().commit()"),file);
        }
        String persistence=Files.readString(Path.of("src/main/resources/META-INF/persistence.xml"));
        assertTrue(persistence.contains("<class>entity.ActivityLog</class>"));
    }
}
