package thesis.ecommerce.productservice.system;

import dev.dominion.ecs.api.Entity;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.productservice.component.Flags;

@Service
public class DeleteEntitySystem implements Runnable {

    private final ECSWorld ecsWorld;

    public DeleteEntitySystem(ECSWorld ecsWorld) {
        this.ecsWorld = ecsWorld;
        this.ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        ecsWorld.getDominion().findEntitiesWith(
                Flags.RequiresDeletion.class,
                Flags.DatabaseProcessingFinished.class).stream()
            .forEach(result -> processDeletion(result.entity()));

    }

    private void processDeletion(Entity entity) {
        ecsWorld.getDominion().deleteEntity(entity);
    }

}