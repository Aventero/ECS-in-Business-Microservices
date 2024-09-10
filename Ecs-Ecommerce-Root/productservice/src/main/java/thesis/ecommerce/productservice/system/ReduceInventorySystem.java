package thesis.ecommerce.productservice.system;

import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Results.With1;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.productservice.component.Flags;
import thesis.ecommerce.productservice.component.Flags.ReduceInventory;
import thesis.ecommerce.productservice.component.FutureResponseComponent;
import thesis.ecommerce.productservice.component.ProductIdComponent;
import thesis.ecommerce.productservice.component.StockComponent;
import thesis.ecommerce.productservice.model.ProductModel;
import thesis.ecommerce.productservice.repository.ProductRepository;

@Service
public class ReduceInventorySystem implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReduceInventorySystem.class);
    private final ECSWorld ecsWorld;
    private final ProductRepository productRepository;

    public ReduceInventorySystem(ECSWorld ecsWorld, ProductRepository productRepository) {
        this.ecsWorld = ecsWorld;
        this.productRepository = productRepository;
        ecsWorld.registerSystem(this);
    }

    @Override
    public void run() {
        ecsWorld.getDominion()
            .findEntitiesWith(Flags.ReduceInventory.class)
            .forEach(this::processWithLock);
    }

    // Process the entity with database lock acquired
    // or request a lock if not already requested
    private void processWithLock(With1<ReduceInventory> result) {
        Entity entity = result.entity();
        if (entity.has(Flags.DatabaseLockAcquired.class)) {
            processInventoryReduction(entity);
        } else if (!entity.has(Flags.RequestDatabaseLock.class)) {
            entity.add(new Flags.RequestDatabaseLock());
        }
    }

    // Reduce inventory for a product
    private void processInventoryReduction(Entity entity) {
        entity.removeType(Flags.ReduceInventory.class);

        UUID productId = entity.get(ProductIdComponent.class).getId();
        int requestedQuantity = entity.get(StockComponent.class).getQuantity();

        Optional<ProductModel> productOptional = productRepository.findById(productId);

        if (productOptional.isPresent()) {
            ProductModel product = productOptional.get();
            boolean isStockEnough = checkInventory(product, requestedQuantity);

            if (isStockEnough) {
                int newStock = product.getStock() - requestedQuantity;
                product.setStock(newStock);
                productRepository.save(product);
                LOGGER.info("Stock reduced for product {}: Requested: {}, New stock: {}",
                    productId, requestedQuantity, newStock);
            }

            entity.add(new FutureResponseComponent(ResponseEntity.ok(isStockEnough)));
        } else {
            LOGGER.warn("Product not found for ID: {}", productId);
            entity.add(new FutureResponseComponent(ResponseEntity.ok(false)));
        }

        // Release the lock
        entity.add(new Flags.ReleaseDatabaseLock());
    }

    private boolean checkInventory(ProductModel product, int requestedQuantity) {
        boolean isAvailable = product.getStock() >= requestedQuantity;
        LOGGER.info("Inventory check for product {}: Requested: {}, Available: {}, Result: {}",
            product.getId(), requestedQuantity, product.getStock(), isAvailable);
        return isAvailable;
    }
}