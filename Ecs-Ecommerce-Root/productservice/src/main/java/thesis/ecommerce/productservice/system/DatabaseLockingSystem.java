package thesis.ecommerce.productservice.system;

import dev.dominion.ecs.api.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.productservice.component.Flags;
import thesis.ecommerce.productservice.component.Flags.DatabaseProcessingFinished;

@Service
public class DatabaseLockingSystem implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseLockingSystem.class);
    private final ECSWorld ecsWorld;
    private boolean isLocked = false;

    public DatabaseLockingSystem(ECSWorld ecsWorld) {
        this.ecsWorld = ecsWorld;
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        handleLockRequests();
        handleUnlockRequests();
    }

    private void handleLockRequests() {
        if (isLocked) {
            return;
        }

        // Acquire the lock and lock the database if requested by any entity
        ecsWorld.getDominion()
            .findEntitiesWith(Flags.RequestDatabaseLock.class).stream()
            .findFirst()
            .ifPresent(result -> {
                Entity entity = result.entity();
                entity.removeType(Flags.RequestDatabaseLock.class);
                entity.add(new Flags.DatabaseLockAcquired());
                isLocked = true;
                LOGGER.info("Database lock acquired");
            });
    }

    // Release the lock if requested by any entity
    private void handleUnlockRequests() {
        ecsWorld.getDominion()
            .findEntitiesWith(Flags.ReleaseDatabaseLock.class).stream()
            .findFirst()
            .ifPresent(result -> {
                Entity entity = result.entity();
                entity.removeType(Flags.ReleaseDatabaseLock.class);
                entity.removeType(Flags.DatabaseLockAcquired.class);
                entity.add(new DatabaseProcessingFinished());
                isLocked = false;
                LOGGER.info("Database lock released");
            });
    }
}