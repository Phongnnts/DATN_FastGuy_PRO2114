package servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import service.AdminInventoryService;
import service.AdminRecipeService;
import utils.*;

@WebServlet("/api/admin/product-variants/*")
public class AdminRecipeServlet extends HttpServlet {
    private final AdminRecipeService service;
    public AdminRecipeServlet(){this(new AdminRecipeService());}
    AdminRecipeServlet(AdminRecipeService service){this.service=service;}
    protected void doGet(HttpServletRequest q,HttpServletResponse p)throws IOException{if(!admin(q,p))return;try{Path x=path(q.getPathInfo());ApiResponse.ok(p,x.availability?service.availability(x.id):service.get(x.id));}catch(NoSuchElementException e){ApiResponse.error(p,e.getMessage(),404);}catch(IllegalArgumentException e){ApiResponse.error(p,e.getMessage(),400);}catch(RuntimeException e){ApiResponse.error(p,"Internal server error",500);}}
    protected void doPut(HttpServletRequest q,HttpServletResponse p)throws IOException{if(!admin(q,p))return;try{Path x=path(q.getPathInfo());if(x.availability)throw new IllegalArgumentException("Invalid path");Map<String,Object>b=AdminInventoryItemServlet.body(q);AdminInventoryItemServlet.requireKeys(b,Set.of("inventoryMode","yieldQuantity","active","items"));if(!(b.get("items") instanceof List<?> raw))throw new IllegalArgumentException("Invalid recipe items");List<Integer>ids=new ArrayList<>();List<BigDecimal>qs=new ArrayList<>();for(Object o:raw){if(!(o instanceof Map<?,?> m))throw new IllegalArgumentException("Invalid recipe item");if(!m.keySet().equals(Set.of("inventoryItemId","quantity")))throw new IllegalArgumentException("Unknown recipe item field");ids.add(AdminInventoryItemServlet.positiveInt(m.get("inventoryItemId"),"inventoryItemId"));qs.add(AdminInventoryService.decimal(m.get("quantity"),true));}ApiResponse.ok(p,service.replace(x.id,AdminInventoryItemServlet.str(b,"inventoryMode"),AdminInventoryItemServlet.dec(b,"yieldQuantity",true),AdminInventoryItemServlet.bool(b,"active"),ids,qs));}catch(NoSuchElementException e){ApiResponse.error(p,e.getMessage(),404);}catch(IllegalArgumentException e){ApiResponse.error(p,e.getMessage(),400);}catch(RuntimeException e){ApiResponse.error(p,"Internal server error",500);}}
    protected boolean admin(HttpServletRequest q,HttpServletResponse p)throws IOException{return AdminApiAuth.require(q,p,tokenReader())>=0;}
    protected AdminApiAuth.TokenReader tokenReader(){return AdminApiAuth.jwt();}
    private static Path path(String s){if(s==null||!s.matches("/[1-9]\\d*/(recipe|availability)"))throw new IllegalArgumentException("Invalid path");String[]p=s.substring(1).split("/");try{return new Path(Integer.parseInt(p[0]),p[1].equals("availability"));}catch(NumberFormatException e){throw new IllegalArgumentException("Invalid path");}}
    private record Path(int id,boolean availability){}
}
