package servlet;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import jakarta.servlet.http.*;
import service.AdminInventoryService;

class AdminInventoryMutationServletTest {
    @Test void adjustmentZeroReturns400WithoutCallingService() throws Exception {
        RecordingService service=new RecordingService();
        AdminInventoryItemAdjustmentServlet servlet=new AdminInventoryItemAdjustmentServlet(service){protected int requireAdmin(HttpServletRequest q,HttpServletResponse p){return 7;}};
        Capture capture=new Capture();servlet.doPost(request(body("0")),response(capture));
        assertEquals(400,capture.status);assertEquals(0,service.calls);
    }

    @Test void adjustmentAcceptsExactPositiveAndNegativeFractionalBoundaries() throws Exception {
        for(String quantity:new String[]{"0.0001","-0.0001"}){
            RecordingService service=new RecordingService();
            AdminInventoryItemAdjustmentServlet servlet=new AdminInventoryItemAdjustmentServlet(service){protected int requireAdmin(HttpServletRequest q,HttpServletResponse p){return 7;}};
            Capture capture=new Capture();servlet.doPost(request(body(quantity)),response(capture));
            assertEquals(1,service.calls,quantity);assertEquals(new BigDecimal(quantity).setScale(4),service.quantity,quantity);
        }
    }

    private String body(String quantity){return "{\"inventoryItemId\":1,\"quantity\":"+quantity+",\"expectedOnHandQuantity\":2,\"reason\":\"COUNT\"}";}
    private HttpServletRequest request(String body){return(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->m.getName().equals("getReader")?new BufferedReader(new StringReader(body)):def(m.getReturnType()));}
    private HttpServletResponse response(Capture c){return(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{if(m.getName().equals("setStatus"))c.status=(int)a[0];if(m.getName().equals("getWriter"))return c.writer;return def(m.getReturnType());});}
    private static Object def(Class<?>t){if(!t.isPrimitive())return null;if(t==boolean.class)return false;if(t==char.class)return'\0';return 0;}
    private static class Capture{int status;StringWriter body=new StringWriter();PrintWriter writer=new PrintWriter(body);}
    private static class RecordingService extends AdminInventoryService{int calls;BigDecimal quantity;public Map<String,Object> mutate(int id,BigDecimal quantity,BigDecimal expected,String reason,String note,int adminId){calls++;this.quantity=quantity;return Map.of();}}
}
