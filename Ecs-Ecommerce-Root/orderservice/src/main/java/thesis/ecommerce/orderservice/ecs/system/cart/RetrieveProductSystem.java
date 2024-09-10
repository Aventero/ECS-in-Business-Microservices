package thesis.ecommerce.orderservice.ecs.system.cart;

import dev.dominion.ecs.api.Entity;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.orderservice.ecs.component.Flags.ProductRetrieved;
import thesis.ecommerce.orderservice.ecs.component.Flags.RetrieveProduct;
import thesis.ecommerce.orderservice.ecs.component.cart.ProductReferenceComponent;
import thesis.ecommerce.orderservice.ecs.component.cart.ProductResponseComponent;
import thesis.ecommerce.orderservice.ecs.component.general.AuthenticationComponent;
import thesis.ecommerce.orderservice.ecs.component.general.FutureResponseComponent;
import thesis.ecommerce.orderservice.external.ProductServiceClient;

@Service
public class RetrieveProductSystem implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetrieveProductSystem.class);
    private final ECSWorld ecsWorld;
    private final ProductServiceClient productServiceClient;

    public RetrieveProductSystem(ECSWorld ecsWorld, ProductServiceClient productServiceClient) {
        this.ecsWorld = ecsWorld;
        this.productServiceClient = productServiceClient;
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        ecsWorld.getDominion()
            .findEntitiesWith(
                RetrieveProduct.class,
                ProductReferenceComponent.class,
                AuthenticationComponent.class).stream()
            .forEach(result -> retrieveProduct(result.entity()));
    }

    private void retrieveProduct(Entity processEntity) {
        LOGGER.info("PROCESSING RETRIEVE PRODUCT REQUEST");

        UUID productId = processEntity.get(ProductReferenceComponent.class).productItemId();
        String token = processEntity.get(AuthenticationComponent.class).token();
        processEntity.removeType(RetrieveProduct.class);

        LOGGER.info("Retrieving product information for product ID: {}", productId);
        productServiceClient.getProduct(productId, token)
            .doOnSuccess(productDto -> {
                processEntity.add(new ProductResponseComponent(List.of(productDto)));
                processEntity.add(new ProductRetrieved());
                LOGGER.info("Successfully retrieved product information for product ID: {}", productId);
            })
            .onErrorResume(error -> {
                handleRetrievalError(processEntity, error, productId);
                return Mono.empty();
            })
            .subscribe();
    }

    private void handleRetrievalError(Entity entity, Throwable error, UUID productId) {
        String errorMessage;
        if (error instanceof WebClientResponseException.NotFound) {
            errorMessage = "Product not found";
            LOGGER.warn("Product not found for ID: {}", productId);
        } else {
            errorMessage = "Error retrieving product information";
            LOGGER.error("Error retrieving product information for ID: {}", productId, error);
        }
        entity.add(new FutureResponseComponent(ResponseEntity.badRequest().body(errorMessage)));
    }
}