package thesis.ecommerce.orderservice.ecs.system.order;

import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Results.With2;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.ecs.component.Flags.FetchedOrder;
import thesis.ecommerce.orderservice.ecs.component.general.AuthenticationComponent;
import thesis.ecommerce.orderservice.ecs.component.general.FutureResponseComponent;
import thesis.ecommerce.orderservice.ecs.component.order.OrderDetailsComponent;
import thesis.ecommerce.orderservice.ecs.component.order.OrderIdsComponent;
import thesis.ecommerce.orderservice.api.dto.order.OrderDetailsDto;
import thesis.ecommerce.orderservice.api.dto.order.OrderHistoryResponseDto;
import thesis.ecommerce.orderservice.ecs.component.Flags.FetchOrder;
import thesis.ecommerce.orderservice.ecs.component.Flags.InitiateOrderFetching;
import thesis.ecommerce.orderservice.ecs.component.Flags.OrdersReady;
import thesis.ecommerce.orderservice.persistence.repository.OrderRepository;

@Service
public class ViewOrderHistorySystem implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ViewOrderHistorySystem.class);
    private final ECSWorld ecsWorld;
    private final OrderRepository orderRepository;

    public ViewOrderHistorySystem(ECSWorld ecsWorld, OrderRepository orderRepository) {
        this.ecsWorld = ecsWorld;
        this.orderRepository = orderRepository;
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        sendOrderFetchSignal();
        processReadyOrders();
    }

    private void sendOrderFetchSignal() {
        ecsWorld.getDominion().findEntitiesWith(
                InitiateOrderFetching.class,
                AuthenticationComponent.class).stream()
            .forEach(result -> {
                LOGGER.info("STARTING ORDER FETCH REQUEST");

                Entity requestEntity = result.entity();
                requestEntity.removeType(InitiateOrderFetching.class);

                String username = requestEntity.get(AuthenticationComponent.class).username();
                List<UUID> orderIds = orderRepository
                    .findOrderIdsByUsername(username).stream().map(UUID::fromString)
                    .toList();

                // End early if no orders are found
                if (orderIds.isEmpty()) {
                    completeRequest(requestEntity, ResponseEntity.noContent().build());
                    return;
                }

                requestEntity.add(new FetchOrder());
                requestEntity.add(new OrderIdsComponent(orderIds));
            });
    }

    private void processReadyOrders() {
        ecsWorld.getDominion().
            findEntitiesWith(OrdersReady.class).stream()
                .forEach(result -> finishRequest(result.entity()));
    }

    private void finishRequest(Entity requestEntity) {
        LOGGER.info("FINISHING ORDER FETCH REQUEST FOR READY ORDERS");

        requestEntity.removeType(OrdersReady.class);
        OrderHistoryResponseDto response = createOrderHistoryResponse();
        completeRequest(requestEntity, ResponseEntity.ok(response));
    }

    private OrderHistoryResponseDto createOrderHistoryResponse() {
        List<OrderDetailsDto> orderHistory = getFetchedOrders().stream()
            .map(fetchedOrder -> fetchedOrder.get(OrderDetailsComponent.class)
                .orderDetails()).toList();

        return new OrderHistoryResponseDto(orderHistory);
    }

    private List<Entity> getFetchedOrders() {
        return ecsWorld.getDominion().findEntitiesWith(FetchedOrder.class, OrderDetailsComponent.class).stream()
            .map(With2::entity).toList();
    }

    private void deleteFetchedOrders() {
        ecsWorld.getDominion().findEntitiesWith(FetchedOrder.class, OrderDetailsComponent.class).stream()
            .map(With2::entity).forEach(entity -> ecsWorld.getDominion().deleteEntity(entity));
    }

    private void completeRequest(Entity requestEntity, ResponseEntity<?> response) {
        requestEntity.add(new FutureResponseComponent(response));
        deleteFetchedOrders();
    }
}