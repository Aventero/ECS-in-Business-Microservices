package thesis.ecommerce.productservice.system;

import dev.dominion.ecs.api.Entity;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.productservice.component.Flags;
import thesis.ecommerce.productservice.component.ProductDetailsComponent;
import thesis.ecommerce.productservice.component.ProductIdComponent;
import thesis.ecommerce.productservice.component.StockComponent;
import thesis.ecommerce.productservice.model.ProductModel;
import thesis.ecommerce.productservice.repository.ProductRepository;

@Service
public class ProductUpdateSystem implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ProductUpdateSystem.class);

    private final ProductRepository productRepository;
    private final ECSWorld world;

    public ProductUpdateSystem(ProductRepository productRepository, ECSWorld world) {
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
            Flags.UpdateProduct.class,
            ProductIdComponent.class).forEach(result -> process(result.entity()));
    }

    private void process(Entity entity) {
        ProductIdComponent idComponent = entity.get(ProductIdComponent.class);
        ProductModel product = productRepository.findById(idComponent.getId())
            .orElseThrow(() -> new RuntimeException("Product not found"));

        if (entity.has(ProductDetailsComponent.class)) {
            product.setName(entity.get(ProductDetailsComponent.class).getName());
        }
        if (entity.has(ProductDetailsComponent.class)) {
            product.setDescription(entity.get(ProductDetailsComponent.class).getDescription());
        }
        if (entity.has(StockComponent.class)) {
            product.setStock(entity.get(StockComponent.class).getQuantity());
        }

        productRepository.save(product);

        logger.info("Product updated: {}", product.getId());
        entity.removeType(Flags.UpdateProduct.class);
    }
}