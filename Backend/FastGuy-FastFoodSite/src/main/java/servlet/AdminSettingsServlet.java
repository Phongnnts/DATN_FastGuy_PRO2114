package servlet;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.StoreConfigService;
import utils.ApiResponse;
import utils.JwtUtil;

@WebServlet("/api/admin/settings")
public class AdminSettingsServlet extends HttpServlet {
    private StoreConfigService storeConfigService = new StoreConfigService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!admin(req, resp)) return;
        resp.setContentType("application/json;charset=UTF-8");
        ApiResponse.ok(resp, storeConfigService.getAll());
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!admin(req, resp)) return;
        resp.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = utils.JsonUtil.fromJson(req.getReader(), Map.class);
        try {
            storeConfigService.update(body);
            ApiResponse.ok(resp, storeConfigService.getAll(), "Settings updated");
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        }
    }

    private boolean admin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) { ApiResponse.error(resp, "Missing token", 401); return false; }
        if (!"ADMIN".equals(JwtUtil.getRole(header.substring(7)))) { ApiResponse.error(resp, "Forbidden", 403); return false; }
        return true;
    }
}
