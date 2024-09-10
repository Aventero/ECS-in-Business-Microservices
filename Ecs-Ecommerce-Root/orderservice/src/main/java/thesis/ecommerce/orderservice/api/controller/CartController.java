package thesis.ecommerce.orderservice.api.controller;

import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Results.With3;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.api.dto.cart.AddToCartRequestDto;
import thesis.ecommerce.orderservice.api.dto.cart.RemoveFromCartRequestDto;
import thesis.ecommerce.orderservice.ecs.component.Flags;
import thesis.ecommerce.orderservice.ecs.component.Flags.Cart;
import thesis.ecommerce.orderservice.ecs.component.cart.CartIdComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.CartItemIdComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.ProductReferenceComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.QuantityComponent;
import thesis.ecommerce.orderservice.ecs.component.general.AuthenticationComponent;
import thesis.ecommerce.orderservice.ecs.component.general.CompletableFutureComponent;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final ECSWorld ecsWorld;
    private final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CartController.class);

    public CartController(ECSWorld ecsWorld) {
        this.ecsWorld = ecsWorld;
    }

    @PostMapping("/add")
    public CompletableFuture<ResponseEntity<?>> addToCart(
        @RequestBody AddToCartRequestDto request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (String) authentication.getPrincipal();
        String token = (String) authentication.getCredentials();

        Entity entity = ecsWorld.createEntity();
        entity.add(new AuthenticationComponent(username, token));
        entity.add(new ProductReferenceComponent(UUID.fromString(request.getProductId())));
        entity.add(new QuantityComponent(request.getQuantity()));
        entity.add(new Flags.CreateCart());
        entity.add(new Flags.RetrieveProduct());
        entity.add(new Flags.AddItemToCart());

        CompletableFuture<ResponseEntity<?>> future = new CompletableFuture<>();
        entity.add(new CompletableFutureComponent(future));

        return future;
    }

    @PostMapping("/remove")
    public CompletableFuture<ResponseEntity<?>> removeFromCart(
        @RequestBody RemoveFromCartRequestDto request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (String) authentication.getPrincipal();
        String token = (String) authentication.getCredentials();

        var entity = ecsWorld.createEntity();
        entity.add(new AuthenticationComponent(username, token));
        entity.add(new CartItemIdComponent(UUID.fromString(request.cartItemId())));
        entity.add(new Flags.RemoveItemFromCart());

        CompletableFuture<ResponseEntity<?>> future = new CompletableFuture<>();
        entity.add(new CompletableFutureComponent(future));

        return future;
    }

    @PostMapping("/empty")
    public ResponseEntity<String> clearCart() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (String) authentication.getPrincipal();

        Optional<Entity> userCarts = ecsWorld.getDominion()
            .findEntitiesWith(Cart.class, CartIdComponent.class, AuthenticationComponent.class)
            .stream()
            .filter(result -> result.comp3().username().equals(username))
            .map(With3::entity)
            .findFirst();

        if (userCarts.isEmpty()) {
            LOGGER.warn("No cart found for user: {}", username);
            return ResponseEntity.badRequest().body("Cart not found");
        }

        Entity cart = userCarts.get();
        CartIdComponent cartIdComp = cart.get(CartIdComponent.class);
        LOGGER.info("Processing cart for user: {}, CartID: {}", username, cartIdComp.cartId());

        if (!cart.has(Flags.EmptyCart.class)) {
            cart.add(new Flags.EmptyCart());
        }

        return ResponseEntity.ok("Started cart clearing process for user: " + username);
    }

    @GetMapping
    public CompletableFuture<ResponseEntity<?>> viewCart() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (String) authentication.getPrincipal();
        String token = (String) authentication.getCredentials();

        var entity = ecsWorld.createEntity();
        entity.add(new AuthenticationComponent(username, token));
        entity.add(new Flags.RetrieveProductsForCart());
        entity.add(new Flags.CreateCart());
        entity.add(new Flags.ViewCart());

        CompletableFuture<ResponseEntity<?>> future = new CompletableFuture<>();
        entity.add(new CompletableFutureComponent(future));

        return future;
    }
}