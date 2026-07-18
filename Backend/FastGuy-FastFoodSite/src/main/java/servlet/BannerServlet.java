package servlet;

import dao.BannerDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet("/api/banners")
public class BannerServlet extends HttpServlet {
    private BannerDAO bannerDAO = new BannerDAO();

    private Map<String, Object> toMap(entity.Banner b) {
        Map<String, Object> m = new HashMap<>();
        m.put("bannerId", b.getBannerId());
        m.put("title", b.getTitle());
        m.put("subtitle", b.getSubtitle());
        m.put("imageUrl", b.getImageUrl());
        m.put("link", b.getLink());
        m.put("sortOrder", b.getSortOrder());
        return m;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        List<Map<String, Object>> list = bannerDAO.findAllActive().stream()
                .map(this::toMap).collect(Collectors.toList());
        ApiResponse.ok(resp, list);
    }
}
