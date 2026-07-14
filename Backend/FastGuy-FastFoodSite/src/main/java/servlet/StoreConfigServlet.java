package servlet;

import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.StoreConfigService;
import utils.ApiResponse;

@WebServlet("/api/store/config")
public class StoreConfigServlet extends HttpServlet {
    private StoreConfigService storeConfigService = new StoreConfigService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        ApiResponse.ok(resp, storeConfigService.getPublicConfig());
    }
}
