package thesis.ecommerce.orderservice.ecs.system.cart;

import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Results.With2;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.ecs.component.Flags;
import thesis.ecommerce.orderservice.ecs.component.cart.CartIdComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.CartItemIdComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.ProductReferenceComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.ProductResponseComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.QuantityComponent;
import thesis.ecommerce.orderservice.ecs.component.general.AuthenticationComponent;
import thesis.ecommerce.orderservice.ecs.component.general.FutureResponseComponent;
import thesis.ecommerce.orderservice.ecs.component.general.PriceComponent;
import thesis.ecommerce.orderservice.api.dto.cart.CartItemDto;
import thesis.ecommerce.orderservice.api.dto.cart.CartResponseDto;
import thesis.ecommerce.orderservice.api.dto.cart.ProductResponseDto;

@Service
public class ViewCartSystem implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ViewCartSystem.class);
    private final ECSWorld ecsWorld;

    public ViewCartSystem(ECSWorld ecsWorld) {
        this.ecsWorld = ecsWorld;
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        ecsWorld.getDominion()
            .findEntitiesWith(
                Flags.ViewCart.class,
                Flags.CartProductsRetrieved.class,
                Flags.CartCreated.class,
                AuthenticationComponent.class).stream()
            .forEach(result -> process(result.entity()));
    }

    private void process(Entity processEntity) {
        processEntity.removeType(Flags.ViewCart.class);
        LOGGER.info("PROCESSING VIEW CART REQUEST");

        try {
            Entity cartEntity = findCartForUser(processEntity);
            UUID cartId = cartEntity.get(CartIdComponent.class).cartId();
            CartResponseDto responseDto = createCartResponseDto(cartId, processEntity);
            processEntity.add(new FutureResponseComponent(ResponseEntity.ok(responseDto)));
        } catch (Exception e) {
            LOGGER.error("Error creating cart response: {}", e.getMessage());
            processEntity.add(
                new FutureResponseComponent(ResponseEntity.badRequest().body(e.getMessage())));
        }
    }

    private CartResponseDto createCartResponseDto(UUID cartId, Entity processEntity) {
        List<ProductResponseDto> products = processEntity.get(ProductResponseComponent.class).productResponses();
        List<CartItemDto> items = findCartItems(cartId, products);
        return new CartResponseDto(cartId, items, calculateTotal(items));
    }

    private Entity findCartForUser(Entity processEntity) {
        String username = processEntity.get(AuthenticationComponent.class).username();
        return ecsWorld.getDominion()
            .findEntitiesWith(Flags.Cart.class, AuthenticationComponent.class)
            .stream()
            .filter(result -> result.comp2().username().equals(username))
            .findFirst()
            .map(With2::entity)
            .orElseThrow(() -> new IllegalStateException("Cart not found for user: " + username));
    }

    private List<CartItemDto> findCartItems(UUID cartId, List<ProductResponseDto> products) {
        return ecsWorld.getDominion()
            .findEntitiesWith(
                Flags.CartItem.class,
                CartIdComponent.class,
                QuantityComponent.class,
                PriceComponent.class,
                CartItemIdComponent.class,
                ProductReferenceComponent.class
            )
            .stream()
            .filter(result -> result.comp2().cartId().equals(cartId))
            .map(result -> mapToCartItemDto(result.entity(), products))
            .toList();
    }

    private CartItemDto mapToCartItemDto(Entity cartItemDto, List<ProductResponseDto> products) {
        UUID productId = cartItemDto.get(ProductReferenceComponent.class).productItemId();

        ProductResponseDto productInfo = products.stream()
            .filter(product -> product.getId().equals(productId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Product not found for ID: " + productId));

        return new CartItemDto(
            cartItemDto.get(CartItemIdComponent.class).cartItemId(),
            cartItemDto.get(ProductReferenceComponent.class).productItemId(),
            cartItemDto.get(QuantityComponent.class).getQuantity(),
            cartItemDto.get(PriceComponent.class).getPrice(),
            productInfo
        );
    }

    private BigDecimal calculateTotal(List<CartItemDto> items) {
        return items.stream()
            .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }
}