package top.sama.haode.order.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.sama.haode.order.service.AdminAuthService;

import java.io.IOException;

@Component
public class AdminTokenFilter extends OncePerRequestFilter {
    private final AdminAuthService authService;

    public AdminTokenFilter(AdminAuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/admin/")
                || request.getRequestURI().equals("/api/admin/auth/login")) {
            chain.doFilter(request, response);
            return;
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")
                || !authService.isValid(authorization.substring("Bearer ".length()).trim())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid administrator token");
            return;
        }
        chain.doFilter(request, response);
    }
}
