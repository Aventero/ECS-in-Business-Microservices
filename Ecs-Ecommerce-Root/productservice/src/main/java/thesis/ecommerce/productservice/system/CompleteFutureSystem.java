package thesis.ecommerce.productservice.system;

import dev.dominion.ecs.api.Entity;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.productservice.component.CompletableFutureComponent;
import thesis.ecommerce.productservice.component.Flags;
import thesis.ecommerce.productservice.component.FutureResponseComponent;

import java.util.concurrent.CompletableFuture;

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
        entity.add(new Flags.RequiresDeletion());
    }

    @Override
    public void run() {
        world.getDominion()
                .findEntitiesWith(FutureResponseComponent.class, CompletableFutureComponent.class)
                .forEach(result -> processEntity(result.entity()));
    }
}