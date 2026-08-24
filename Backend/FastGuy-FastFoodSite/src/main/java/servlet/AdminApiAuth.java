package servlet;

import java.io.IOException;
import jakarta.servlet.http.*;
import utils.*;

final class AdminApiAuth {
    interface TokenReader { String role(String token); int userId(String token); }
    private AdminApiAuth() {}
    static int require(HttpServletRequest req,HttpServletResponse resp,TokenReader reader)throws IOException{
        String header=req.getHeader("Authorization");
        if(header==null||!header.startsWith("Bearer ")||header.length()==7){ApiResponse.error(resp,"Missing or invalid token",401);return-1;}
        try{String token=header.substring(7);String role=reader.role(token);int userId=reader.userId(token);if(!"ADMIN".equals(role)||!PrivilegedAuth.isActiveRole(userId,"ADMIN")){ApiResponse.error(resp,"Forbidden",403);return-1;}return userId;}
        catch(RuntimeException e){ApiResponse.error(resp,"Missing or invalid token",401);return-1;}
    }
    static TokenReader jwt(){return new TokenReader(){public String role(String token){return JwtUtil.getRole(token);}public int userId(String token){return JwtUtil.getUserId(token);}};}
}
