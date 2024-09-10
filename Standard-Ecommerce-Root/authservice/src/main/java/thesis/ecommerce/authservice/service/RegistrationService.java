package thesis.ecommerce.authservice.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import thesis.ecommerce.authservice.dtos.RegisterUserDto;
import thesis.ecommerce.authservice.model.UserCredentialsModel;
import thesis.ecommerce.authservice.model.UserRole;
import thesis.ecommerce.authservice.repository.UserCredentialsRepository;

@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);
    private final UserCredentialsRepository userCredentialsRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserCredentialsRepository userCredentialsRepository,
        PasswordEncoder passwordEncoder) {
        this.userCredentialsRepository = userCredentialsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ResponseEntity<?> registerUser(RegisterUserDto registrationDto) {
        String username = registrationDto.getUsername();
        String password = registrationDto.getPassword();

        if (!isValidUsername(username) || !isValidPassword(password)) {
            String msg = "Registration failed: Invalid username or password";
            logger.warn(msg);
            return ResponseEntity.badRequest().body(msg);
        }

        if (userCredentialsRepository.existsByUsername(username)) {
            String msg = "Registration failed: Username already exists: " + username;
            logger.warn(msg);
            return ResponseEntity.badRequest().body(msg);
        }

        String hashedPassword = passwordEncoder.encode(password);
        UserCredentialsModel newUser = new UserCredentialsModel();
        newUser.setUsername(username);
        newUser.getRoles().add(UserRole.ROLE_USER.name());
        newUser.setHashedPassword(hashedPassword);
        userCredentialsRepository.save(newUser);

        return ResponseEntity.ok(newUser);
    }

    private boolean isValidUsername(String username) {
        return username != null && username.length() >= 3;
    }

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
}