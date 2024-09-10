package thesis.ecommerce.authservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import thesis.ecommerce.authservice.dtos.RoleDto;
import thesis.ecommerce.authservice.service.RoleService;

@RestController
@RequestMapping("/api/role")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> addRole(@RequestBody RoleDto roleDto) {
        return roleService.addRole(roleDto.getUsername(), roleDto.getRole());
    }

    @PostMapping("/remove")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> removeRole(@RequestBody RoleDto roleDto) {
        return roleService.removeRole(roleDto.getUsername(), roleDto.getRole());
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> getUserRoles(@PathVariable String username) {
        return roleService.getUserRoles(username);
    }
}