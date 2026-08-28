package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import service.OperatingFinanceService;
import utils.ApiResponse;
import java.io.IOException;
import java.time.*;

@WebServlet("/api/admin/reports/operating-profit")
public class OperatingProfitReportServlet extends HttpServlet {
    private final OperatingFinanceService service;public OperatingProfitReportServlet(){this(new OperatingFinanceService());}OperatingProfitReportServlet(OperatingFinanceService service){this.service=service;}
    protected void doGet(HttpServletRequest q,HttpServletResponse p)throws IOException{if(admin(q,p)<0)return;try{String from=q.getParameter("fromDate"),to=q.getParameter("toDate");if(from==null||to==null)throw new IllegalArgumentException("fromDate and toDate are required");ApiResponse.ok(p,service.operatingProfit(LocalDate.parse(from),LocalDate.parse(to)));}catch(IllegalArgumentException|DateTimeException e){ApiResponse.error(p,e.getMessage(),400);}catch(RuntimeException e){ApiResponse.error(p,"Internal server error",500);}}
    protected int admin(HttpServletRequest q,HttpServletResponse p)throws IOException{return AdminApiAuth.require(q,p,AdminApiAuth.jwt());}
}
