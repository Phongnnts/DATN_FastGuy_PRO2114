package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import service.StockCountService;
import utils.ApiResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@WebServlet("/api/admin/inventory/stock-counts/*")
public class StockCountServlet extends HttpServlet {
    private final StockCountService service;
    public StockCountServlet(){this(new StockCountService());} StockCountServlet(StockCountService value){service=value;}
    protected void doGet(HttpServletRequest q,HttpServletResponse p)throws IOException{if(!authorized(q,p))return;try{String info=q.getPathInfo();if(info==null||"/".equals(info)){ApiResponse.ok(p,service.list());return;}String[]parts=path(info);if(!parts[1].isEmpty())throw new IllegalArgumentException("Invalid path");ApiResponse.ok(p,service.get(Integer.parseInt(parts[0])));}catch(NoSuchElementException e){ApiResponse.error(p,e.getMessage(),404);}catch(IllegalArgumentException e){ApiResponse.error(p,e.getMessage(),400);}catch(RuntimeException e){ApiResponse.error(p,"Internal server error",500);}}
    protected void doPost(HttpServletRequest q,HttpServletResponse p)throws IOException{int user=admin(q,p);if(user<0)return;try{String info=q.getPathInfo();if(info==null||"/".equals(info)){Map<String,Object>b=AdminInventoryItemServlet.body(q);AdminInventoryItemServlet.requireExactKeys(b,Set.of("countDate","frequency"));Map<String,Object>result=service.create(LocalDate.parse(AdminInventoryItemServlet.str(b,"countDate")),AdminInventoryItemServlet.str(b,"frequency"),user);p.setStatus(201);ApiResponse.ok(p,result);}else{String[]parts=path(info);if(!"approve".equals(parts[1]))throw new IllegalArgumentException("Invalid path");ApiResponse.ok(p,service.approve(Integer.parseInt(parts[0]),user));}}catch(NoSuchElementException e){ApiResponse.error(p,e.getMessage(),404);}catch(IllegalStateException e){ApiResponse.error(p,e.getMessage(),409);}catch(IllegalArgumentException|java.time.DateTimeException e){ApiResponse.error(p,e.getMessage(),400);}catch(RuntimeException e){ApiResponse.error(p,"Internal server error",500);}}
    @SuppressWarnings("unchecked") protected void doPut(HttpServletRequest q,HttpServletResponse p)throws IOException{if(!authorized(q,p))return;try{String[]parts=path(q.getPathInfo());if(!parts[1].isEmpty())throw new IllegalArgumentException("Invalid path");Map<String,Object>b=AdminInventoryItemServlet.body(q);AdminInventoryItemServlet.requireExactKeys(b,Set.of("items"));if(!(b.get("items") instanceof List<?>items))throw new IllegalArgumentException("Invalid items");ApiResponse.ok(p,service.update(Integer.parseInt(parts[0]),(List<Map<String,Object>>)(List<?>)items));}catch(NoSuchElementException e){ApiResponse.error(p,e.getMessage(),404);}catch(IllegalStateException e){ApiResponse.error(p,e.getMessage(),409);}catch(IllegalArgumentException e){ApiResponse.error(p,e.getMessage(),400);}catch(RuntimeException e){ApiResponse.error(p,"Internal server error",500);}}
    static String[] path(String value){return GoodsReceiptServlet.path(value);} protected int admin(HttpServletRequest q,HttpServletResponse p)throws IOException{return AdminApiAuth.require(q,p,tokenReader());} protected boolean authorized(HttpServletRequest q,HttpServletResponse p)throws IOException{return admin(q,p)>=0;} protected AdminApiAuth.TokenReader tokenReader(){return AdminApiAuth.jwt();}
}
