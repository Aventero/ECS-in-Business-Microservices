package thesis.ecommerce.orderservice.ecs.system.order;


import dev.dominion.ecs.api.Entity;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.ecs.component.Flags.DeleteOrderEntity;
import thesis.ecommerce.orderservice.ecs.component.Flags.InventoryUpdateSuccess;
import thesis.ecommerce.orderservice.ecs.component.Flags.Order;
import thesis.ecommerce.orderservice.ecs.component.Flags.OrderCompleted;
import thesis.ecommerce.orderservice.ecs.component.Flags.SyncOrder;
import thesis.ecommerce.orderservice.ecs.component.order.OrderIdComponent;
import thesis.ecommerce.orderservice.api.dto.order.OrderStatus;
import thesis.ecommerce.orderservice.ecs.component.order.OrderStatusComponent;

@Service
public class CompleteOrderSystem implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompleteOrderSystem.class);
    private final ECSWorld ecsWorld;

    public CompleteOrderSystem(ECSWorld ecsWorld) {
        this.ecsWorld = ecsWorld;
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        ecsWorld.getDominion()
            .findEntitiesWith(
                Order.class,
                InventoryUpdateSuccess.class,
                OrderIdComponent.class
            ).stream()
            .forEach(result -> completeOrder(result.entity()));
    }

    private void completeOrder(Entity orderEntity) {
        LOGGER.info("PROCESSING COMPLETE ORDER");

        UUID orderId = orderEntity.get(OrderIdComponent.class).orderId();
        LOGGER.info("Completing order: {}", orderId);

        orderEntity.removeType(OrderStatusComponent.class);
        orderEntity.removeType(InventoryUpdateSuccess.class);
        orderEntity.add(new OrderStatusComponent(OrderStatus.COMPLETED, Instant.now()));
        orderEntity.add(new OrderCompleted());
        orderEntity.add(new DeleteOrderEntity());
        orderEntity.add(new SyncOrder());

        LOGGER.info("Order {} has been successfully completed", orderId);
    }
}