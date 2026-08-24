package servlet;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import jakarta.servlet.http.*;
import service.AdminRecipeService;

class AdminRecipeServletTest {
    @Test void nullWrongAndUnknownRecipeItemShapesReturn400() throws Exception {
        AdminRecipeServlet servlet=authorized();
        for(String body:new String[]{"{\"inventoryMode\":\"INGREDIENT\",\"yieldQuantity\":1,\"active\":true,\"items\":[null]}","{\"inventoryMode\":\"INGREDIENT\",\"yieldQuantity\":1,\"active\":true,\"items\":[1]}","{\"inventoryMode\":\"INGREDIENT\",\"yieldQuantity\":1,\"active\":true,\"items\":[{\"inventoryItemId\":1,\"quantity\":1,\"extra\":true}]}"}){
            Capture capture=new Capture();servlet.doPut(request("/1/recipe",body),response(capture));assertEquals(400,capture.status,body);
        }
    }

    @Test void malformedZeroAndNegativeVariantPathsReturn400() throws Exception {
        AdminRecipeServlet servlet=authorized();
        for(String path:new String[]{"/abc/recipe","/0/recipe","/-1/recipe"}){
            Capture capture=new Capture();servlet.doGet(request(path,null),response(capture));assertEquals(400,capture.status,path);
        }
    }

    private AdminRecipeServlet authorized(){return new AdminRecipeServlet(new AdminRecipeService()){protected boolean admin(HttpServletRequest q,HttpServletResponse p){return true;}};}
    private HttpServletRequest request(String path,String body){return(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->switch(m.getName()){case"getPathInfo"->path;case"getReader"->new BufferedReader(new StringReader(body));default->def(m.getReturnType());});}
    private HttpServletResponse response(Capture c){return(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{if(m.getName().equals("setStatus"))c.status=(int)a[0];if(m.getName().equals("getWriter"))return c.writer;return def(m.getReturnType());});}
    private static Object def(Class<?>t){if(!t.isPrimitive())return null;if(t==boolean.class)return false;if(t==char.class)return'\0';return 0;}
    private static class Capture{int status;StringWriter body=new StringWriter();PrintWriter writer=new PrintWriter(body);}
}
