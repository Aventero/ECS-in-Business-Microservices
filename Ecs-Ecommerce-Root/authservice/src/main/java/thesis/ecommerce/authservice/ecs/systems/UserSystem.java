package thesis.ecommerce.authservice.ecs.systems;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import dev.dominion.ecs.api.Entity;
import jakarta.annotation.PostConstruct;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.authservice.ecs.components.Flags;
import thesis.ecommerce.authservice.ecs.components.FutureResponseComponent;
import thesis.ecommerce.authservice.ecs.components.UsernameComponent;
import thesis.ecommerce.authservice.repository.UserCredentialsRepository;

@Service
public class UserSystem implements Runnable {
    private final ECSWorld world;
    private final UserCredentialsRepository userRepository;

    UserSystem(ECSWorld world, UserCredentialsRepository userRepository) {
        this.world = world;
        this.userRepository = userRepository;
    }

    protected void processEntity(Entity entity) {
        String username = entity.get(UsernameComponent.class).getUsername();
        removeUserComponent(entity);
        userRepository.findByUsername(username)
                .ifPresentOrElse(
                        user -> entity.add(new FutureResponseComponent(ResponseEntity.ok().body(user))),
                        () -> entity.add(new FutureResponseComponent(ResponseEntity.notFound().build())));
    }

    private void removeUserComponent(Entity entity) {
        entity.removeType(UsernameComponent.class);
        entity.removeType(Flags.GetUser.class);
    }

    @Override
    public void run() {
        world.getDominion().findEntitiesWith(Flags.GetUser.class, UsernameComponent.class)
                .forEach(result -> processEntity(result.entity()));
    }

    @PostConstruct
    public void init() {
        world.registerSystem(this);
    }
}
