package thesis.ecommerce.orderservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import thesis.ecommerce.orderservice.api.dto.cart.AddToCartRequestDto;
import thesis.ecommerce.orderservice.api.dto.cart.ProductResponseDto;
import thesis.ecommerce.orderservice.external.ProductServiceClient;
import thesis.ecommerce.orderservice.persistence.model.CartItemModel;
import thesis.ecommerce.orderservice.persistence.model.CartModel;
import thesis.ecommerce.orderservice.persistence.repository.CartItemRepository;
import thesis.ecommerce.orderservice.persistence.repository.CartRepository;

class CartServiceTest {

    private static CartService cartService;

    private static ProductServiceClient productServiceClient;
    private static CartRepository cartRepository;
    private static CartItemRepository cartItemRepository;

    @BeforeAll
    static void setUp() {
        productServiceClient = mock(ProductServiceClient.class);
        cartRepository = mock(CartRepository.class);
        cartItemRepository = mock(CartItemRepository.class);
        cartService = new CartService(productServiceClient, cartRepository, cartItemRepository);
    }

    @Test
    void testAddToCart_Success() {
        // Arrange
        String username = "testUser";
        String token = "testToken";
        UUID productId = UUID.randomUUID();
        int quantity = 1;

        AddToCartRequestDto request = new AddToCartRequestDto();
        request.setProductId(productId.toString());
        request.setQuantity(quantity);

        ProductResponseDto productResponse = new ProductResponseDto(
            productId, "Test Product", "Description", BigDecimal.valueOf(10.00), 100,
            "Category", Instant.now(), Instant.now()
        );

        CartModel cartModel = new CartModel();
        cartModel.setCartId(UUID.randomUUID());
        cartModel.setUsername(username);

        CartItemModel cartItemModel = new CartItemModel();
        cartItemModel.setCartItemId(UUID.randomUUID().toString());

        when(productServiceClient.getProduct(productId, token)).thenReturn(Mono.just(productResponse));
        when(cartRepository.findByUsername(username)).thenReturn(java.util.Optional.of(cartModel));
        when(cartRepository.save(any(CartModel.class))).thenReturn(cartModel);
        when(cartItemRepository.save(any(CartItemModel.class))).thenReturn(cartItemModel);

        // Act
        ResponseEntity<?> response = cartService.addToCart(request, username, token);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testAddToCart_NotFound() {
        // Arrange
        String username = "testUser";
        String token = "testToken";
        UUID productId = UUID.randomUUID();
        int quantity = 1;

        AddToCartRequestDto request = new AddToCartRequestDto();
        request.setProductId(productId.toString());
        request.setQuantity(quantity);

        when(productServiceClient.getProduct(productId, token))
            .thenReturn(Mono.error(new RuntimeException("Product not found")));

        // Act
        ResponseEntity<?> response = cartService.addToCart(request, username, token);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}