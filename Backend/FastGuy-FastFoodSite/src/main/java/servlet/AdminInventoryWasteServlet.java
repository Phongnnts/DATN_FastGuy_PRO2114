package servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import exception.InventoryItemConflictException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.AdminInventoryService;
import utils.ApiResponse;

@WebServlet("/api/admin/inventory/transactions/waste")
public class AdminInventoryWasteServlet extends HttpServlet {
    private final AdminInventoryService service;
    public AdminInventoryWasteServlet(){this(new AdminInventoryService());}
    AdminInventoryWasteServlet(AdminInventoryService service){this.service=service;}
    protected void doPost(HttpServletRequest q,HttpServletResponse p)throws IOException{int uid=AdminApiAuth.require(q,p,AdminApiAuth.jwt());if(uid<0)return;try{Map<String,Object>b=AdminInventoryItemServlet.body(q);AdminInventoryItemServlet.requireExactKeys(b,Set.of("inventoryItemId","quantity","expectedOnHandQuantity","reason","note"));int id=AdminInventoryItemServlet.positiveInt(b.get("inventoryItemId"),"inventoryItemId");BigDecimal quantity=AdminInventoryItemServlet.dec(b,"quantity",true);BigDecimal expected=AdminInventoryItemServlet.dec(b,"expectedOnHandQuantity",false);Object note=b.get("note");ApiResponse.ok(p,service.waste(id,quantity,expected,AdminInventoryItemServlet.str(b,"reason"),note==null?null:String.valueOf(note),uid));}catch(InventoryItemConflictException e){AdminInventoryItemServlet.conflict(p,e);}catch(NoSuchElementException e){ApiResponse.error(p,e.getMessage(),404);}catch(IllegalArgumentException e){ApiResponse.error(p,e.getMessage(),400);}catch(RuntimeException e){ApiResponse.error(p,"Internal server error",500);}}
}
