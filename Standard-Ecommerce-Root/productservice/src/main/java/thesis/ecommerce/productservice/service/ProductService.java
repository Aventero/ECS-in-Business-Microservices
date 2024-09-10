package thesis.ecommerce.productservice.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import thesis.ecommerce.productservice.dto.ProductDto;
import thesis.ecommerce.productservice.dto.ProductSearchRequest;
import thesis.ecommerce.productservice.dto.ProductSearchResponse;
import thesis.ecommerce.productservice.model.ProductModel;
import thesis.ecommerce.productservice.repository.ProductRepository;

@Service
public class ProductService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ResponseEntity<?> createProduct(ProductDto productDto) {
        try {
            ProductModel product = new ProductModel();
            product.setName(productDto.getName());
            product.setDescription(productDto.getDescription());
            product.setPrice(productDto.getPrice());
            product.setStock(productDto.getStock());
            product.setCategory(productDto.getCategory());

            Instant now = Instant.now();
            product.setCreationDate(now);
            product.setLastUpdated(now);

            ProductModel savedProduct = productRepository.save(product);
            return ResponseEntity.ok(savedProduct);
        } catch (Exception e) {
            String errorMessage = "Failed to create product: " + e.getMessage();
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    public ResponseEntity<?> getProduct(UUID productId) {
        try {
            Optional<ProductModel> productOpt = productRepository.findById(productId);

            if (productOpt.isPresent()) {
                ProductModel product = productOpt.get();
                return ResponseEntity.ok(product);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            String errorMessage = "Failed to read product: " + e.getMessage();
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    public ResponseEntity<?> searchProducts(ProductSearchRequest request) {
        try {
            validateAndNormalizeSearchCriteria(request);
            List<ProductModel> searchResults = performSearch(request);
            List<ProductSearchResponse> response = processSearchResults(searchResults);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            String errorMessage = "Failed to perform product search: " + e.getMessage();
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }

    public ResponseEntity<?> updateProduct(UUID productId, ProductDto productDTO) {
        try {
            ProductModel product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

            product.setName(productDTO.getName());
            product.setDescription(productDTO.getDescription());
            product.setPrice(productDTO.getPrice());
            product.setStock(productDTO.getStock());
            product.setCategory(productDTO.getCategory());
            product.setLastUpdated(Instant.now());

            productRepository.save(product);

            LOGGER.info("Product updated: {}", product.getId());
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            LOGGER.error("Error updating product: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error updating product: " + e.getMessage());
        }
    }

    public ResponseEntity<?> deleteProduct(UUID productId) {
        try {
            if (!productRepository.existsById(productId)) {
                return ResponseEntity.notFound().build();
            }
            productRepository.deleteById(productId);
            LOGGER.info("Product deleted: {}", productId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            LOGGER.error("Error deleting product: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error deleting product: " + e.getMessage());
        }
    }

    private void validateAndNormalizeSearchCriteria(ProductSearchRequest request) {
        if (request.getKeyword() != null) {
            request.setKeyword(request.getKeyword().trim().toLowerCase());
        }
        if (request.getCategory() != null) {
            request.setCategory(request.getCategory().trim());
        }
        if ((request.getKeyword() == null || request.getKeyword().length() < 3)
            && (request.getCategory() == null || request.getCategory().isEmpty())) {
            throw new IllegalArgumentException("Search requires a keyword of at least 3 characters or a category");
        }
    }

    private List<ProductModel> performSearch(ProductSearchRequest request) {
        String keyword = request.getKeyword();
        String category = request.getCategory();

        if (keyword != null && category != null) {
            return productRepository.findByCategoryAndNameContainingIgnoreCase(category, keyword);
        } else if (keyword != null) {
            return productRepository.findByNameContainingIgnoreCase(keyword);
        } else {
            return productRepository.findByCategoryIgnoreCase(category);
        }
    }

    private List<ProductSearchResponse> processSearchResults(List<ProductModel> searchResults) {
        return searchResults.stream()
            .map(this::mapToProductSearchResponse)
            .collect(Collectors.toList());
    }

    private ProductSearchResponse mapToProductSearchResponse(ProductModel product) {
        ProductSearchResponse response = new ProductSearchResponse();
        response.setId(product.getId().toString());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setStock(product.getStock());
        response.setPrice(product.getPrice());
        response.setCategory(product.getCategory());
        return response;
    }

}