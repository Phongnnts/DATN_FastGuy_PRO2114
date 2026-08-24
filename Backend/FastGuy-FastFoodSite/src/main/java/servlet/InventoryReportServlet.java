package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import service.InventoryReportService;
import utils.ApiResponse;
import java.io.IOException;
import java.time.*;

@WebServlet("/api/admin/inventory/reports/*")
public class InventoryReportServlet extends HttpServlet {
    private final InventoryReportService service;
    public InventoryReportServlet(){this(new InventoryReportService());} InventoryReportServlet(InventoryReportService value){service=value;}
    protected void doGet(HttpServletRequest q,HttpServletResponse p)throws IOException{if(!authorized(q,p))return;try{String path=q.getPathInfo();if("/menu-cost".equals(path)){ApiResponse.ok(p,service.menuCost());return;}if(!"/summary".equals(path)&&!"/item-loss".equals(path))throw new IllegalArgumentException("Invalid path");String from=q.getParameter("fromDate"),to=q.getParameter("toDate");if(from==null||to==null)throw new IllegalArgumentException("fromDate and toDate are required");LocalDate start=LocalDate.parse(from),end=LocalDate.parse(to);ApiResponse.ok(p,"/summary".equals(path)?service.summary(start,end):service.itemLoss(start,end));}catch(IllegalArgumentException|DateTimeException e){ApiResponse.error(p,e.getMessage(),400);}catch(RuntimeException e){ApiResponse.error(p,"Internal server error",500);}}
    protected boolean authorized(HttpServletRequest q,HttpServletResponse p)throws IOException{return AdminApiAuth.require(q,p,AdminApiAuth.jwt())>=0;}
}
