package thesis.ecommerce.orderservice.ecs.system.general;

import dev.dominion.ecs.api.Entity;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.ecs.component.general.CompletableFutureComponent;
import thesis.ecommerce.orderservice.ecs.component.general.FutureResponseComponent;

@Service
public class CompleteFutureSystem implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompleteFutureSystem.class);
    private final ECSWorld world;

    public CompleteFutureSystem(ECSWorld world) {
        this.world = world;
        world.registerSystem(this);
    }

    protected void processEntity(Entity entity) {
        LOGGER.info("PROCESSING COMPLETE FUTURE REQUEST");
        CompletableFuture<ResponseEntity<?>> future = entity.get(CompletableFutureComponent.class)
            .getFuture();
        FutureResponseComponent response = entity.get(FutureResponseComponent.class);
        LOGGER.info("Completing future: {}", response.getResult());
        future.complete(response.getResult());
        entity.removeType(CompletableFutureComponent.class);
        world.getDominion().deleteEntity(entity);
    }

    @Override
    public void run() {
        world.getDominion()
            .findEntitiesWith(FutureResponseComponent.class, CompletableFutureComponent.class).stream()
            .forEach(result -> processEntity(result.entity()));
    }
}