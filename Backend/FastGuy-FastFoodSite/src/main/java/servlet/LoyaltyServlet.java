package servlet;

import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.LoyaltyService;
import utils.ApiResponse;
import utils.JwtUtil;

@WebServlet("/api/loyalty/me")
public class LoyaltyServlet extends HttpServlet {
    private LoyaltyService loyaltyService = new LoyaltyService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { ApiResponse.error(resp, "Missing token", 401); return; }
        String token = header.substring(7);
        if (!"USER".equals(JwtUtil.getRole(token))) { ApiResponse.error(resp, "Forbidden", 403); return; }
        int userId = JwtUtil.getUserId(token);
        var data = loyaltyService.getForUser(userId);
        if (data == null) ApiResponse.error(resp, "User not found", 404);
        else ApiResponse.ok(resp, data);
    }
}
