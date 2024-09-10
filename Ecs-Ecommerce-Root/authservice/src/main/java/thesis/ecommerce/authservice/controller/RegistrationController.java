package thesis.ecommerce.authservice.controller;

import dev.dominion.ecs.api.Entity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.authservice.dtos.RegisterUserDto;
import thesis.ecommerce.authservice.ecs.components.CompletableFutureComponent;
import thesis.ecommerce.authservice.ecs.components.Flags;
import thesis.ecommerce.authservice.ecs.components.PlaintextPasswordComponent;
import thesis.ecommerce.authservice.ecs.components.UsernameComponent;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

    private final ECSWorld ecsWorld;

    public RegistrationController(ECSWorld ecsWorld) {
        this.ecsWorld = ecsWorld;
    }

    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<?>> registerUser(@RequestBody RegisterUserDto request) {
        Entity entity = ecsWorld.createEntity(new UsernameComponent(request.getUsername()), new PlaintextPasswordComponent(request.getPassword()), new Flags.Register());
        CompletableFuture<ResponseEntity<?>> futureResponse = new CompletableFuture<>();
        entity.add(new CompletableFutureComponent(futureResponse));
        return futureResponse;
    }
}