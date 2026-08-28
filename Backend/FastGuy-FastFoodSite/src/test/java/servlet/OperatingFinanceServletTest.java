package servlet;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.lang.reflect.Proxy;
import java.util.*;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import service.OperatingFinanceService;

class OperatingFinanceServletTest {
    @Test void expensesRequireAdminAndRejectUnknownKeys() throws Exception {
        Capture unauthorized=new Capture();new OperatingExpenseServlet(new OperatingFinanceService()).doGet(request(null,null,null,Map.of()),response(unauthorized));assertEquals(401,unauthorized.status);
        Capture invalid=new Capture();authorizedExpenses().doPost(request("Bearer x",null,"{\"expenseDate\":\"2024-01-01\",\"category\":\"RENT\",\"description\":\"Rent\",\"amount\":1,\"extra\":true}",Map.of()),response(invalid));assertEquals(400,invalid.status);
    }

    @Test void financeServletsReturnGeneric500WithoutInternalMessage() throws Exception {
        OperatingFinanceService failing=new OperatingFinanceService(){public List<Map<String,Object>> listExpenses(){throw new RuntimeException("jdbc password=secret");}};
        OperatingExpenseServlet servlet=new OperatingExpenseServlet(failing){protected int admin(HttpServletRequest q,HttpServletResponse p){return 1;}};
        Capture capture=new Capture();servlet.doGet(request("Bearer x",null,null,Map.of()),response(capture));capture.writer.flush();assertEquals(500,capture.status);assertFalse(capture.body.toString().contains("secret"));assertTrue(capture.body.toString().contains("Internal server error"));
    }

    @Test void reportRequiresExactDatesAndUsesSuccessEnvelope() throws Exception {
        OperatingFinanceService service=new OperatingFinanceService(){public Map<String,Object> operatingProfit(java.time.LocalDate from,java.time.LocalDate to){return Map.of("fromDate",from.toString(),"toDate",to.toString());}};
        OperatingProfitReportServlet servlet=new OperatingProfitReportServlet(service){protected int admin(HttpServletRequest q,HttpServletResponse p){return 1;}};
        Capture missing=new Capture();servlet.doGet(request("Bearer x",null,null,Map.of("fromDate","2024-01-01")),response(missing));assertEquals(400,missing.status);
        Capture ok=new Capture();servlet.doGet(request("Bearer x",null,null,Map.of("fromDate","2024-01-01","toDate","2024-01-31")),response(ok));ok.writer.flush();assertTrue(ok.body.toString().contains("\"status\":\"success\""));
    }

    private OperatingExpenseServlet authorizedExpenses(){return new OperatingExpenseServlet(new OperatingFinanceService()){protected int admin(HttpServletRequest q,HttpServletResponse p){return 1;}};}
    private HttpServletRequest request(String auth,String path,String body,Map<String,String>params){return(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->switch(m.getName()){case"getHeader"->auth;case"getPathInfo"->path;case"getReader"->new BufferedReader(new StringReader(body==null?"":body));case"getParameter"->params.get((String)a[0]);default->def(m.getReturnType());});}
    private HttpServletResponse response(Capture c){return(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{if(m.getName().equals("setStatus"))c.status=(int)a[0];if(m.getName().equals("getWriter"))return c.writer;return def(m.getReturnType());});}
    private static Object def(Class<?>t){if(!t.isPrimitive())return null;if(t==boolean.class)return false;if(t==char.class)return'\0';return 0;}
    private static class Capture{int status=200;StringWriter body=new StringWriter();PrintWriter writer=new PrintWriter(body);}
}
