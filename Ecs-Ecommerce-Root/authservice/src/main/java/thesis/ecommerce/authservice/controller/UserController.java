package thesis.ecommerce.authservice.controller;

import dev.dominion.ecs.api.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.authservice.ecs.components.CompletableFutureComponent;
import thesis.ecommerce.authservice.ecs.components.Flags;
import thesis.ecommerce.authservice.ecs.components.UsernameComponent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final ECSWorld ecsWorld;

    public UserController(ECSWorld ecsWorld) {
        this.ecsWorld = ecsWorld;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getAuthenticatedUser(Authentication authentication) {
        logger.info("Authenticating user: {}", authentication);
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("username", username);
            userInfo.put("authorities", authorities);
            return ResponseEntity.ok(userInfo);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
    }

    @GetMapping("/{username}")
    public CompletableFuture<ResponseEntity<?>> getUser(@PathVariable String username) {
        Entity entity = ecsWorld.createEntity();
        entity.add(new UsernameComponent(username));
        entity.add(new Flags.GetUser());

        CompletableFuture<ResponseEntity<?>> futureResponse = new CompletableFuture<>();
        entity.add(new CompletableFutureComponent(futureResponse));
        return futureResponse.thenApply(response -> {
            logger.info("Sending response for user: {}. Response status: {}", username, response.getStatusCode());
            return response;
        });
    }
}