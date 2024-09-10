package thesis.ecommerce.authservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import thesis.ecommerce.authservice.model.UserCredentialsModel;
import thesis.ecommerce.authservice.model.UserRole;
import thesis.ecommerce.authservice.repository.UserCredentialsRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    // Logger
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final UserCredentialsRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public DataInitializer(
            UserCredentialsRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.username}") String adminUsername,
            @Value("${admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername(adminUsername)) {
            UserCredentialsModel adminUser = new UserCredentialsModel();
            adminUser.setUsername(adminUsername);
            adminUser.setHashedPassword(passwordEncoder.encode(adminPassword));
            adminUser.getRoles().add(UserRole.ROLE_ADMIN.name());
            adminUser.getRoles().add(UserRole.ROLE_USER.name());
            userRepository.save(adminUser);
            logger.info("Admin user created");
        } else {
            logger.info("Admin user already exists");
        }
    }
}