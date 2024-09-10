package thesis.ecommerce.orderservice.ecs.system.cart;

import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Results.With2;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.ecs.component.Flags;
import thesis.ecommerce.orderservice.ecs.component.cart.CartIdComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.ProductReferenceComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.ProductResponseComponent;
import thesis.ecommerce.orderservice.ecs.component.general.AuthenticationComponent;
import thesis.ecommerce.orderservice.ecs.component.general.FutureResponseComponent;
import thesis.ecommerce.orderservice.external.ProductServiceClient;

@Service
public class RetrieveCartProductsSystem implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        RetrieveCartProductsSystem.class);
    private final ECSWorld ecsWorld;
    private final ProductServiceClient productServiceClient;

    public RetrieveCartProductsSystem(ECSWorld ecsWorld,
        ProductServiceClient productServiceClient) {
        this.ecsWorld = ecsWorld;
        this.productServiceClient = productServiceClient;
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        ecsWorld.getDominion()
            .findEntitiesWith(
                Flags.CartCreated.class,
                Flags.RetrieveProductsForCart.class,
                AuthenticationComponent.class).stream()
            .forEach(result -> processProductRetrieval(result.entity()));
    }

    private void processProductRetrieval(Entity processEntity) {
        LOGGER.info("PROCESSING RETRIEVE CART PRODUCTS REQUEST");

        processEntity.removeType(Flags.RetrieveProductsForCart.class);
        String jwtToken = processEntity.get(AuthenticationComponent.class).token();
        String username = processEntity.get(AuthenticationComponent.class).username();

        Optional<Entity> cart = getCart(username);
        if (cart.isEmpty()) {
            processEntity.add(new FutureResponseComponent(
                ResponseEntity.badRequest().body("Cart not found for user " + username)));
            return;
        }

        List<Entity> cartItemEntities = getCartItems(cart.get().get(CartIdComponent.class).cartId());
        Map<UUID, List<Entity>> productIdToEntiesMap = cartItemEntities.stream()
            .collect(Collectors.groupingBy(
                entity -> entity.get(ProductReferenceComponent.class).productItemId()
            ));

        // Get ProductInfo and add it to the cart item entities
        addProductInfoToCartItems(processEntity, productIdToEntiesMap, jwtToken);
    }

    private void addProductInfoToCartItems(Entity processEntity,
        Map<UUID, List<Entity>> productIdToEntiiesMap,
        String jwtToken) {
        Flux.fromIterable(productIdToEntiiesMap.keySet())
            .flatMap(productId -> productServiceClient.getProduct(productId, jwtToken))
            .collectList()
            .doOnNext(products -> {
                processEntity.add(new Flags.CartProductsRetrieved());
                processEntity.add(new ProductResponseComponent(products));
            })
            .doOnError(WebClientResponseException.Forbidden.class, error -> {
                LOGGER.error("Authentication failed");
                processEntity.add(new FutureResponseComponent(
                    ResponseEntity.badRequest().body("Authentication failed")));
            })
            .doOnError(error -> {
                if (!(error instanceof WebClientResponseException.Forbidden)) {
                    LOGGER.error("Product retrieval failed", error);
                    processEntity.add(new FutureResponseComponent(
                        ResponseEntity.badRequest().body("Retrieval failed")));
                }
            })
            .subscribe();
    }

    // Find entities matching cart item IDs and get their product references
    private List<Entity> getCartItems(UUID cartId) {
        return ecsWorld.getDominion().findEntitiesWith(CartIdComponent.class, Flags.CartItem.class)
            .stream()
            .filter(result -> result.comp1().cartId().equals(cartId))
            .map(With2::entity)
            .collect(Collectors.toList());
    }

    private Optional<Entity> getCart(String username) {
        return ecsWorld.getDominion()
            .findEntitiesWith(AuthenticationComponent.class, Flags.Cart.class)
            .stream()
            .filter(cartResult -> cartResult.comp1().username().equals(username))
            .findAny()
            .map(With2::entity);
    }
}