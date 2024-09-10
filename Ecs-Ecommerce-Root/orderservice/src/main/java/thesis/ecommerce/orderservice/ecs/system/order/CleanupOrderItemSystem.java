package thesis.ecommerce.orderservice.ecs.system.order;

import dev.dominion.ecs.api.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.ecs.component.order.OrderItemIdComponent;
import thesis.ecommerce.orderservice.ecs.component.Flags.DeleteOrderItemEntity;
import thesis.ecommerce.orderservice.ecs.component.Flags.SyncOrderItem;

@Service
public class CleanupOrderItemSystem implements Runnable {

    private final ECSWorld ecsWorld;
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupOrderItemSystem.class);

    public CleanupOrderItemSystem(ECSWorld ecsWorld) {
        this.ecsWorld = ecsWorld;
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        ecsWorld.getDominion().findEntitiesWith(DeleteOrderItemEntity.class)
            .forEach(result -> process(result.entity()));
    }

    private void process(Entity orderItemEntity) {
        LOGGER.info("PROCESSING DELETE ORDER ITEM ENTITY");

        if (orderItemEntity.has(SyncOrderItem.class)) {
            return;
        }
        LOGGER.info("Removing orderItem entity orderItemId: {}",
            orderItemEntity.get(OrderItemIdComponent.class).orderItemId());

        ecsWorld.getDominion().deleteEntity(orderItemEntity);
    }

}