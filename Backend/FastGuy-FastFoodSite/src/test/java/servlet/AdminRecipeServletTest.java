package servlet;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import jakarta.servlet.http.*;
import service.AdminRecipeService;

class AdminRecipeServletTest {
    @Test void nullWrongAndUnknownRecipeItemShapesReturn400() throws Exception {
        AdminRecipeServlet servlet=authorized();
        for(String body:new String[]{"{\"yieldQuantity\":1,\"active\":true,\"items\":[null]}","{\"yieldQuantity\":1,\"active\":true,\"items\":[1]}","{\"yieldQuantity\":1,\"active\":true,\"items\":[{\"inventoryItemId\":1,\"quantity\":1,\"extra\":true}]}","{\"inventoryMode\":\"INGREDIENT\",\"yieldQuantity\":1,\"active\":true,\"items\":[{\"inventoryItemId\":1,\"quantity\":1}]}"}){
            Capture capture=new Capture();servlet.doPut(request("/1/recipe",body),response(capture));assertEquals(400,capture.status,body);
        }
    }

    @Test void requiresAdminAndRoutesRecipeSettingsAndCapacity() throws Exception {
        StubService service=new StubService();
        AdminRecipeServlet secured=new AdminRecipeServlet(service);
        Capture denied=new Capture();secured.doGet(request("/1/inventory-capacity",null),response(denied));assertEquals(401,denied.status);

        AdminRecipeServlet servlet=authorized(service);
        for(String path:new String[]{"/1/recipe","/1/inventory-settings","/1/inventory-capacity"}){
            Capture capture=new Capture();servlet.doGet(request(path,null),response(capture));capture.writer.flush();assertEquals(0,capture.status,path);assertTrue(capture.body.toString().contains("\"kind\""),path);
        }
        assertEquals(List.of("recipe","settings","capacity"),service.calls);
    }

    @Test void settingsPutUsesExactBodyAndRecipePutDoesNotAcceptInventoryMode() throws Exception {
        StubService service=new StubService();AdminRecipeServlet servlet=authorized(service);
        Capture settings=new Capture();servlet.doPut(request("/2/inventory-settings","{\"inventoryMode\":\"UNTRACKED\",\"expectedUpdatedAt\":\"2026-08-24T10:11:12\"}"),response(settings));assertEquals("UNTRACKED",service.mode);assertEquals(LocalDateTime.parse("2026-08-24T10:11:12"),service.expected);
        Capture unknown=new Capture();servlet.doPut(request("/2/inventory-settings","{\"inventoryMode\":\"UNTRACKED\",\"extra\":1}"),response(unknown));assertEquals(400,unknown.status);
        Capture recipe=new Capture();servlet.doPut(request("/2/recipe","{\"yieldQuantity\":2,\"active\":true,\"items\":[{\"inventoryItemId\":1,\"quantity\":4}],\"expectedUpdatedAt\":\"2026-08-24T10:11:12\"}"),response(recipe));assertEquals(0,new BigDecimal("2").compareTo(service.yield));
        Capture create=new Capture();servlet.doPut(request("/2/recipe","{\"yieldQuantity\":2,\"active\":true,\"items\":[{\"inventoryItemId\":1,\"quantity\":4}],\"expectedUpdatedAt\":null}"),response(create));assertEquals(0,create.status);assertNull(service.expected);
    }

    @Test void ingredientWithoutValidRecipeReturns409AndMissingVariant404() throws Exception {
        AdminRecipeServlet conflict=authorized(new StubService(){public Map<String,Object> updateSettings(int id,String mode,LocalDateTime expected){throw new AdminRecipeService.ModeNotReadyException(AdminRecipeService.INGREDIENT_NOT_READY);}});
        Capture c=new Capture();conflict.doPut(request("/1/inventory-settings","{\"inventoryMode\":\"INGREDIENT\",\"expectedUpdatedAt\":\"2026-08-24T10:11:12\"}"),response(c));c.writer.flush();assertEquals(409,c.status);assertTrue(c.body.toString().contains(AdminRecipeService.INGREDIENT_NOT_READY));
        AdminRecipeServlet missing=authorized(new StubService(){public Map<String,Object> settings(int id){throw new NoSuchElementException("Variant not found");}});
        c=new Capture();missing.doGet(request("/1/inventory-settings",null),response(c));assertEquals(404,c.status);
    }

    @Test void deactivatingIngredientModeRecipeReturns409WithDomainMessage() throws Exception {
        AdminRecipeServlet servlet=authorized(new StubService(){public Map<String,Object> replace(int id,BigDecimal yield,boolean active,List<Integer>ids,List<BigDecimal>quantities,LocalDateTime expected){throw new AdminRecipeService.ModeNotReadyException(AdminRecipeService.INGREDIENT_RECIPE_ACTIVE_REQUIRED);}});
        Capture capture=new Capture();servlet.doPut(request("/1/recipe","{\"yieldQuantity\":1,\"active\":false,\"items\":[{\"inventoryItemId\":1,\"quantity\":1}],\"expectedUpdatedAt\":\"2026-08-24T10:11:12\"}"),response(capture));capture.writer.flush();
        assertEquals(409,capture.status);assertTrue(capture.body.toString().contains(AdminRecipeService.INGREDIENT_RECIPE_ACTIVE_REQUIRED));
    }

    @Test void unexpectedPersistedModeFailureReturnsGeneric500() throws Exception {
        AdminRecipeServlet servlet=authorized(new StubService(){public Map<String,Object> capacity(int id){throw new IllegalStateException("Unknown persisted inventory mode");}});
        Capture capture=new Capture();servlet.doGet(request("/1/inventory-capacity",null),response(capture));capture.writer.flush();
        assertEquals(500,capture.status);assertTrue(capture.body.toString().contains("Internal server error"));assertFalse(capture.body.toString().contains("Unknown persisted"));
    }

    @Test void malformedZeroAndNegativeVariantPathsReturn400() throws Exception {
        AdminRecipeServlet servlet=authorized();
        for(String path:new String[]{"/abc/recipe","/0/recipe","/-1/recipe"}){
            Capture capture=new Capture();servlet.doGet(request(path,null),response(capture));assertEquals(400,capture.status,path);
        }
    }

    private AdminRecipeServlet authorized(){return authorized(new StubService());}
    private AdminRecipeServlet authorized(AdminRecipeService service){return new AdminRecipeServlet(service){protected boolean admin(HttpServletRequest q,HttpServletResponse p){return true;}};}
    private HttpServletRequest request(String path,String body){return(HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->switch(m.getName()){case"getPathInfo"->path;case"getReader"->new BufferedReader(new StringReader(body));default->def(m.getReturnType());});}
    private HttpServletResponse response(Capture c){return(HttpServletResponse)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletResponse.class},(p,m,a)->{if(m.getName().equals("setStatus"))c.status=(int)a[0];if(m.getName().equals("getWriter"))return c.writer;return def(m.getReturnType());});}
    private static Object def(Class<?>t){if(!t.isPrimitive())return null;if(t==boolean.class)return false;if(t==char.class)return'\0';return 0;}
    private static class Capture{int status;StringWriter body=new StringWriter();PrintWriter writer=new PrintWriter(body);}
    private static class StubService extends AdminRecipeService{
        final List<String> calls=new ArrayList<>();String mode;BigDecimal yield;LocalDateTime expected;
        public Map<String,Object> get(int id){calls.add("recipe");return Map.of("kind","recipe");}
        public Map<String,Object> settings(int id){calls.add("settings");return Map.of("kind","settings");}
        public Map<String,Object> capacity(int id){calls.add("capacity");return Map.of("kind","capacity");}
        public Map<String,Object> updateSettings(int id,String mode,LocalDateTime expected){this.mode=mode;this.expected=expected;return Map.of("variantId",id,"inventoryMode",mode);}
        public Map<String,Object> replace(int id,BigDecimal yield,boolean active,List<Integer>ids,List<BigDecimal>quantities,LocalDateTime expected){this.yield=yield;this.expected=expected;return Map.of();}
    }
}
