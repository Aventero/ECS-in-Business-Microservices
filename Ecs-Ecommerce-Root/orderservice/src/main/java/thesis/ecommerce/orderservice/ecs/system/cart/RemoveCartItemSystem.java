package thesis.ecommerce.orderservice.ecs.system.cart;

import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Results;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.ecs.component.Flags;
import thesis.ecommerce.orderservice.ecs.component.cart.CartItemIdComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.CartItemsComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.ProductReferenceComponent;
import thesis.ecommerce.orderservice.ecs.component.general.AuthenticationComponent;
import thesis.ecommerce.orderservice.ecs.component.general.FutureResponseComponent;
import thesis.ecommerce.orderservice.persistence.repository.CartItemRepository;

@Service
public class RemoveCartItemSystem implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveCartItemSystem.class);
    private final ECSWorld ecsWorld;
    private final CartItemRepository cartItemRepository;

    public RemoveCartItemSystem(ECSWorld ecsWorld, CartItemRepository cartItemRepository) {
        this.ecsWorld = ecsWorld;
        this.cartItemRepository = cartItemRepository;
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        ecsWorld.getDominion()
            .findEntitiesWith(
                Flags.RemoveItemFromCart.class,
                AuthenticationComponent.class,
                CartItemIdComponent.class).stream().forEach(result -> process(result.entity()));
    }

    private void process(Entity processEntity) {
        LOGGER.info("PROCESSING REMOVE ITEM FROM CART REQUEST");

        processEntity.removeType(Flags.RemoveItemFromCart.class);
        processEntity.removeType(ProductReferenceComponent.class);

        AuthenticationComponent auth = processEntity.get(AuthenticationComponent.class);
        Optional<Entity> cartOpt = findUserCart(auth);
        if (cartOpt.isEmpty()) {
            handleCartNotFound(processEntity, auth);
            return;
        }

        Entity cart = cartOpt.get();
        CartItemsComponent cartItems = cart.get(CartItemsComponent.class);
        UUID itemId = processEntity.get(CartItemIdComponent.class).cartItemId();
        if (!cartItems.getCartItemIds().remove(itemId)) {
            handleItemNotFound(processEntity, itemId, auth);
            return;
        }

        removeCartItemEntity(itemId, auth);
        processEntity.add(new FutureResponseComponent(ResponseEntity.ok("Item removed from cart")));
    }

    private Optional<Entity> findUserCart(AuthenticationComponent auth) {
        return ecsWorld.getDominion()
            .findEntitiesWith(Flags.Cart.class, AuthenticationComponent.class).stream().filter(
                result -> result.entity().get(AuthenticationComponent.class).username()
                    .equals(auth.username())).findFirst().map(Results.With2::entity);
    }

    private void handleCartNotFound(Entity entity, AuthenticationComponent auth) {
        LOGGER.warn("Cart not found for user: {}", auth.username());
        entity.add(new FutureResponseComponent(ResponseEntity.badRequest().body("Cart not found")));
    }

    private void handleItemNotFound(Entity entity, UUID itemId, AuthenticationComponent auth) {
        LOGGER.warn("Item {} not found in cart for user {}", itemId, auth.username());
        entity.add(new FutureResponseComponent(
            ResponseEntity.badRequest().body("Item not found in cart")));
    }

    private void removeCartItemEntity(UUID itemId, AuthenticationComponent auth) {
        ecsWorld.getDominion().findEntitiesWith(Flags.CartItem.class, CartItemIdComponent.class)
            .stream()
            .filter(result -> result.entity().get(CartItemIdComponent.class).cartItemId()
                .equals(itemId))
            .findFirst().ifPresent(result -> {
                cartItemRepository.deleteById(itemId.toString());
                LOGGER.info("Removed item {} from cart for user {}", itemId, auth.username());
                ecsWorld.getDominion().deleteEntity(result.entity());
            });
    }
}