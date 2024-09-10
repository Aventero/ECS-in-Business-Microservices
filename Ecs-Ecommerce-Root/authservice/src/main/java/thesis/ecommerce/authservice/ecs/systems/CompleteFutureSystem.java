package thesis.ecommerce.authservice.ecs.systems;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import dev.dominion.ecs.api.Entity;
import jakarta.annotation.PostConstruct;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.authservice.ecs.components.CompletableFutureComponent;
import thesis.ecommerce.authservice.ecs.components.FutureResponseComponent;

@Service
public class CompleteFutureSystem implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompleteFutureSystem.class);
    private final ECSWorld world;

    public CompleteFutureSystem(ECSWorld world) {
        this.world = world;
    }

    @PostConstruct
    public void init() {
        world.registerSystem(this);
    }

    protected void processEntity(Entity entity) {
        CompletableFuture<ResponseEntity<?>> future = entity.get(CompletableFutureComponent.class).getFuture();
        FutureResponseComponent response = entity.get(FutureResponseComponent.class);
        LOGGER.info("Completing future: {}", response.getResult());
        future.complete(response.getResult());
        entity.removeType(CompletableFutureComponent.class);
        world.getDominion().deleteEntity(entity);
    }

    @Override
    public void run() {
        world.getDominion()
                .findEntitiesWith(FutureResponseComponent.class, CompletableFutureComponent.class)
                .forEach(result -> processEntity(result.entity()));
    }
}
