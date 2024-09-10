package thesis.ecommerce.gatewayservice.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final AuthenticationFilter authenticationFilter;

    public GatewayConfig(AuthenticationFilter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
    }

    @Bean
    RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("auth_route",
                r -> r.path("/api/auth/**").filters(f -> f.filter(authenticationFilter))
                    .uri("http://localhost:8081"))
            .route("user_route",
                r -> r.path("/api/users/**").filters(f -> f.filter(authenticationFilter))
                    .uri("http://localhost:8081"))
            .route("role_route",
                r -> r.path("/api/role/**").filters(f -> f.filter(authenticationFilter))
                    .uri("http://localhost:8081"))
            .route("order_route",
                r -> r.path("/api/orders/**").filters(f -> f.filter(authenticationFilter))
                    .uri("http://localhost:8082"))
            .route("cart_route",
                r -> r.path("/api/cart/**").filters(f -> f.filter(authenticationFilter))
                    .uri("http://localhost:8082"))
            .route("product_route",
                r -> r.path("/api/products/**").filters(f -> f.filter(authenticationFilter))
                    .uri("http://localhost:8083")).build();
    }
}