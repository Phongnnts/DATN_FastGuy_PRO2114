package servlet;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import jakarta.servlet.http.*;
import service.AdminInventoryService;

class AdminInventoryItemServletTest {
    @Test void rejectsUnknownFieldsAndFractionalOrOverflowIds() {
        assertThrows(IllegalArgumentException.class, () -> AdminInventoryItemServlet.requireKeys(Map.of("name","x","extra",1), Set.of("name")));
        assertThrows(IllegalArgumentException.class, () -> AdminInventoryItemServlet.positiveInt(1.5, "inventoryItemId"));
        assertThrows(IllegalArgumentException.class, () -> AdminInventoryItemServlet.positiveInt(2147483648L, "inventoryItemId"));
        assertEquals(7, AdminInventoryItemServlet.positiveInt(7, "inventoryItemId"));
    }

    @Test void missingAuthReturns401AndSuccessUsesApiEnvelope() throws Exception {
        AdminInventoryItemServlet servlet=new AdminInventoryItemServlet(new StubService());
        Capture missing=new Capture(); servlet.doGet(request(null),response(missing)); assertEquals(401,missing.status);
        Capture ok=new Capture(); servlet=new AdminInventoryItemServlet(new StubService()){protected boolean admin(HttpServletRequest q,HttpServletResponse p){return true;}};
        servlet.doGet(request("Bearer x"),response(ok));ok.writer.flush();assertTrue(ok.body.toString().contains("\"status\":\"success\""));assertTrue(ok.body.toString().contains("\"data\":["));
    }

    @Test void malformedJsonReturns400AndUnexpectedDatabaseFailureReturns500() throws Exception {
        AdminInventoryItemServlet servlet=authorized(new StubService());
        Capture malformed=new Capture();servlet.doPost(request("Bearer x","{"),response(malformed));assertEquals(400,malformed.status);
        Capture malformedPut=new Capture();servlet.doPut(request("Bearer x","/1","{"),response(malformedPut));assertEquals(400,malformedPut.status);
        servlet=authorized(new FailingService());
        Capture failed=new Capture();servlet.doPost(request("Bearer x","{\"inventoryCode\":\"SALT\",\"name\":\"Salt\",\"itemType\":\"INGREDIENT\",\"baseUnit\":\"G\",\"minimumQuantity\":0,\"countFrequency\":\"WEEKLY\",\"active\":true}"),response(failed));assertEquals(500,failed.status);
    }

    private AdminInventoryItemServlet authorized(AdminInventoryService service){return new AdminInventoryItemServlet(service){protected boolean admin(HttpServletRequest q,HttpServletResponse p){return true;}};}
    private HttpServletRequest request(String auth){return request(auth,null,null);}
    private HttpServletRequest request(String auth,String body){return request(auth,null,body);}
    private HttpServletRequest request(String auth,String path,String body){return(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->switch(m.getName()){case"getHeader"->auth;case"getPathInfo"->path;case"getReader"->new BufferedReader(new StringReader(body));default->def(m.getReturnType());});}
    private HttpServletResponse response(Capture c){return(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{if(m.getName().equals("setStatus"))c.status=(int)a[0];if(m.getName().equals("getWriter"))return c.writer;return def(m.getReturnType());});}
    private static Object def(Class<?>t){if(!t.isPrimitive())return null;if(t==boolean.class)return false;if(t==char.class)return'\0';return 0;}
    private static class Capture{int status;StringWriter body=new StringWriter();PrintWriter writer=new PrintWriter(body);}
    private static class StubService extends AdminInventoryService{public List<Map<String,Object>> list(){return List.of(Map.of("inventoryItemId",1,"onHandQuantity",new BigDecimal("1.0000")));}public Map<String,Object> create(String name,String type,String unit,BigDecimal minimum,boolean active){return Map.of();}}
    private static class FailingService extends StubService{public Map<String,Object> create(String code,String name,String type,String unit,BigDecimal minimum,String frequency,boolean active){throw new RuntimeException("database unavailable");}}
}
