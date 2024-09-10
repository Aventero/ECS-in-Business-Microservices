package thesis.ecommerce.orderservice.ecs.system.order;

import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Results.With2;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.ecs.component.Flags.InventoryUpdateSuccess;
import thesis.ecommerce.orderservice.ecs.component.Flags.Order;
import thesis.ecommerce.orderservice.ecs.component.Flags.OrderItem;
import thesis.ecommerce.orderservice.ecs.component.Flags.RequiresCancellation;
import thesis.ecommerce.orderservice.ecs.component.Flags.UpdateInventory;
import thesis.ecommerce.orderservice.ecs.component.cart.ProductReferenceComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.QuantityComponent;
import thesis.ecommerce.orderservice.ecs.component.general.AuthenticationComponent;
import thesis.ecommerce.orderservice.ecs.component.order.OrderCancellationComponent;
import thesis.ecommerce.orderservice.ecs.component.order.OrderIdComponent;
import thesis.ecommerce.orderservice.external.ProductServiceClient;

@Service
public class ProcessInventorySystem implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessInventorySystem.class);
    private final ECSWorld ecsWorld;
    private final ProductServiceClient productServiceClient;

    public ProcessInventorySystem(ECSWorld ecsWorld, ProductServiceClient productServiceClient) {
        this.ecsWorld = ecsWorld;
        this.productServiceClient = productServiceClient;
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        ecsWorld.getDominion()
            .findEntitiesWith(
                Order.class,
                UpdateInventory.class).stream()
            .forEach(entity -> processInventory(entity.entity()));
    }

    private void processInventory(Entity order) {
        LOGGER.info("PROCESSING INVENTORY FOR ORDER");

        order.removeType(UpdateInventory.class);

        String token = order.get(AuthenticationComponent.class).token();
        UUID orderId = order.get(OrderIdComponent.class).orderId();

        // Group order items by product ID and sum their quantities
        Map<UUID, Integer> productQuantities = getOrderItems(orderId).stream()
            .collect(Collectors.groupingBy(
                item -> item.get(ProductReferenceComponent.class).productItemId(),
                Collectors.summingInt(item -> item.get(QuantityComponent.class).getQuantity())
            ));

        Flux.fromIterable(productQuantities.entrySet())
            .flatMap(entry -> reduceInventoryByQuantity(entry.getKey(), entry.getValue(), token))
            .all(Boolean::booleanValue)
            .flatMap(allSuccessful ->
                Mono.fromRunnable(() -> handleInventoryUpdateResult(order, allSuccessful))
                    .thenReturn(allSuccessful)
            )
            .onErrorResume(error -> {
                handleError(order, error);
                return Mono.empty();
            })
            .subscribe();
    }

    private Mono<Boolean> reduceInventoryByQuantity(UUID productId, int totalQuantity, String token) {
        return productServiceClient.reduceInventory(productId, totalQuantity, token);
    }

    private List<Entity> getOrderItems(UUID orderId) {
        return ecsWorld.getDominion()
            .findEntitiesWith(OrderItem.class, OrderIdComponent.class).stream()
            .filter(result -> result.comp2().orderId().equals(orderId))
            .map(With2::entity)
            .toList();
    }

    private void handleError(Entity order, Throwable error) {
        LOGGER.error("Error during inventory update for order {}: {}",
            order.get(OrderIdComponent.class).orderId(), error.getMessage());
        order.add(new OrderCancellationComponent(List.of("Error during inventory update: " + error.getMessage())));
        order.add(new RequiresCancellation());
    }

    private void handleInventoryUpdateResult(Entity order, boolean allSuccessful) {
        if (!allSuccessful) {
            LOGGER.warn("Inventory update failed for order {}. Flagging for cancellation.",
                order.get(OrderIdComponent.class).orderId());
            order.add(new RequiresCancellation());
            order.add(new OrderCancellationComponent(List.of("Inventory update failed")));
        } else {
            LOGGER.info("Inventory successfully updated for order {}",
                order.get(OrderIdComponent.class).orderId());
            order.add(new InventoryUpdateSuccess());
        }
    }
}