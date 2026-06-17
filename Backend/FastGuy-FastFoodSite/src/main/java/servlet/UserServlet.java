package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.UserService;
import utils.ApiResponse;
import utils.JwtUtil;

import java.io.IOException;

@WebServlet("/api/user/home")
public class UserServlet extends HttpServlet {
    private UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return;
        }

        String token = authHeader.substring(7);
        int userId = JwtUtil.getUserId(token);
        if (userId < 0) {
            ApiResponse.error(resp, "Invalid token", 401);
            return;
        }

        var data = userService.getDashboard(userId);
        if (data == null) {
            ApiResponse.error(resp, "User not found", 404);
            return;
        }

        ApiResponse.ok(resp, data);
    }
}
