package thesis.ecommerce.orderservice.ecs.system.order;

import dev.dominion.ecs.api.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.ecs.component.order.OrderIdComponent;
import thesis.ecommerce.orderservice.ecs.component.Flags.DeleteOrderEntity;
import thesis.ecommerce.orderservice.ecs.component.Flags.DeleteOrderItemEntity;
import thesis.ecommerce.orderservice.ecs.component.Flags.OrderItem;
import thesis.ecommerce.orderservice.ecs.component.Flags.SyncOrder;

@Service
public class CleanupOrderSystem implements Runnable {

    private final ECSWorld ecsWorld;
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanupOrderSystem.class);

    public CleanupOrderSystem(ECSWorld ecsWorld) {
        this.ecsWorld = ecsWorld;
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        ecsWorld.getDominion().findEntitiesWith(DeleteOrderEntity.class)
            .forEach(result -> process(result.entity()));
    }

    private void process(Entity orderEntity) {
        LOGGER.info("PROCESSING DELETE ORDER ENTITY");

        if (orderEntity.has(SyncOrder.class)) {
            return;
        }
        LOGGER.info("Removing order entity from world orderId: {}",
            orderEntity.get(OrderIdComponent.class).orderId());

        // Flag all order items for deletion
        ecsWorld.getDominion().findEntitiesWith(OrderItem.class, OrderIdComponent.class)
            .forEach(result -> markOrderItemsForDeletion(orderEntity, result.entity()));

        ecsWorld.getDominion().deleteEntity(orderEntity);
    }

    private static void markOrderItemsForDeletion(Entity orderEntity, Entity orderItem) {
        if (orderItem.get(OrderIdComponent.class).orderId().equals(orderEntity.get(OrderIdComponent.class).orderId())) {
            if (!orderItem.has(DeleteOrderItemEntity.class)) {
                orderItem.add(new DeleteOrderItemEntity());
            }
        }
    }
}