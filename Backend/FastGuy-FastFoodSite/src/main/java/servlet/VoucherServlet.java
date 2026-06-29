package servlet;

import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;
import utils.JwtUtil;

@WebServlet("/api/user/vouchers")
public class VoucherServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return;
        }
        int userId = JwtUtil.getUserId(authHeader.substring(7));
        if (userId < 0) { ApiResponse.error(resp, "Invalid token", 401); return; }
        ApiResponse.ok(resp, new java.util.ArrayList<>());
    }
}
