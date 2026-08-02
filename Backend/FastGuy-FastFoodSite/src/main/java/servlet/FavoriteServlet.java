package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.UserFavoriteService;
import utils.ApiResponse;
import utils.JwtUtil;

import java.io.IOException;

@WebServlet("/api/favorites/*")
public class FavoriteServlet extends HttpServlet {
    private UserFavoriteService favoriteService = new UserFavoriteService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req);
        if (userId <= 0) {
            ApiResponse.error(resp, "Unauthorized", 401);
            return;
        }

        String path = req.getPathInfo();
        if (path != null && path.startsWith("/check/")) {
            int productId = Integer.parseInt(path.substring("/check/".length()));
            ApiResponse.ok(resp, java.util.Map.of("productId", productId, "favorite", favoriteService.isFavorite(userId, productId)));
            return;
        }
        ApiResponse.ok(resp, favoriteService.getByUserId(userId));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = getUserId(req);
        if (userId <= 0) {
            ApiResponse.error(resp, "Unauthorized", 401);
            return;
        }

        String path = req.getPathInfo();
        if (path == null || !path.startsWith("/toggle/")) {
            ApiResponse.error(resp, "Not found", 404);
            return;
        }

        try {
            int productId = Integer.parseInt(path.substring("/toggle/".length()));
            ApiResponse.ok(resp, favoriteService.toggle(userId, productId));
        } catch (IllegalArgumentException e) {
            ApiResponse.error(resp, e.getMessage(), 400);
        } catch (Exception e) {
            ApiResponse.error(resp, "Favorite failed", 500);
        }
    }

    private int getUserId(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return -1;
        return JwtUtil.getUserId(auth.substring(7));
    }
}
