package thesis.ecommerce.authservice.service;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import thesis.ecommerce.authservice.model.UserCredentialsModel;
import thesis.ecommerce.authservice.model.UserRole;
import thesis.ecommerce.authservice.repository.UserCredentialsRepository;

@Service
public class RoleService {

    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);
    private final UserCredentialsRepository userRepository;

    public RoleService(UserCredentialsRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ResponseEntity<?> addRole(String username, UserRole role) {
        logger.info("Adding role {} to user {}", role, username);
        try {
            UserCredentialsModel user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
            user.getRoles().add(role.name());
            userRepository.save(user);
            logger.info("Role {} successfully added to user {}", role, username);
            return ResponseEntity.ok("Role added successfully");
        } catch (Exception e) {
            logger.error("Error adding role: ", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    public ResponseEntity<?> removeRole(String username, UserRole role) {
        logger.info("Removing role {} from user {}", role, username);
        try {
            UserCredentialsModel user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
            if (user.getRoles().remove(role.name())) {
                userRepository.save(user);
                logger.info("Role {} successfully removed from user {}", role, username);
                return ResponseEntity.ok("Role removed successfully");
            } else {
                logger.warn("User {} did not have the role {}", username, role);
                return ResponseEntity.badRequest().body("User did not have the specified role");
            }
        } catch (Exception e) {
            logger.error("Error removing role: ", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    public ResponseEntity<?> getUserRoles(String username) {
        logger.info("Getting roles for user {}", username);
        try {
            UserCredentialsModel user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
            Set<String> roles = user.getRoles();
            logger.info("Retrieved roles for user {}: {}", username, roles);
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            logger.error("Error getting roles: ", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}