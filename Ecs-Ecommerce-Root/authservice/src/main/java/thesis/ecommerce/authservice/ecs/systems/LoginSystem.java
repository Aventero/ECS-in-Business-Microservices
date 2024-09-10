package thesis.ecommerce.authservice.ecs.systems;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import dev.dominion.ecs.api.Entity;
import jakarta.annotation.PostConstruct;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.authservice.ecs.components.Flags;
import thesis.ecommerce.authservice.ecs.components.FutureResponseComponent;
import thesis.ecommerce.authservice.ecs.components.PlaintextPasswordComponent;
import thesis.ecommerce.authservice.ecs.components.UsernameComponent;
import thesis.ecommerce.authservice.model.UserCredentialsModel;
import thesis.ecommerce.authservice.repository.UserCredentialsRepository;
import thesis.ecommerce.authservice.util.JwtService;

@Service
public class LoginSystem implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(LoginSystem.class);

    private final UserCredentialsRepository userCredentialsRepository;
    private final ECSWorld world;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginSystem(UserCredentialsRepository userCredentialsRepository, ECSWorld world,
            AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userCredentialsRepository = userCredentialsRepository;
        this.world = world;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostConstruct
    public void init() {
        world.registerSystem(this);
    }

    public void process(Entity entity) {
        String username = entity.get(UsernameComponent.class).getUsername();
        String plaintextPassword = entity.get(PlaintextPasswordComponent.class).getPassword();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, plaintextPassword));
        } catch (Exception e) {
            String msg = "Login failed: " + e.getMessage();
            logger.warn(msg);
            removeLoginComponents(entity);
            entity.add(new FutureResponseComponent(ResponseEntity.badRequest().body(msg)));
            return;
        }

        UserCredentialsModel storedUser = userCredentialsRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!BCrypt.checkpw(plaintextPassword, storedUser.getHashedPassword())) {
            String msg = "Login failed: Password incorrect. Username: " + username;
            logger.warn(msg);
            removeLoginComponents(entity);
            entity.add(new FutureResponseComponent(ResponseEntity.badRequest().body(msg)));
            return;
        }

        String token = jwtService.generateToken(storedUser);
        removeLoginComponents(entity);
        entity.add(new FutureResponseComponent(ResponseEntity.ok(token)));
    }

    private void removeLoginComponents(Entity entity) {
        entity.removeType(Flags.Login.class);
        entity.removeType(PlaintextPasswordComponent.class);
        entity.removeType(UsernameComponent.class);
    }

    @Override
    public void run() {
        world.getDominion()
                .findEntitiesWith(Flags.Login.class)
                .forEach(result -> process(result.entity()));
    }
}