package thesis.ecommerce.gatewayservice.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.JwtException;
import reactor.core.publisher.Mono;
import thesis.ecommerce.gatewayservice.util.JwtService;

@Component
public class AuthenticationFilter implements GatewayFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFilter.class);
    private final JwtService jwtService;
    private final List<String> excludedPaths = List.of("/api/auth/register", "/api/auth/login");

    public AuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();
        LOGGER.debug("Received request for path: {}", path);

        if (isExcludedPath(path)) {
            LOGGER.debug("Path {} is excluded from authentication", path);
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.error(new JwtException("No valid Authorization header"));
        }

        String token = authHeader.substring(7);
        try {
            String username = jwtService.extractUsername(token);
            String roles = String.join(",", jwtService.extractRoles(token));
            LOGGER.info("JWT token validated successfully for user: {}", username);
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", username)
                    .header("X-User-Roles", roles)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build())
                    .then(Mono.fromRunnable(() -> {
                        ServerHttpResponse response = exchange.getResponse();
                        HttpStatusCode status = response.getStatusCode();
                        LOGGER.info("Response status: {}", status);
                        response.getHeaders()
                                .forEach((key, value) -> LOGGER.info("Response header: {} = {}", key, value));
                    }));
        } catch (Exception e) {
            return Mono.error(new JwtException("Invalid token: " + e.getMessage()));
        }
    }

    private boolean isExcludedPath(String path) {
        return excludedPaths.stream().anyMatch(path::startsWith);
    }
}