package servlet;

import dao.BannerDAO;
import entity.Banner;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;
import utils.JwtUtil;
import utils.JsonUtil;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet("/api/admin/banners/*")
public class AdminBannerServlet extends HttpServlet {
    private BannerDAO bannerDAO = new BannerDAO();

    private boolean checkAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse.error(resp, "Missing token", 401);
            return false;
        }
        String role = JwtUtil.getRole(authHeader.substring(7));
        if (!"ADMIN".equals(role)) {
            ApiResponse.error(resp, "Forbidden", 403);
            return false;
        }
        return true;
    }

    private Map<String, Object> toMap(Banner b) {
        Map<String, Object> m = new HashMap<>();
        m.put("bannerId", b.getBannerId());
        m.put("title", b.getTitle());
        m.put("subtitle", b.getSubtitle());
        m.put("imageUrl", b.getImageUrl());
        m.put("link", b.getLink());
        m.put("sortOrder", b.getSortOrder() != null ? b.getSortOrder() : 0);
        m.put("isActive", b.getIsActive() != null ? b.getIsActive() : true);
        m.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);
        return m;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            List<Map<String, Object>> list = bannerDAO.findAll().stream()
                    .map(this::toMap).collect(Collectors.toList());
            ApiResponse.ok(resp, list);
        } else {
            try {
                int id = Integer.parseInt(path.substring(1));
                Banner b = bannerDAO.findById(id);
                if (b == null) { ApiResponse.error(resp, "Not found", 404); return; }
                ApiResponse.ok(resp, toMap(b));
            } catch (NumberFormatException e) {
                ApiResponse.error(resp, "Invalid ID", 400);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
        if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }

        Banner b = new Banner();
        b.setImageUrl((String) body.get("imageUrl"));
        b.setTitle((String) body.get("title"));
        b.setSubtitle((String) body.get("subtitle"));
        b.setLink((String) body.get("link"));
        if (body.containsKey("sortOrder")) b.setSortOrder(((Number) body.get("sortOrder")).intValue());
        b.setIsActive(true);
        b.setCreatedAt(LocalDateTime.now());
        bannerDAO.save(b);

        resp.setStatus(201);
        ApiResponse.ok(resp, toMap(b), "Created");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { ApiResponse.error(resp, "Missing ID", 400); return; }
        try {
            int id = Integer.parseInt(path.substring(1));
            Banner b = bannerDAO.findById(id);
            if (b == null) { ApiResponse.error(resp, "Not found", 404); return; }

            Map<String, Object> body = JsonUtil.fromJson(req.getReader(), Map.class);
            if (body == null) { ApiResponse.error(resp, "Invalid data", 400); return; }

            if (body.containsKey("imageUrl")) b.setImageUrl((String) body.get("imageUrl"));
            if (body.containsKey("title")) b.setTitle((String) body.get("title"));
            if (body.containsKey("subtitle")) b.setSubtitle((String) body.get("subtitle"));
            if (body.containsKey("link")) b.setLink((String) body.get("link"));
            if (body.containsKey("sortOrder")) b.setSortOrder(((Number) body.get("sortOrder")).intValue());
            if (body.containsKey("isActive")) b.setIsActive((Boolean) body.get("isActive"));
            bannerDAO.save(b);
            ApiResponse.ok(resp, toMap(b), "Updated");
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid ID", 400);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if (!checkAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null || path.equals("/")) { ApiResponse.error(resp, "Missing ID", 400); return; }
        try {
            int id = Integer.parseInt(path.substring(1));
            bannerDAO.delete(id);
            ApiResponse.ok(resp, null, "Deleted");
        } catch (NumberFormatException e) {
            ApiResponse.error(resp, "Invalid ID", 400);
        }
    }
}
