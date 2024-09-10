package thesis.ecommerce.productservice.system;

import dev.dominion.ecs.api.Entity;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.productservice.component.*;
import thesis.ecommerce.productservice.model.ProductModel;
import thesis.ecommerce.productservice.repository.ProductRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class ProductReadingSystem implements Runnable {
    private final ECSWorld ecsWorld;
    private final ProductRepository productRepository;

    public ProductReadingSystem(ECSWorld ecsWorld, ProductRepository productRepository) {
        this.ecsWorld = ecsWorld;
        this.productRepository = productRepository;
    }

    @PostConstruct
    public void init() {
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        ecsWorld.getDominion().findEntitiesWith(
                Flags.GetProduct.class,
                ProductIdComponent.class
        ).forEach(result -> process(result.entity()));
    }

    private void process(Entity entity) {
        try {
            UUID productId = entity.get(ProductIdComponent.class).getId();
            Optional<ProductModel> productOpt = productRepository.findById(productId);

            if (productOpt.isPresent()) {
                ProductModel product = productOpt.get();
                entity.add(new ProductDetailsComponent(product.getName(), product.getDescription()));
                entity.add(new PriceComponent(product.getPrice()));
                entity.add(new StockComponent(product.getStock()));
                entity.add(new CategoryComponent(product.getCategory()));
                entity.add(new ProductDateComponent(product.getCreationDate(), product.getLastUpdated()));
                entity.add(new FutureResponseComponent(ResponseEntity.ok(product)));
            } else {
                entity.add(new FutureResponseComponent(ResponseEntity.notFound().build()));
            }
        } catch (Exception e) {
            String errorMessage = "Failed to read product: " + e.getMessage();
            entity.add(new FutureResponseComponent(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage)));
        } finally {
            entity.removeType(Flags.GetProduct.class);
        }
    }
}