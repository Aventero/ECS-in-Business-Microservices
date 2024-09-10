package thesis.ecommerce.orderservice.ecs.system.order;

import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Results.With2;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.ecs.component.Flags.FetchedOrder;
import thesis.ecommerce.orderservice.ecs.component.general.FutureResponseComponent;
import thesis.ecommerce.orderservice.ecs.component.order.OrderDetailsComponent;
import thesis.ecommerce.orderservice.api.dto.order.OrderDetailsDto;
import thesis.ecommerce.orderservice.ecs.component.Flags.OrdersReady;
import thesis.ecommerce.orderservice.ecs.component.Flags.ViewOrder;

@Service
public class ViewOrderSystem implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ViewOrderSystem.class);
    private final ECSWorld ecsWorld;

    public ViewOrderSystem(ECSWorld ecsWorld) {
        this.ecsWorld = ecsWorld;
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        ecsWorld.getDominion()
            .findEntitiesWith(
                OrdersReady.class,
                ViewOrder.class)
            .stream()
            .forEach(result -> processViewOrder( result.entity()));
    }

    private void processViewOrder(Entity requestEntity) {
        LOGGER.info("PROCESSING VIEW ORDER REQUEST");

        requestEntity.removeType(ViewOrder.class);
        requestEntity.removeType(OrdersReady.class);
        finishRequest(requestEntity);
    }

    private void finishRequest(Entity requestEntity) {
        OrderDetailsDto responseDto = getFetchedOrders().getFirst().get(OrderDetailsComponent.class).orderDetails();
        LOGGER.info("Finish view order request for order ID: {}", responseDto.orderId());
        completeRequest(requestEntity, ResponseEntity.ok(responseDto));
    }

    private List<Entity> getFetchedOrders() {
        return ecsWorld.getDominion().findEntitiesWith(FetchedOrder.class, OrderDetailsComponent.class).stream()
            .map(With2::entity).toList();
    }

    private void completeRequest(Entity requestEntity, ResponseEntity<?> response) {
        requestEntity.add(new FutureResponseComponent(response));
        deleteFetchedOrders();
    }

    private void deleteFetchedOrders() {
        ecsWorld.getDominion().findEntitiesWith(FetchedOrder.class, OrderDetailsComponent.class).stream()
            .map(With2::entity).forEach(entity -> ecsWorld.getDominion().deleteEntity(entity));
    }

}