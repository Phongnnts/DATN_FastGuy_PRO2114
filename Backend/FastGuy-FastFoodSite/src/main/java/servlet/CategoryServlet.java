package servlet;

import dao.CategoryDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ApiResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/categories")
public class CategoryServlet extends HttpServlet {
    private CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        List<Map<String, Object>> categories = categoryDAO.findAllActive()
                .stream()
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("categoryId", c.getCategoryId());
                    m.put("name", c.getName());
                    m.put("description", c.getDescription() != null ? c.getDescription() : "");
                    return m;
                })
                .collect(Collectors.toList());

        ApiResponse.ok(resp, categories);
    }
}
