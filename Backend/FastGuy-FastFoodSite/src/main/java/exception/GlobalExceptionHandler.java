package exception;

import dto.ApiResponse;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import utils.JsonUtil;

import java.io.IOException;

public class GlobalExceptionHandler implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (AppException e) {
            handleException((HttpServletResponse) response, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            handleException((HttpServletResponse) response, 500, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    private void handleException(HttpServletResponse resp, int statusCode, String message) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write(JsonUtil.toJson(ApiResponse.error(message)));
    }

    @Override public void init(FilterConfig filterConfig) {}
    @Override public void destroy() {}
}
