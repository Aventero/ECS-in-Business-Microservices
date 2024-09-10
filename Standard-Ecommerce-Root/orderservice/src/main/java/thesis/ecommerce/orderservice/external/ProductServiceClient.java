package thesis.ecommerce.orderservice.external;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import thesis.ecommerce.orderservice.api.dto.cart.ProductResponseDto;

@Service
public class ProductServiceClient {

    private final WebClient webClient;

    public ProductServiceClient(@Value("${product.service.url}") String productServiceUrl) {
        this.webClient = WebClient.create(productServiceUrl);
    }

    public Mono<ProductResponseDto> getProduct(UUID productId, String jwtToken) {
        return webClient.get()
            .uri("/api/products/{id}", productId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
            .retrieve()
            .bodyToMono(ProductResponseDto.class);
    }

    public Mono<Boolean> reduceInventory(UUID productId, int quantity, String jwtToken) {
        return webClient.post()
            .uri("/api/products/{productId}/reduce-inventory?quantity={quantity}", productId, quantity)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
            .retrieve()
            .bodyToMono(Boolean.class);
    }
}