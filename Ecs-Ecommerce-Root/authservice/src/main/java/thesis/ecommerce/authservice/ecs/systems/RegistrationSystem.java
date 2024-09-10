package thesis.ecommerce.authservice.ecs.systems;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import dev.dominion.ecs.api.Entity;
import jakarta.annotation.PostConstruct;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.authservice.ecs.components.Flags;
import thesis.ecommerce.authservice.ecs.components.FutureResponseComponent;
import thesis.ecommerce.authservice.ecs.components.PlaintextPasswordComponent;
import thesis.ecommerce.authservice.ecs.components.UsernameComponent;
import thesis.ecommerce.authservice.model.UserCredentialsModel;
import thesis.ecommerce.authservice.model.UserRole;
import thesis.ecommerce.authservice.repository.UserCredentialsRepository;

@Service
public class RegistrationSystem implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationSystem.class);
    private final UserCredentialsRepository userCredentialsRepository;
    private final ECSWorld world;
    private final PasswordEncoder passwordEncoder;

    public RegistrationSystem(UserCredentialsRepository userCredentialsRepository, ECSWorld world,
            PasswordEncoder passwordEncoder) {
        this.userCredentialsRepository = userCredentialsRepository;
        this.world = world;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        world.registerSystem(this);
    }

    public void process(Entity entity) {
        String username = entity.get(UsernameComponent.class).getUsername();
        String password = entity.get(PlaintextPasswordComponent.class).getPassword();

        if (!isValidUsername(username) || !isValidPassword(password)) {
            String msg = "Registration failed: Invalid username or invalid password";
            logger.warn(msg);
            removeRegistrationComponents(entity);
            entity.add(new FutureResponseComponent(ResponseEntity.badRequest().body(msg)));
            return;
        }

        if (userCredentialsRepository.existsByUsername(username)) {
            String msg = "Registration failed: Username already exists: " + username;
            logger.warn(msg);
            removeRegistrationComponents(entity);
            entity.add(new FutureResponseComponent(ResponseEntity.badRequest().body(msg)));
            return;
        }

        String hashedPassword = passwordEncoder.encode(password);
        UserCredentialsModel newUser = new UserCredentialsModel();
        newUser.setUsername(username);
        newUser.getRoles().add(UserRole.ROLE_USER.name());
        newUser.setHashedPassword(hashedPassword);
        userCredentialsRepository.save(newUser);
        removeRegistrationComponents(entity);
        entity.add(new FutureResponseComponent(ResponseEntity.ok(newUser)));
    }

    private boolean isValidUsername(String username) {
        return username != null && username.length() >= 3;
    }

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    private void removeRegistrationComponents(Entity entity) {
        entity.removeType(Flags.Register.class);
        entity.removeType(PlaintextPasswordComponent.class);
        entity.removeType(UsernameComponent.class);
    }

    @Override
    public void run() {
        world.getDominion().findEntitiesWith(Flags.Register.class)
                .forEach(result -> process(result.entity()));
    }
}