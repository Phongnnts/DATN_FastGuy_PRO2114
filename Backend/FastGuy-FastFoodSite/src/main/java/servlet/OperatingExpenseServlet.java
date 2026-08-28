package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import service.OperatingFinanceService;
import utils.ApiResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@WebServlet("/api/admin/operating-expenses/*")
public class OperatingExpenseServlet extends HttpServlet {
    private static final Set<String> KEYS=Set.of("expenseDate","category","description","amount");private final OperatingFinanceService service;
    public OperatingExpenseServlet(){this(new OperatingFinanceService());}OperatingExpenseServlet(OperatingFinanceService service){this.service=service;}
    protected void doGet(HttpServletRequest q,HttpServletResponse p)throws IOException{int user=admin(q,p);if(user<0)return;try{String path=q.getPathInfo();ApiResponse.ok(p,path==null||"/".equals(path)?service.listExpenses():service.getExpense(id(path)));}catch(NoSuchElementException e){ApiResponse.error(p,e.getMessage(),404);}catch(IllegalArgumentException e){ApiResponse.error(p,e.getMessage(),400);}catch(RuntimeException e){ApiResponse.error(p,"Internal server error",500);}}
    protected void doPost(HttpServletRequest q,HttpServletResponse p)throws IOException{int user=admin(q,p);if(user<0)return;try{Map<String,Object>b=body(q);exact(b,KEYS);p.setStatus(201);ApiResponse.ok(p,service.createExpense(date(b,"expenseDate"),str(b,"category"),str(b,"description"),money(b,"amount"),user));}catch(IllegalArgumentException|DateTimeException e){ApiResponse.error(p,e.getMessage(),400);}catch(RuntimeException e){ApiResponse.error(p,"Internal server error",500);}}
    protected void doPut(HttpServletRequest q,HttpServletResponse p)throws IOException{int user=admin(q,p);if(user<0)return;try{Map<String,Object>b=body(q);exact(b,KEYS);ApiResponse.ok(p,service.updateExpense(id(q.getPathInfo()),date(b,"expenseDate"),str(b,"category"),str(b,"description"),money(b,"amount")));}catch(NoSuchElementException e){ApiResponse.error(p,e.getMessage(),404);}catch(IllegalArgumentException|DateTimeException e){ApiResponse.error(p,e.getMessage(),400);}catch(RuntimeException e){ApiResponse.error(p,"Internal server error",500);}}
    protected void doDelete(HttpServletRequest q,HttpServletResponse p)throws IOException{int user=admin(q,p);if(user<0)return;try{service.deleteExpense(id(q.getPathInfo()));p.setStatus(204);}catch(NoSuchElementException e){ApiResponse.error(p,e.getMessage(),404);}catch(IllegalArgumentException e){ApiResponse.error(p,e.getMessage(),400);}catch(RuntimeException e){ApiResponse.error(p,"Internal server error",500);}}
    protected int admin(HttpServletRequest q,HttpServletResponse p)throws IOException{return AdminApiAuth.require(q,p,AdminApiAuth.jwt());}
    static Map<String,Object> body(HttpServletRequest q)throws IOException{try{Map<String,Object>b=utils.JsonUtil.getMapper().readValue(q.getReader(),Map.class);if(b==null)throw new IllegalArgumentException("Invalid JSON");return b;}catch(com.fasterxml.jackson.core.JacksonException e){throw new IllegalArgumentException("Invalid JSON");}}
    static void exact(Map<String,Object>b,Set<String>keys){if(!b.keySet().equals(keys))throw new IllegalArgumentException(keys.containsAll(b.keySet())?"Missing request field":"Unknown request field");}
    static int id(String path){if(path==null||!path.matches("/[1-9]\\d*"))throw new IllegalArgumentException("Invalid ID");try{return Integer.parseInt(path.substring(1));}catch(NumberFormatException e){throw new IllegalArgumentException("Invalid ID");}}
    static String str(Map<String,Object>b,String key){if(!(b.get(key)instanceof String value))throw new IllegalArgumentException("Invalid "+key);return value;}
    static LocalDate date(Map<String,Object>b,String key){return LocalDate.parse(str(b,key));}
    static BigDecimal money(Map<String,Object>b,String key){if(!(b.get(key)instanceof Number value))throw new IllegalArgumentException("Invalid "+key);return new BigDecimal(value.toString());}
    static int integer(Map<String,Object>b,String key){Object value=b.get(key);if(!(value instanceof Number number))throw new IllegalArgumentException("Invalid "+key);try{BigDecimal decimal=new BigDecimal(number.toString());if(decimal.stripTrailingZeros().scale()>0)return-1;return decimal.intValueExact();}catch(ArithmeticException e){return-1;}}
}
