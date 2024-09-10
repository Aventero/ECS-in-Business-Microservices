package thesis.ecommerce.orderservice.ecs.system.order;

import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Results.With2;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.api.dto.order.OrderStatus;
import thesis.ecommerce.orderservice.ecs.component.Flags.Cart;
import thesis.ecommerce.orderservice.ecs.component.Flags.CartItem;
import thesis.ecommerce.orderservice.ecs.component.Flags.EmptyCart;
import thesis.ecommerce.orderservice.ecs.component.Flags.Order;
import thesis.ecommerce.orderservice.ecs.component.Flags.OrderItem;
import thesis.ecommerce.orderservice.ecs.component.Flags.PlaceOrder;
import thesis.ecommerce.orderservice.ecs.component.Flags.SyncOrderItem;
import thesis.ecommerce.orderservice.ecs.component.Flags.UpdateInventory;
import thesis.ecommerce.orderservice.ecs.component.cart.CartIdComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.CartItemsComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.ProductReferenceComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.QuantityComponent;
import thesis.ecommerce.orderservice.ecs.component.general.AuthenticationComponent;
import thesis.ecommerce.orderservice.ecs.component.general.FutureResponseComponent;
import thesis.ecommerce.orderservice.ecs.component.general.PriceComponent;
import thesis.ecommerce.orderservice.ecs.component.order.CustomerInfoComponent;
import thesis.ecommerce.orderservice.ecs.component.order.OrderIdComponent;
import thesis.ecommerce.orderservice.ecs.component.order.OrderItemIdComponent;
import thesis.ecommerce.orderservice.ecs.component.order.OrderStatusComponent;
import thesis.ecommerce.orderservice.ecs.component.order.PaymentInfoComponent;
import thesis.ecommerce.orderservice.ecs.component.order.ShippingInfoComponent;

@Service
public class PlaceOrderSystem implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaceOrderSystem.class);
    private final ECSWorld ecsWorld;

    public PlaceOrderSystem(ECSWorld ecsWorld) {
        this.ecsWorld = ecsWorld;
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        ecsWorld.getDominion()
            .findEntitiesWith(
                PlaceOrder.class,
                AuthenticationComponent.class)
            .forEach(result -> processPlaceOrder(result.entity()));
    }


    private void processPlaceOrder(Entity processEntity) {
        LOGGER.info("PROCESSING PLACE ORDER");

        processEntity.removeType(PlaceOrder.class);
        String username = processEntity.get(AuthenticationComponent.class).username();

        Entity cartEntity = findCartForUser(username);
        if (cartEntity == null) {
            LOGGER.warn("Cart not found for user: {}", username);
            processEntity.add(
                new FutureResponseComponent(ResponseEntity.badRequest().body("Cart not found")));
            return;
        }

        boolean isCartEmpty = cartEntity.get(CartItemsComponent.class).getCartItemIds().isEmpty();
        if (isCartEmpty) {
            LOGGER.warn("Cart is empty for user: {}", username);
            processEntity.add(
                new FutureResponseComponent(ResponseEntity.badRequest().body("Cart is empty")));
            return;
        }

        Entity orderEntity = createOrderFromCart(cartEntity, processEntity);
        List<Entity> orderItems = createOrderItemsFromCart(cartEntity, orderEntity);
        orderEntity.add(new PriceComponent(calculateTotal(orderItems)));
        cartEntity.add(new EmptyCart());

        LOGGER.info("Order created with ID: {}", orderEntity.get(OrderIdComponent.class).orderId());

        processEntity.add(new FutureResponseComponent(
            ResponseEntity.ok("Order placed successfully. Order ID: " +
                orderEntity.get(OrderIdComponent.class).orderId())));
    }

    // Find the Cart entity for the given username
    private Entity findCartForUser(String username) {
        return ecsWorld.getDominion()
            .findEntitiesWith(Cart.class, AuthenticationComponent.class).stream()
            .filter(result -> result.comp2().username().equals(username))
            .findFirst()
            .map(With2::entity)
            .orElse(null);
    }

    // Create a new Order entity from the Cart entity
    private Entity createOrderFromCart(Entity cartEntity, Entity processEntity) {
        Entity orderEntity = ecsWorld.createEntity();
        String username = processEntity.get(AuthenticationComponent.class).username();
        String token = processEntity.get(AuthenticationComponent.class).token();

        orderEntity.add(new CartIdComponent(cartEntity.get(CartIdComponent.class).cartId()));
        orderEntity.add(new AuthenticationComponent(username, token));
        orderEntity.add(new OrderIdComponent(UUID.randomUUID()));
        orderEntity.add(new OrderStatusComponent(OrderStatus.CREATED, Instant.now()));
        orderEntity.add(new UpdateInventory());
        orderEntity.add(new Order());

        orderEntity.add(processEntity.get(CustomerInfoComponent.class));
        orderEntity.add(processEntity.get(PaymentInfoComponent.class));
        orderEntity.add(processEntity.get(ShippingInfoComponent.class));

        return orderEntity;
    }

    // Create a new OrderItem entity for each CartItem entity
    private List<Entity> createOrderItemsFromCart(Entity cartEntity, Entity orderEntity) {
        List<Entity> orderItems = new ArrayList<>();
        ecsWorld.getDominion()
            .findEntitiesWith(CartItem.class, CartIdComponent.class).stream()
            .filter(result -> result.comp2().cartId().equals(cartEntity.get(CartIdComponent.class).cartId()))
            .forEach(result -> {
                Entity cartItem = result.entity();
                Entity orderItemEntity = ecsWorld.createEntity();
                orderItemEntity.add(new OrderItemIdComponent(UUID.randomUUID()));
                orderItemEntity.add(new OrderIdComponent(orderEntity.get(OrderIdComponent.class).orderId()));
                orderItemEntity.add(
                    new ProductReferenceComponent(cartItem.get(ProductReferenceComponent.class).productItemId()));
                orderItemEntity.add(new QuantityComponent(cartItem.get(QuantityComponent.class).getQuantity()));
                orderItemEntity.add(new PriceComponent(cartItem.get(PriceComponent.class).getPrice()));
                orderItemEntity.add(new OrderItem());
                orderItemEntity.add(new SyncOrderItem());
                orderItems.add(orderItemEntity);
            });
        return orderItems;
    }

    private BigDecimal calculateTotal(List<Entity> orderItemEntities) {
        return orderItemEntities.stream()
            .map(entity -> {
                QuantityComponent quantity = entity.get(QuantityComponent.class);
                PriceComponent price = entity.get(PriceComponent.class);
                return price.getPrice().multiply(BigDecimal.valueOf(quantity.getQuantity()));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}