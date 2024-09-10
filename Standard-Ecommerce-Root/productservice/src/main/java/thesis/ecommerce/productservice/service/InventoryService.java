package thesis.ecommerce.productservice.service;

import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import thesis.ecommerce.productservice.model.ProductModel;
import thesis.ecommerce.productservice.repository.ProductRepository;

@Service
public class InventoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryService.class);
    private final ProductRepository productRepository;

    public InventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ResponseEntity<Boolean> reduceInventory(UUID productId, int requestedQuantity) {
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

            return ResponseEntity.ok(isStockEnough);
        } else {
            LOGGER.warn("Product not found for ID: {}", productId);
            return ResponseEntity.ok(false);
        }
    }

    private boolean checkInventory(ProductModel product, int requestedQuantity) {
        boolean isAvailable = product.getStock() >= requestedQuantity;
        LOGGER.info("Inventory check for product {}: Requested: {}, Available: {}, Result: {}",
            product.getId(), requestedQuantity, product.getStock(), isAvailable);
        return isAvailable;
    }
}