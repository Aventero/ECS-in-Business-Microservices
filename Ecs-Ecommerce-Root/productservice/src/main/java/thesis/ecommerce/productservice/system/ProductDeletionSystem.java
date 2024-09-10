package thesis.ecommerce.productservice.system;

import dev.dominion.ecs.api.Entity;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.productservice.component.Flags;
import thesis.ecommerce.productservice.component.FutureResponseComponent;
import thesis.ecommerce.productservice.component.ProductIdComponent;
import thesis.ecommerce.productservice.repository.ProductRepository;

@Service
public class ProductDeletionSystem implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ProductDeletionSystem.class);

    private final ProductRepository productRepository;
    private final ECSWorld world;

    public ProductDeletionSystem(ProductRepository productRepository, ECSWorld world) {
        this.productRepository = productRepository;
        this.world = world;
    }

    @PostConstruct
    public void init() {
        world.registerSystem(this);
    }

    @Override
    public void run() {
        world.getDominion().findEntitiesWith(
            Flags.DeleteProduct.class,
            ProductIdComponent.class
        ).forEach(result -> process(result.entity()));
    }

    private void process(Entity entity) {
        try {
            UUID productId = entity.get(ProductIdComponent.class).getId();

            if (productRepository.existsById(productId)) {
                productRepository.deleteById(productId);
                logger.info("Product deleted: {}", productId);
                entity.add(new FutureResponseComponent(ResponseEntity.ok().build()));
            } else {
                logger.warn("Product not found for deletion: {}", productId);
                entity.add(new FutureResponseComponent(ResponseEntity.notFound().build()));
            }
        } catch (Exception e) {
            logger.error("Error deleting product: {}", e.getMessage());
            entity.add(new FutureResponseComponent(
                ResponseEntity.internalServerError().body("Error deleting product: " + e.getMessage())
            ));
        } finally {
            entity.removeType(Flags.DeleteProduct.class);
        }
    }
}