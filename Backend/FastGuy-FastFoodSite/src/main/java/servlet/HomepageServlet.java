package servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import service.HomepageService;
import utils.ApiResponse;

@WebServlet("/api/homepage")
public class HomepageServlet extends HttpServlet {

    private final HomepageService homepageService = new HomepageService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try {
            ApiResponse.ok(resp, responseData(homepageService.getHomepage()));
        } catch (RuntimeException e) {
            ApiResponse.error(resp, "Homepage data could not be loaded", 500);
        }
    }

    static Map<String, Object> responseData(Map<String, Object> data) {
        return data;
    }
}
