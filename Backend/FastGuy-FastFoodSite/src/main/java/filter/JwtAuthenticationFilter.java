package filter;

import exception.UnauthorizedException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import utils.JwtUtil;

import java.io.IOException;
import java.util.Set;

public class JwtAuthenticationFilter implements Filter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/", "/api/products", "/api/categories", "/api/delivery-zones", "/api/reviews"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI();

        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::contains);
        if (isPublic) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Thiếu token xác thực");
        }

        String token = authHeader.substring(7);
        try {
            Long userId = JwtUtil.getUserId(token);
            String role = JwtUtil.getRole(token);
            req.setAttribute("userId", userId);
            req.setAttribute("role", role);
            chain.doFilter(request, response);
        } catch (Exception e) {
            throw new UnauthorizedException("Token không hợp lệ hoặc đã hết hạn");
        }
    }

    @Override public void init(FilterConfig filterConfig) {}
    @Override public void destroy() {}
}
