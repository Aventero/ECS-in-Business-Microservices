package thesis.ecommerce.authservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import thesis.ecommerce.authservice.dtos.LoginUserDto;
import thesis.ecommerce.authservice.service.AuthenticationService;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authService;

    @Autowired
    public AuthenticationController(AuthenticationService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginUserDto loginUserDto) {
        return authService.login(loginUserDto.getUsername(), loginUserDto.getPassword());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestParam String username) {
        return authService.logout(username);
    }
}