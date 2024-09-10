package thesis.ecommerce.orderservice.ecs.system.order;

import dev.dominion.ecs.api.Entity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.ecs.component.Flags.DeleteOrderEntity;
import thesis.ecommerce.orderservice.ecs.component.Flags.OrderCompleted;
import thesis.ecommerce.orderservice.ecs.component.Flags.RequiresCancellation;
import thesis.ecommerce.orderservice.ecs.component.Flags.SyncOrder;
import thesis.ecommerce.orderservice.ecs.component.order.OrderCancellationComponent;
import thesis.ecommerce.orderservice.ecs.component.order.OrderIdComponent;
import thesis.ecommerce.orderservice.api.dto.order.OrderStatus;
import thesis.ecommerce.orderservice.ecs.component.order.OrderStatusComponent;

@Service
public class CancelOrderSystem implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancelOrderSystem.class);
    private final ECSWorld ecsWorld;

    public CancelOrderSystem(ECSWorld ecsWorld) {
        this.ecsWorld = ecsWorld;
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        ecsWorld.getDominion()
            .findEntitiesWith(
                RequiresCancellation.class,
                OrderIdComponent.class,
                OrderCancellationComponent.class,
                OrderStatusComponent.class)
            .stream().forEach(result -> cancelOrder(result.entity()));
    }

    private void cancelOrder(Entity orderEntity) {
        LOGGER.info("PROCESSING CANCEL ORDER");

        UUID orderId = orderEntity.get(OrderIdComponent.class).orderId();
        List<String> cancellationReasons = orderEntity.get(OrderCancellationComponent.class).getReasons();

        LOGGER.info("Cancelling order: {}. Reasons: {}", orderId, String.join(", ", cancellationReasons));
        removeOrderFromActiveOrders(orderEntity);
    }

    private void removeOrderFromActiveOrders(Entity orderEntity) {
        orderEntity.removeType(RequiresCancellation.class);
        orderEntity.removeType(OrderCancellationComponent.class);
        orderEntity.removeType(OrderStatusComponent.class);
        orderEntity.add(new OrderStatusComponent(OrderStatus.CANCELLED, Instant.now()));
        orderEntity.add(new SyncOrder());
        orderEntity.add(new OrderCompleted());
        orderEntity.add(new DeleteOrderEntity());
    }

}