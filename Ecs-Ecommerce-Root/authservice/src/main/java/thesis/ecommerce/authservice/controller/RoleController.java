package thesis.ecommerce.authservice.controller;

import dev.dominion.ecs.api.Entity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.authservice.dtos.RoleDto;
import thesis.ecommerce.authservice.ecs.components.CompletableFutureComponent;
import thesis.ecommerce.authservice.ecs.components.Flags;
import thesis.ecommerce.authservice.ecs.components.RoleComponent;
import thesis.ecommerce.authservice.ecs.components.UsernameComponent;
import thesis.ecommerce.authservice.model.UserRole;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/role")
public class RoleController {
    private final ECSWorld ecsWorld;

    public RoleController(ECSWorld ecsWorld) {
        this.ecsWorld = ecsWorld;
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public CompletableFuture<ResponseEntity<?>> addRole(@RequestBody RoleDto roleDto) {
        return processRoleRequest(roleDto.getUsername(), roleDto.getRole(), new Flags.AddRole());
    }

    @PostMapping("/remove")
    public CompletableFuture<ResponseEntity<?>> removeRole(@RequestBody RoleDto roleDto) {
        return processRoleRequest(roleDto.getUsername(), roleDto.getRole(), new Flags.RemoveRole());
    }

    @GetMapping("/{username}")
    public CompletableFuture<ResponseEntity<?>> getUserRoles(@PathVariable String username) {
        return processRoleRequest(username, null, new Flags.GetRoles());
    }

    private CompletableFuture<ResponseEntity<?>> processRoleRequest(String username, UserRole role, Object flag) {
        Entity entity = ecsWorld.createEntity();
        entity.add(new UsernameComponent(username));
        if (role != null) {
            entity.add(new RoleComponent(role));
        }
        entity.add(flag);

        CompletableFuture<ResponseEntity<?>> futureResponse = new CompletableFuture<>();
        entity.add(new CompletableFutureComponent(futureResponse));
        return futureResponse;
    }
}