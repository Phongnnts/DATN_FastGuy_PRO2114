package servlet;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import jakarta.servlet.http.*;

class AdminInventoryAuthBehaviorTest {
    @Test void malformedTokenIs401AndWrongRoleIs403() throws Exception {
        Capture malformed=new Capture();int a=AdminApiAuth.require(request("Bearer bad"),response(malformed),new AdminApiAuth.TokenReader(){public String role(String t){throw new IllegalArgumentException();}public int userId(String t){return 1;}});assertEquals(-1,a);assertEquals(401,malformed.status);
        Capture forbidden=new Capture();int b=AdminApiAuth.require(request("Bearer valid"),response(forbidden),new AdminApiAuth.TokenReader(){public String role(String t){return "USER";}public int userId(String t){return 1;}});assertEquals(-1,b);assertEquals(403,forbidden.status);
    }
    private HttpServletRequest request(String auth){return(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->m.getName().equals("getHeader")?auth:null);}
    private HttpServletResponse response(Capture c){return(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{if(m.getName().equals("setStatus"))c.status=(int)a[0];if(m.getName().equals("getWriter"))return c.writer;return null;});}
    private static class Capture{int status;PrintWriter writer=new PrintWriter(new StringWriter());}
}
