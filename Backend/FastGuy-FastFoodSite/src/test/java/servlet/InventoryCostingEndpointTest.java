package servlet;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import service.*;

class InventoryCostingEndpointTest {
    @Test void mapsOpenApiInventoryCostingPaths() {
        assertEquals("/api/admin/inventory/receipts/*", GoodsReceiptServlet.class.getAnnotation(WebServlet.class).value()[0]);
        assertEquals("/api/admin/inventory/stock-counts/*", StockCountServlet.class.getAnnotation(WebServlet.class).value()[0]);
        assertEquals("/api/admin/inventory/reports/*", InventoryReportServlet.class.getAnnotation(WebServlet.class).value()[0]);
        assertEquals("/api/admin/inventory/analytics", InventoryAnalyticsServlet.class.getAnnotation(WebServlet.class).value()[0]);
    }

    @Test void parsesDocumentAndApprovePathsStrictly() {
        assertArrayEquals(new String[]{"12",""}, GoodsReceiptServlet.path("/12"));
        assertArrayEquals(new String[]{"12","approve"}, GoodsReceiptServlet.path("/12/approve"));
        assertThrows(IllegalArgumentException.class,()->GoodsReceiptServlet.path("/0"));
        assertThrows(IllegalArgumentException.class,()->GoodsReceiptServlet.path("/12/other"));
        assertArrayEquals(new String[]{"9","approve"}, StockCountServlet.path("/9/approve"));
    }


    @Test void getRejectsApproveActionInsteadOfReturningDocument() throws Exception {
        StubGoodsReceiptService service=new StubGoodsReceiptService();
        TestGoodsReceiptServlet servlet=new TestGoodsReceiptServlet(service);
        ResponseCapture capture=new ResponseCapture();
        servlet.get(request("/12/approve",Map.of()),response(capture));
        assertEquals(400,capture.status);
        assertFalse(service.got);

        StubStockCountService countService=new StubStockCountService();
        TestStockCountServlet countServlet=new TestStockCountServlet(countService);
        ResponseCapture countCapture=new ResponseCapture();
        countServlet.get(request("/9/approve",Map.of()),response(countCapture));
        assertEquals(400,countCapture.status);
        assertFalse(countService.got);
    }

    @Test void reportRoutesInventoryAnalyticsWithExactDayGranularity() throws Exception {
        StubReportService service=new StubReportService(false);
        TestInventoryAnalyticsServlet servlet=new TestInventoryAnalyticsServlet(service);
        ResponseCapture capture=new ResponseCapture();
        servlet.get(request(null,Map.of("fromDate","2026-09-01","toDate","2026-09-30","granularity","DAY")),response(capture));
        assertEquals(200,capture.status);
        assertTrue(service.analyticsCalled);
        ResponseCapture bad=new ResponseCapture();
        servlet.get(request(null,Map.of("fromDate","2026-09-01","toDate","2026-09-30","granularity","WEEK")),response(bad));
        assertEquals(400,bad.status);
    }

    @Test void reportMapsValidationTo400AndUnexpectedFailureToGeneric500() throws Exception {
        TestInventoryReportServlet validation=new TestInventoryReportServlet(new StubReportService(false));
        ResponseCapture bad=new ResponseCapture();
        validation.get(request("/summary",Map.of()),response(bad));
        assertEquals(400,bad.status);

        TestInventoryReportServlet failure=new TestInventoryReportServlet(new StubReportService(true));
        ResponseCapture error=new ResponseCapture();
        failure.get(request("/menu-cost",Map.of()),response(error));
        error.writer.flush();
        assertEquals(500,error.status);
        assertTrue(error.body.toString().contains("Internal server error"));
        assertFalse(error.body.toString().contains("database detail"));
    }

    private HttpServletRequest request(String path,Map<String,String> parameters){return (HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{HttpServletRequest.class},(p,m,a)->switch(m.getName()){case "getPathInfo"->path;case "getParameter"->parameters.get((String)a[0]);default->defaultValue(m.getReturnType());});}
    private HttpServletResponse response(ResponseCapture capture){return (HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{HttpServletResponse.class},(p,m,a)->{if("setStatus".equals(m.getName()))capture.status=(int)a[0];if("getWriter".equals(m.getName()))return capture.writer;return defaultValue(m.getReturnType());});}
    private Object defaultValue(Class<?>type){if(!type.isPrimitive())return null;if(type==boolean.class)return false;if(type==char.class)return '\0';return 0;}
    private static class TestGoodsReceiptServlet extends GoodsReceiptServlet{TestGoodsReceiptServlet(GoodsReceiptService s){super(s);}@Override protected boolean authorized(HttpServletRequest q,HttpServletResponse p){return true;}void get(HttpServletRequest q,HttpServletResponse p)throws Exception{doGet(q,p);}}
    private static class StubGoodsReceiptService extends GoodsReceiptService{boolean got;@Override public Map<String,Object> get(int id){got=true;return Map.of();}}
    private static class TestStockCountServlet extends StockCountServlet{TestStockCountServlet(StockCountService s){super(s);}@Override protected boolean authorized(HttpServletRequest q,HttpServletResponse p){return true;}void get(HttpServletRequest q,HttpServletResponse p)throws Exception{doGet(q,p);}}
    private static class StubStockCountService extends StockCountService{boolean got;@Override public Map<String,Object> get(int id){got=true;return Map.of();}}
    private static class TestInventoryReportServlet extends InventoryReportServlet{TestInventoryReportServlet(InventoryReportService s){super(s);}@Override protected boolean authorized(HttpServletRequest q,HttpServletResponse p){return true;}void get(HttpServletRequest q,HttpServletResponse p)throws Exception{doGet(q,p);}}
    private static class TestInventoryAnalyticsServlet extends InventoryAnalyticsServlet{TestInventoryAnalyticsServlet(InventoryReportService s){super(s);}@Override protected boolean authorized(HttpServletRequest q,HttpServletResponse p){return true;}void get(HttpServletRequest q,HttpServletResponse p)throws Exception{doGet(q,p);}}
    private static class StubReportService extends InventoryReportService{private final boolean fail;boolean analyticsCalled;StubReportService(boolean fail){this.fail=fail;}@Override public java.util.List<Map<String,Object>> menuCost(){if(fail)throw new IllegalStateException("database detail");return java.util.List.of();}@Override public Map<String,Object> analytics(java.time.LocalDate from,java.time.LocalDate to){analyticsCalled=true;return Map.of();}}
    private static class ResponseCapture{int status=200;StringWriter body=new StringWriter();PrintWriter writer=new PrintWriter(body);}
}
