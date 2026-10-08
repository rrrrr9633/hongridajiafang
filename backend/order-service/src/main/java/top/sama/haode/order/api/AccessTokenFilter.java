package top.sama.haode.order.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.sama.haode.order.domain.Session;
import top.sama.haode.order.repository.SessionRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Component
public class AccessTokenFilter extends OncePerRequestFilter {
    private final SessionRepository sessionRepository;

    public AccessTokenFilter(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/admin/")
                || (request.getRequestURI().startsWith("/api/media/") && request.getMethod().equals("GET"))
                || request.getRequestURI().equals("/api/auth/wechat")
                || request.getRequestURI().equals("/api/auth/dev")
                || request.getRequestURI().startsWith("/actuator/")
                || (request.getRequestURI().equals("/api/products") && request.getMethod().equals("GET"))
                || (request.getRequestURI().startsWith("/api/reviews/products/") && request.getMethod().equals("GET"))) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing access token");
            return;
        }

        String token = authorization.substring("Bearer ".length()).trim();
        String tokenHash = sha256(token);
        Session session = sessionRepository.findByTokenHashAndExpiresAtAfter(tokenHash, Instant.now()).orElse(null);
        if (session == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid access token");
            return;
        }

        request.setAttribute("userId", session.getUserId());
        filterChain.doFilter(request, response);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成会话摘要", exception);
        }
    }
}
