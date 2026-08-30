package servlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import service.StaffPayRateService;
import utils.*;

@WebServlet("/api/admin/staff/*")
public class AdminStaffPayRateServlet extends HttpServlet {
 private final StaffPayRateService service=new StaffPayRateService();
 protected void doGet(HttpServletRequest q,HttpServletResponse p)throws IOException{int admin=AdminApiAuth.require(q,p,AdminApiAuth.jwt());if(admin<0)return;try{ApiResponse.ok(p,service.list(userId(q.getPathInfo())));}catch(java.util.NoSuchElementException e){ApiResponse.error(p,e.getMessage(),404);}catch(IllegalArgumentException e){ApiResponse.error(p,e.getMessage(),400);}}
 protected void doPost(HttpServletRequest q,HttpServletResponse p)throws IOException{int admin=AdminApiAuth.require(q,p,AdminApiAuth.jwt());if(admin<0)return;try{Map<String,Object>b=JsonUtil.getMapper().readValue(q.getReader(),Map.class);if(b==null||!b.keySet().equals(java.util.Set.of("effectiveFrom","regularHourlyRate","overtimeHourlyRate")))throw new IllegalArgumentException("Invalid pay rate payload");p.setStatus(201);ApiResponse.ok(p,service.create(userId(q.getPathInfo()),LocalDate.parse((String)b.get("effectiveFrom")),decimal(b.get("regularHourlyRate")),decimal(b.get("overtimeHourlyRate")),admin));}catch(StaffPayRateService.DuplicateRate e){ApiResponse.error(p,e.getMessage(),409);}catch(java.util.NoSuchElementException e){ApiResponse.error(p,e.getMessage(),404);}catch(Exception e){ApiResponse.error(p,e.getMessage(),400);}}
 static int userId(String path){if(path==null||!path.matches("/[1-9]\\d*/pay-rates"))throw new IllegalArgumentException("Invalid user ID");return Integer.parseInt(path.substring(1,path.indexOf('/',1)));}
 static BigDecimal decimal(Object value){if(!(value instanceof Number number))throw new IllegalArgumentException("Invalid hourly rate");return new BigDecimal(number.toString());}
}
