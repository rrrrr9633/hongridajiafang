package top.sama.haode.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayCorsFilter implements WebFilter {
    private final Set<String> allowedOrigins;

    public GatewayCorsFilter(
            @Value("${APP_CORS_ALLOWED_ORIGINS:http://127.0.0.1:3000,http://localhost:3000}") String origins
    ) {
        this.allowedOrigins = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String origin = exchange.getRequest().getHeaders().getOrigin();
        writeCors(exchange.getResponse(), origin);
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    private void writeCors(ServerHttpResponse response, String origin) {
        if (origin == null || !allowedOrigins.contains(origin)) {
            return;
        }
        HttpHeaders headers = response.getHeaders();
        headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization");
        headers.set(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,PATCH,DELETE,OPTIONS");
        headers.set(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
        headers.set(HttpHeaders.VARY, "Origin");
    }

    @Configuration
    static class DownstreamCorsStrip {
        @Bean
        HttpHeadersFilter stripDownstreamCorsHeaders() {
            return (input, exchange) -> {
                HttpHeaders filtered = new HttpHeaders();
                input.forEach((name, values) -> {
                    if (!name.toLowerCase().startsWith("access-control-")) {
                        filtered.put(name, values);
                    }
                });
                return filtered;
            };
        }
    }
}
