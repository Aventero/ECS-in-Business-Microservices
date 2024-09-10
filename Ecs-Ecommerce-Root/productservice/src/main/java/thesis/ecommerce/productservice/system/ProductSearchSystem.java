package thesis.ecommerce.productservice.system;

import dev.dominion.ecs.api.Entity;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.productservice.component.*;
import thesis.ecommerce.productservice.dto.ProductSearchResponse;
import thesis.ecommerce.productservice.model.ProductModel;
import thesis.ecommerce.productservice.repository.ProductRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductSearchSystem implements Runnable {
    private final ECSWorld ecsWorld;
    private final ProductRepository productRepository;

    public ProductSearchSystem(ECSWorld ecsWorld, ProductRepository productRepository) {
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
                Flags.SearchProducts.class,
                SearchCriteriaComponent.class
        ).forEach(result -> process(result.entity()));
    }

    private void process(Entity entity) {
        try {
            SearchCriteriaComponent criteria = entity.get(SearchCriteriaComponent.class);
            validateAndNormalizeCriteria(criteria);
            List<ProductModel> searchResults = performSearch(criteria);
            List<ProductSearchResultComponent> resultComponents = processSearchResults(searchResults);
            prepareSearchResponse(entity, resultComponents);

        } catch (Exception e) {
            String errorMessage = "Failed to perform product search: " + e.getMessage();
            entity.add(new FutureResponseComponent(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage)));
        } finally {
            entity.removeType(Flags.SearchProducts.class);
            entity.removeType(ProductSearchResultsComponent.class);
        }
    }

    // Validate and normalize search criteria
    private void validateAndNormalizeCriteria(SearchCriteriaComponent criteria) {
        if (criteria.getKeyword() != null) {
            criteria.setKeyword(criteria.getKeyword().trim().toLowerCase());
        }
        if (criteria.getCategory() != null) {
            criteria.setCategory(criteria.getCategory().trim());
        }
    }

    private List<ProductModel> performSearch(SearchCriteriaComponent criteria) {
        if ((criteria.getKeyword() == null || criteria.getKeyword().length() < 3)
                && (criteria.getCategory() == null || criteria.getCategory().trim().isEmpty())) {
            throw new IllegalArgumentException("Search requires a keyword of at least 3 characters or a category");
        }

        String keyword = criteria.getKeyword() != null ? criteria.getKeyword().trim().toLowerCase() : null;
        String category = criteria.getCategory() != null ? criteria.getCategory().trim() : null;

        if (keyword != null && category != null) {
            return productRepository.findByCategoryAndNameContainingIgnoreCase(category, keyword);
        } else if (keyword != null) {
            return productRepository.findByNameContainingIgnoreCase(keyword);
        } else {
            return productRepository.findByCategoryIgnoreCase(category);
        }
    }

    // This processes the search results by mapping them to search result components
    private List<ProductSearchResultComponent> processSearchResults(List<ProductModel> searchResults) {
        return searchResults.stream()
                .map(this::mapToSearchResultComponent)
                .collect(Collectors.toList());
    }

    // Map product entity to search result component
    private ProductSearchResultComponent mapToSearchResultComponent(ProductModel product) {
        return new ProductSearchResultComponent(
                product.getId().toString(),
                product.getName(),
                product.getDescription(),
                product.getStock(),
                product.getPrice(),
                product.getCategory()
        );
    }

    // Prepares the search response by adding the search results to the entity and completing the future response
    private void prepareSearchResponse(Entity entity, List<ProductSearchResultComponent> resultComponents) {
        entity.add(new ProductSearchResultsComponent(resultComponents));

        List<ProductSearchResponse> response = resultComponents.stream()
                .map(this::mapToProductSearchResponse)
                .collect(Collectors.toList());

        entity.add(new FutureResponseComponent(ResponseEntity.ok(response)));

    }

    private ProductSearchResponse mapToProductSearchResponse(ProductSearchResultComponent component) {
        ProductSearchResponse response = new ProductSearchResponse();
        response.setId(component.getId());
        response.setName(component.getName());
        response.setDescription(component.getDescription());
        response.setStock(component.getStock());
        response.setPrice(component.getPrice());
        response.setCategory(component.getCategory());
        return response;
    }
}