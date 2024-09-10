package thesis.ecommerce.orderservice.ecs.system.cart;


import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Results.With2;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.ecs.component.Flags;
import thesis.ecommerce.orderservice.ecs.component.cart.CartIdComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.CartItemIdComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.CartItemsComponent;
import thesis.ecommerce.orderservice.ecs.component.general.AuthenticationComponent;
import thesis.ecommerce.orderservice.persistence.repository.CartItemRepository;

@Service
public class EmptyCartSystem implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmptyCartSystem.class);
    private final ECSWorld ecsWorld;
    private final CartItemRepository cartItemRepository;

    public EmptyCartSystem(ECSWorld ecsWorld, CartItemRepository cartItemRepository) {
        this.ecsWorld = ecsWorld;
        this.cartItemRepository = cartItemRepository;
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        ecsWorld.getDominion()
            .findEntitiesWith(
                Flags.EmptyCart.class,
                CartIdComponent.class,
                AuthenticationComponent.class).stream()
            .forEach(result -> emptyCart(result.entity()));
    }


    private void emptyCart(Entity cartEntity) {
        LOGGER.info("PROCESSING EMPTY CART FOR CART");

        cartEntity.removeType(Flags.EmptyCart.class);

        UUID cartId = cartEntity.get(CartIdComponent.class).cartId();
        String username = cartEntity.get(AuthenticationComponent.class).username();

        // Clear ids inside cart entity
        cartEntity.get(CartItemsComponent.class).setCartItemIds(new HashSet<>());

        removeCartItems(cartId);
        LOGGER.info("Cart emptied successfully for user: {}, cartId: {}", username, cartId);
    }


    private void removeCartItems(UUID cartId) {
        List<Entity> cartItemEntities = ecsWorld.getDominion()
            .findEntitiesWith(Flags.CartItem.class, CartIdComponent.class).stream()
            .filter(result -> result.comp2().cartId().equals(cartId))
            .map(With2::entity)
            .toList();

        for (Entity cartItemEntity : cartItemEntities) {
            if (cartItemRepository.findById(cartItemEntity.get(CartItemIdComponent.class).cartItemId().toString()).isEmpty()) {
                LOGGER.warn("Cart item not found in repository: {}", cartItemEntity.get(CartItemIdComponent.class).cartItemId());
            } else {
                LOGGER.info("Removing cart item from repository: {}", cartItemEntity.get(CartItemIdComponent.class).cartItemId());
                cartItemRepository.deleteById(cartItemEntity.get(CartItemIdComponent.class).cartItemId().toString());
            }

            ecsWorld.getDominion().deleteEntity(cartItemEntity);
        }

        LOGGER.info("Removed {} cart items for cartId: {}", cartItemEntities.size(), cartId);
    }
}