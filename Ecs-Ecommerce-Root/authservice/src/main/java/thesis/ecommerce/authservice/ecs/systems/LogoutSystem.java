package thesis.ecommerce.authservice.ecs.systems;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.dominion.ecs.api.Entity;
import jakarta.annotation.PostConstruct;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.authservice.ecs.components.Flags;
import thesis.ecommerce.authservice.ecs.components.UsernameComponent;
import thesis.ecommerce.authservice.model.UserCredentialsModel;
import thesis.ecommerce.authservice.repository.UserCredentialsRepository;

@Service
public class LogoutSystem implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(LogoutSystem.class);

    private final UserCredentialsRepository userCredentialsRepository;
    private final ECSWorld world;

    public LogoutSystem(UserCredentialsRepository userCredentialsRepository, ECSWorld world) {
        this.userCredentialsRepository = userCredentialsRepository;
        this.world = world;
    }

    @PostConstruct
    public void init() {
        world.registerSystem(this);
    }

    private void process(Entity entity) {
        UserCredentialsModel storedUser = userCredentialsRepository
                .findByUsername(entity.get(UsernameComponent.class).getUsername()).orElseThrow();

        logger.info("Logout complete: {}", storedUser.getUsername());
        entity.removeType(Flags.Logout.class);
        world.getDominion().deleteEntity(entity);
    }

    @Override
    public void run() {
        world.getDominion()
                .findEntitiesWith(Flags.Logout.class, UsernameComponent.class)
                .forEach(result -> process(result.entity()));
    }

}