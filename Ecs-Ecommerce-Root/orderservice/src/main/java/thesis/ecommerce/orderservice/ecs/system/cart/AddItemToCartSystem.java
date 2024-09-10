package thesis.ecommerce.orderservice.ecs.system.cart;


import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Results;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.ecs.component.Flags;
import thesis.ecommerce.orderservice.ecs.component.cart.CartIdComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.CartItemIdComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.CartItemsComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.ProductReferenceComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.ProductResponseComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.QuantityComponent;
import thesis.ecommerce.orderservice.ecs.component.general.AuthenticationComponent;
import thesis.ecommerce.orderservice.ecs.component.general.FutureResponseComponent;
import thesis.ecommerce.orderservice.ecs.component.general.PriceComponent;
import thesis.ecommerce.orderservice.ecs.component.general.TimestampComponent;
import thesis.ecommerce.orderservice.api.dto.cart.ProductResponseDto;
import thesis.ecommerce.orderservice.persistence.model.CartItemModel;
import thesis.ecommerce.orderservice.persistence.repository.CartItemRepository;


@Service
public class AddItemToCartSystem implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddItemToCartSystem.class);
    private final ECSWorld ecsWorld;
    private final CartItemRepository cartItemRepository;

    public AddItemToCartSystem(ECSWorld ecsWorld, CartItemRepository cartItemRepository) {
        this.ecsWorld = ecsWorld;
        this.cartItemRepository = cartItemRepository;
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        processAddToCartRequests();
    }

    private void processAddToCartRequests() {
        ecsWorld.getDominion()
            .findEntitiesWith(
                Flags.AddItemToCart.class,
                Flags.CartCreated.class,
                Flags.ProductRetrieved.class,
                AuthenticationComponent.class,
                QuantityComponent.class).stream()
            .forEach(result -> addItemToCart(result.entity()));
    }

    private void addItemToCart(Entity processEntity) {
        processEntity.removeType(Flags.AddItemToCart.class);

        int quantity = processEntity.get(QuantityComponent.class).getQuantity();
        String username = processEntity.get(AuthenticationComponent.class).username();
        ProductResponseDto product = processEntity.get(ProductResponseComponent.class).productResponses().getFirst();

        Entity cartEntity = findCartInMemory(username);
        if (cartEntity == null) {
            handleCartNotFound(processEntity, username);
            return;
        }

        Entity cartItem = createCartItem(cartEntity, product, quantity);
        UUID cartItemId = cartItem.get(CartItemIdComponent.class).cartItemId();
        cartEntity.get(CartItemsComponent.class).getCartItemIds().add(cartItemId);

        updateProcessEntity(processEntity, cartItem);
    }

    private Entity createCartItem(Entity cart,
        ProductResponseDto product, int quantity) {
        Entity cartItem = ecsWorld.createEntity();
        cartItem.add(new Flags.CartItem());
        cartItem.add(new ProductReferenceComponent(product.getId()));
        cartItem.add(new PriceComponent(product.getPrice()));
        cartItem.add(new QuantityComponent(quantity));
        cartItem.add(new CartIdComponent(cart.get(CartIdComponent.class).cartId()));
        cartItem.add(new CartItemIdComponent(UUID.randomUUID()));
        cartItem.add(new TimestampComponent(Instant.now(), Instant.now()));

        CartItemModel cartItemModel = mapToCartItemModel(cartItem);
        cartItemRepository.save(cartItemModel);
        return cartItem;
    }

    private CartItemModel mapToCartItemModel(Entity item) {
        CartItemModel cartItemModel = new CartItemModel();
        cartItemModel.setCartItemId(item.get(CartItemIdComponent.class).cartItemId().toString());
        cartItemModel.setCartId(item.get(CartIdComponent.class).cartId());
        cartItemModel.setProductId(item.get(ProductReferenceComponent.class).productItemId());
        cartItemModel.setPrice(item.get(PriceComponent.class).getPrice());
        cartItemModel.setQuantity(item.get(QuantityComponent.class).getQuantity());
        return cartItemModel;
    }

    private void updateProcessEntity(Entity processEntity, Entity cartItem) {
        processEntity.add(new Flags.CartItemAdded());
        processEntity.add(new FutureResponseComponent(
            ResponseEntity.ok("Item added to cart")));
    }

    private Entity findCartInMemory(String username) {
        return ecsWorld.getDominion()
            .findEntitiesWith(AuthenticationComponent.class, Flags.Cart.class)
            .stream()
            .filter(result -> result.comp1().username().equals(username))
            .findFirst()
            .map(Results.With2::entity)
            .orElse(null);
    }

    private void handleCartNotFound(Entity processEntity, String username) {
        LOGGER.info("Cart not found for user: {}", username);
        processEntity.add(new FutureResponseComponent(ResponseEntity.notFound().build()));
    }
}