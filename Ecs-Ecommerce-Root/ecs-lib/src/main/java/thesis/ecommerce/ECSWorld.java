package thesis.ecommerce;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Scheduler;
import java.util.logging.Logger;

public class ECSWorld {

    private static final Logger LOGGER = Logger.getLogger(ECSWorld.class.getName());
    private final Dominion dominion;
    private final Scheduler scheduler;
    private boolean isTickRateSet = false;

    public ECSWorld() {
        this.dominion = Dominion.create();
        this.scheduler = this.dominion.createScheduler();
        LOGGER.info("ECSWorld initialized");
    }

    public void registerSystem(Runnable system) {
        this.scheduler.schedule(system);
        setTickRate(30);
        LOGGER.info("SYSTEM REGISTERED: " + system.getClass().getName());
    }



    public void setTickRate(int ticksPerSecond) {
        if (!isTickRateSet) {  // 30 is the default rate
            this.scheduler.tickAtFixedRate(ticksPerSecond);
            isTickRateSet = true;
            LOGGER.info("Tick rate set to " + ticksPerSecond);
        }
    }

    public Entity createEntity(Object... components) {
        return dominion.createEntity(components);
    }

    public Dominion getDominion() {
        return dominion;
    }

}