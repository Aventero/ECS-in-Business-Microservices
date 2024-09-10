package thesis.ecommerce.authservice.controller;

import dev.dominion.ecs.api.Entity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.authservice.dtos.LoginUserDto;
import thesis.ecommerce.authservice.ecs.components.CompletableFutureComponent;
import thesis.ecommerce.authservice.ecs.components.Flags;
import thesis.ecommerce.authservice.ecs.components.PlaintextPasswordComponent;
import thesis.ecommerce.authservice.ecs.components.UsernameComponent;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final ECSWorld ecsWorld;

    public LoginController(ECSWorld ecsWorld) {
        this.ecsWorld = ecsWorld;
    }

    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<?>> loginUser(@RequestBody LoginUserDto loginUserDto) {

        Entity entity = ecsWorld.createEntity(new UsernameComponent(loginUserDto.getUsername()), new PlaintextPasswordComponent(loginUserDto.getPassword()), new Flags.Login());
        CompletableFuture<ResponseEntity<?>> futureResponse = new CompletableFuture<>();
        entity.add(new CompletableFutureComponent(futureResponse));
        return futureResponse;
    }

    @PostMapping("/logout")
    public CompletableFuture<ResponseEntity<?>> logoutUser(@RequestParam String username) {
        Entity entity = ecsWorld.createEntity(new UsernameComponent(username), new Flags.Logout());
        CompletableFuture<ResponseEntity<?>> futureResponse = new CompletableFuture<>();
        entity.add(new CompletableFutureComponent(futureResponse));
        return futureResponse;
    }

}