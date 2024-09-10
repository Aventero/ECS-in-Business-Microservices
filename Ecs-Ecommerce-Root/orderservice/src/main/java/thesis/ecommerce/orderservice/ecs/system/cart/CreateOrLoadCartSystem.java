package thesis.ecommerce.orderservice.ecs.system.cart;

import dev.dominion.ecs.api.Entity;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.ecs.component.Flags;
import thesis.ecommerce.orderservice.ecs.component.Flags.CartCreated;
import thesis.ecommerce.orderservice.ecs.component.cart.CartIdComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.CartItemIdComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.CartItemsComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.ProductReferenceComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.QuantityComponent;
import thesis.ecommerce.orderservice.ecs.component.general.AuthenticationComponent;
import thesis.ecommerce.orderservice.ecs.component.general.PriceComponent;
import thesis.ecommerce.orderservice.ecs.component.general.TimestampComponent;
import thesis.ecommerce.orderservice.persistence.model.CartItemModel;
import thesis.ecommerce.orderservice.persistence.model.CartModel;
import thesis.ecommerce.orderservice.persistence.repository.CartItemRepository;
import thesis.ecommerce.orderservice.persistence.repository.CartRepository;

@Service
public class CreateOrLoadCartSystem implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateOrLoadCartSystem.class);
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ECSWorld ecsWorld;

    public CreateOrLoadCartSystem(CartRepository cartRepository,
        CartItemRepository cartItemRepository,
        ECSWorld ecsWorld) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.ecsWorld = ecsWorld;
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        ecsWorld.getDominion()
            .findEntitiesWith(
                AuthenticationComponent.class,
                Flags.CreateCart.class).stream()
            .forEach(result -> process(result.entity()));
    }

    private void process(Entity processEntity) {
        LOGGER.info("PROCESSING CREATE OR LOAD CART REQUEST");
        processEntity.removeType(Flags.CreateCart.class);

        AuthenticationComponent authenticationComponent = processEntity.get(AuthenticationComponent.class);
        String username = authenticationComponent.username();
        String token = authenticationComponent.token();

        if (userHasExistingCartInMemory(username)) {
            LOGGER.info("User {} already has a cart in memory", username);
            processEntity.add(new CartCreated());
            return;
        }

        createOrLoadCart(username, token);
        processEntity.add(new CartCreated());
    }

    private boolean userHasExistingCartInMemory(String username) {
        return ecsWorld.getDominion()
            .findEntitiesWith(AuthenticationComponent.class, Flags.Cart.class)
            .stream()
            .anyMatch(
                cartResult -> cartResult.entity().get(AuthenticationComponent.class).username()
                    .equals(username));
    }

    private void createOrLoadCart(String username, String token) {
        cartRepository.findByUsername(username).ifPresentOrElse(cart -> {
            mapAndCreateCartEntity(cart, token, createCartItemEntities(cart.getCartId()));
            LOGGER.info("Loaded existing cart from database for user: {}", username);
        }, () -> {
            createNewCart(username, token);
            LOGGER.info("Created new empty cart for user: {}", username);
        });
    }

    private List<Entity> createCartItemEntities(UUID cartId) {
        return cartItemRepository.findByCartId(cartId).stream()
            .map(this::mapAndCreateCartItemEntity)
            .toList();
    }

    private void mapAndCreateCartEntity(CartModel cart, String token, List<Entity> cartItemEntities) {
        LOGGER.info("Creating cart entity for cart: {}", cart);
        Entity cartEntity = ecsWorld.createEntity();
        cartEntity.add(new AuthenticationComponent(cart.getUsername(), token));
        cartEntity.add(new CartIdComponent(cart.getCartId()));
        cartEntity.add(new TimestampComponent(cart.getCreatedAt(), cart.getUpdatedAt()));
        cartEntity.add(new CartItemsComponent(new HashSet<>()));
        cartEntity.add(new Flags.Cart());
        cartItemEntities.forEach(
            cartItem -> cartEntity.get(CartItemsComponent.class).getCartItemIds()
                .add(cartItem.get(CartItemIdComponent.class).cartItemId()));
    }

    private Entity mapAndCreateCartItemEntity(CartItemModel cartItem) {
        LOGGER.info("Creating cart item entity for cart item: {}", cartItem);
        Entity cartItemEntity = ecsWorld.createEntity();
        cartItemEntity.add(new CartItemIdComponent(UUID.fromString(cartItem.getCartItemId())));
        cartItemEntity.add(new CartIdComponent(cartItem.getCartId()));
        cartItemEntity.add(new ProductReferenceComponent(cartItem.getProductId()));
        cartItemEntity.add(new PriceComponent(cartItem.getPrice()));
        cartItemEntity.add(new QuantityComponent(cartItem.getQuantity()));
        cartItemEntity.add(new Flags.CartItem());
        return cartItemEntity;
    }

    private void createNewCart(String username, String token) {
        LOGGER.info("Creating new cart for user: {}", username);
        Entity cart = ecsWorld.createEntity();
        cart.add(new AuthenticationComponent(username, token));
        cart.add(new CartIdComponent(UUID.randomUUID()));
        cart.add(new TimestampComponent(Instant.now(), Instant.now()));
        cart.add(new CartItemsComponent(new HashSet<>()));
        cart.add(new Flags.Cart());

        CartModel cartModel = mapToCartModel(cart);
        cartRepository.save(cartModel);
    }

    private CartModel mapToCartModel(Entity cart) {
        CartModel cartModel = new CartModel();
        cartModel.setCartId(cart.get(CartIdComponent.class).cartId());
        cartModel.setUsername(cart.get(AuthenticationComponent.class).username());
        cartModel.setCreatedAt(cart.get(TimestampComponent.class).getCreatedAt());
        cartModel.setUpdatedAt(cart.get(TimestampComponent.class).getUpdatedAt());
        return cartModel;
    }
}