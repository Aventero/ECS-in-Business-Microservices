package thesis.ecommerce.authservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import thesis.ecommerce.authservice.model.UserCredentialsModel;
import thesis.ecommerce.authservice.repository.UserCredentialsRepository;
import thesis.ecommerce.authservice.util.JwtService;

@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserCredentialsRepository userCredentialsRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthenticationService(UserCredentialsRepository userCredentialsRepository,
        AuthenticationManager authenticationManager,
        JwtService jwtService) {
        this.userCredentialsRepository = userCredentialsRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public ResponseEntity<?> login(String username, String plaintextPassword) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, plaintextPassword));
        } catch (Exception e) {
            String msg = "Login failed: " + e.getMessage();
            logger.warn(msg);
            return ResponseEntity.badRequest().body(msg);
        }

        UserCredentialsModel storedUser = userCredentialsRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!BCrypt.checkpw(plaintextPassword, storedUser.getHashedPassword())) {
            String msg = "Login failed: Password incorrect. Username: " + username;
            logger.warn(msg);
            return ResponseEntity.badRequest().body(msg);
        }

        String token = jwtService.generateToken(storedUser);
        return ResponseEntity.ok(token);
    }

    public ResponseEntity<?> logout(String username) {
        try {
            UserCredentialsModel storedUser = userCredentialsRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            logger.info("Logout complete: {}", storedUser.getUsername());

            return ResponseEntity.ok("Logout successful");
        } catch (UsernameNotFoundException e) {
            logger.warn("Logout failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}