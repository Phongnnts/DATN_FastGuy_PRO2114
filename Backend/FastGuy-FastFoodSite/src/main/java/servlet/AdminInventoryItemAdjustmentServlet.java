package servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import service.AdminInventoryService;
import exception.InventoryItemConflictException;
import utils.*;

@WebServlet("/api/admin/inventory/transactions/adjustments")
public class AdminInventoryItemAdjustmentServlet extends HttpServlet {
    private final AdminInventoryService service;
    public AdminInventoryItemAdjustmentServlet(){this(new AdminInventoryService());}
    AdminInventoryItemAdjustmentServlet(AdminInventoryService service){this.service=service;}
    protected void doPost(HttpServletRequest q,HttpServletResponse p)throws IOException{int uid=requireAdmin(q,p);if(uid<0)return;try{Map<String,Object>b=AdminInventoryItemServlet.body(q);AdminInventoryItemServlet.requireKeys(b,Set.of("inventoryItemId","quantity","expectedOnHandQuantity","reason","note"));int id=AdminInventoryItemServlet.positiveInt(b.get("inventoryItemId"),"inventoryItemId");BigDecimal quantity=signedDecimal(b.get("quantity"));BigDecimal expected=AdminInventoryItemServlet.dec(b,"expectedOnHandQuantity",false);ApiResponse.ok(p,service.mutate(id,quantity,expected,AdminInventoryItemServlet.str(b,"reason"),note(b),uid));}catch(InventoryItemConflictException e){AdminInventoryItemServlet.conflict(p,e);}catch(NoSuchElementException e){ApiResponse.error(p,e.getMessage(),404);}catch(IllegalArgumentException e){ApiResponse.error(p,e.getMessage(),400);}catch(RuntimeException e){ApiResponse.error(p,"Internal server error",500);}}
    protected int requireAdmin(HttpServletRequest q,HttpServletResponse p)throws IOException{return AdminApiAuth.require(q,p,tokenReader());}
    protected AdminApiAuth.TokenReader tokenReader(){return AdminApiAuth.jwt();}
    private static BigDecimal signedDecimal(Object value){try{BigDecimal d=new BigDecimal(String.valueOf(value));if(d.stripTrailingZeros().scale()>4||d.signum()==0||d.abs().compareTo(new BigDecimal("999999999999999.9999"))>0)throw new IllegalArgumentException("Invalid quantity");return d.setScale(4);}catch(NumberFormatException e){throw new IllegalArgumentException("Invalid quantity");}}
    private static String note(Map<String,Object>b){Object n=b.get("note");if(n==null)return null;if(!(n instanceof String s)||s.length()>500)throw new IllegalArgumentException("Invalid note");return s;}
}
