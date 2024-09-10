package thesis.ecommerce.productservice.controller;

import dev.dominion.ecs.api.Entity;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import thesis.ecommerce.ECSWorld;
import thesis.ecommerce.productservice.component.CategoryComponent;
import thesis.ecommerce.productservice.component.CompletableFutureComponent;
import thesis.ecommerce.productservice.component.Flags;
import thesis.ecommerce.productservice.component.PriceComponent;
import thesis.ecommerce.productservice.component.ProductDateComponent;
import thesis.ecommerce.productservice.component.ProductDetailsComponent;
import thesis.ecommerce.productservice.component.ProductIdComponent;
import thesis.ecommerce.productservice.component.SearchCriteriaComponent;
import thesis.ecommerce.productservice.component.StockComponent;
import thesis.ecommerce.productservice.dto.ProductDto;
import thesis.ecommerce.productservice.dto.ProductSearchRequest;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ECSWorld ecsWorld;

    public ProductController(ECSWorld ecsWorld) {
        this.ecsWorld = ecsWorld;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public CompletableFuture<ResponseEntity<?>> createProduct(@RequestBody ProductDto productDTO) {
        Entity entity = ecsWorld.createEntity(
                new ProductDetailsComponent(productDTO.getName(), productDTO.getDescription()),
                new PriceComponent(productDTO.getPrice()),
                new StockComponent(productDTO.getStock()),
                new CategoryComponent(productDTO.getCategory()),
                new ProductDateComponent(Instant.now(), Instant.now()),
                new Flags.CreateProduct());
        CompletableFuture<ResponseEntity<?>> futureResponse = new CompletableFuture<>();
        entity.add(new CompletableFutureComponent(futureResponse));
        return futureResponse;
    }

    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<?>> searchProducts(@RequestBody ProductSearchRequest request) {
        var entity = ecsWorld.createEntity();
        entity.add(new SearchCriteriaComponent(request.getKeyword(), request.getCategory()));
        entity.add(new Flags.SearchProducts());

        CompletableFuture<ResponseEntity<?>> future = new CompletableFuture<>();
        entity.add(new CompletableFutureComponent(future));

        return future;
    }

    @GetMapping("/{productId}")
    public CompletableFuture<ResponseEntity<?>> getProduct(@PathVariable String productId) {
        Entity entity = ecsWorld.createEntity(
                new ProductIdComponent(UUID.fromString(productId)),
                new Flags.GetProduct());
        CompletableFuture<ResponseEntity<?>> futureResponse = new CompletableFuture<>();
        entity.add(new CompletableFutureComponent(futureResponse));
        return futureResponse;
    }

    @PostMapping("/{productId}/reduce-inventory")
    public CompletableFuture<ResponseEntity<?>> reduceInventory(
        @PathVariable UUID productId, @RequestParam int quantity) {

        Entity entity = ecsWorld.createEntity(
            new ProductIdComponent(productId),
            new StockComponent(quantity),
            new Flags.ReduceInventory());

        CompletableFuture<ResponseEntity<?>> futureResponse = new CompletableFuture<>();
        entity.add(new CompletableFutureComponent(futureResponse));
        return futureResponse;
    }


    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public CompletableFuture<ResponseEntity<?>> updateProduct(
            @PathVariable String productId,
            @RequestBody ProductDto productDTO) {
        Entity entity = ecsWorld.createEntity(
                new ProductIdComponent(UUID.fromString(productId)),
                new ProductDetailsComponent(productDTO.getName(), productDTO.getDescription()),
                new PriceComponent(productDTO.getPrice()),
                new StockComponent(productDTO.getStock()),
                new CategoryComponent(productDTO.getCategory()),
                new ProductDateComponent(null, Instant.now()),
                new Flags.UpdateProduct());
        CompletableFuture<ResponseEntity<?>> futureResponse = new CompletableFuture<>();
        entity.add(new CompletableFutureComponent(futureResponse));
        return futureResponse;
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public CompletableFuture<ResponseEntity<?>> deleteProduct(@PathVariable String productId) {
        Entity entity = ecsWorld.createEntity(
                new ProductIdComponent(UUID.fromString(productId)),
                new Flags.DeleteProduct());
        CompletableFuture<ResponseEntity<?>> futureResponse = new CompletableFuture<>();
        entity.add(new CompletableFutureComponent(futureResponse));
        return futureResponse;
    }

}