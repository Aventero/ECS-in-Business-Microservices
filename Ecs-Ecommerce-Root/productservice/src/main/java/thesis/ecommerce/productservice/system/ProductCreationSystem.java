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

import java.time.Instant;

@Service
public class ProductCreationSystem implements Runnable {

    private final ECSWorld ecsWorld;
    private final ProductRepository productRepository;

    public ProductCreationSystem(ECSWorld ecsWorld, ProductRepository productRepository) {
        this.ecsWorld = ecsWorld;
        this.productRepository = productRepository;
    }

    @PostConstruct
    public void init() {
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        ecsWorld.getDominion().findEntitiesWith(Flags.CreateProduct.class,
                ProductDetailsComponent.class,
                PriceComponent.class,
                StockComponent.class,
                CategoryComponent.class).forEach(result -> process(result.entity()));
    }

    private void process(Entity entity) {
        try {
            ProductDetailsComponent details = entity.get(ProductDetailsComponent.class);
            PriceComponent price = entity.get(PriceComponent.class);
            StockComponent stock = entity.get(StockComponent.class);
            CategoryComponent category = entity.get(CategoryComponent.class);

            // Create a new product object
            ProductModel product = new ProductModel();
            product.setName(details.getName());
            product.setDescription(details.getDescription());
            product.setPrice(price.getPrice());
            product.setStock(stock.getQuantity());
            product.setCategory(category.getName());

            // Set the creation and update dates
            Instant now = Instant.now();
            product.setCreationDate(now);
            product.setLastUpdated(now);

            // Save the product to the database
            ProductModel savedProduct = productRepository.save(product);

            // Add the product ID to the entity
            entity.add(new ProductIdComponent(savedProduct.getId()));

            // Create a success response
            entity.add(new FutureResponseComponent(ResponseEntity.ok(savedProduct)));

        } catch (Exception e) {
            String errorMessage = "Failed to create product: " + e.getMessage();
            entity.add(new FutureResponseComponent(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage)));
        } finally {
            entity.removeType(Flags.CreateProduct.class);
        }
    }
}